package polygon;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import polygon.FixtureGenerator.Match;

class FixtureGeneratorTest
{
    @ParameterizedTest
    @MethodSource
    void testFixtureGenerator(int teams, int games)
    {
        var teamList = IntStream.rangeClosed(1, teams).boxed().toList();
        var fixtures = FixtureGenerator.teams(teamList).games(games).generate().toList();
        testRounds(teamList, games, fixtures);
    }

    <T> void testRounds(List<T> teams, int games, List<Match<T>> matches)
    {
        var teamCount = teams.size();
        var matchCount = matches.size();
        var roundCount = games * (teamCount - 1 + teamCount % 2);
        var matchesPerRound = (teamCount - teamCount % 2) / 2;
        assertThat(matchCount).isEqualTo(roundCount * matchesPerRound);
        var rounds = IntStream
                .iterate(0, i -> i < matchCount, i -> i + matchesPerRound)
                .mapToObj(i -> matches.subList(i, i + matchesPerRound))
                .toList();
        var opponentsPerRound = rounds
                .stream()
                .map(r -> r
                        .stream()
                        .flatMap(m -> Stream
                                .of(Pair.of(m::home, m::away), Pair.of(m::away, m::home)))
                        .collect(Collectors.toMap(Pair::first, Pair::second)))
                .toList();
        teams.forEach(t ->
        {
            assertThat(opponentsPerRound.stream().filter(r -> !r.containsKey(t)).count())
                    .isEqualTo(games * (teamCount % 2));
            assertThat(opponentsPerRound
                    .stream()
                    .map(r -> r.get(t))
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .values()).allMatch(c -> c == games);
            var homeAway = matches
                    .stream()
                    .map(m -> m.home().equals(t) ? "H" : m.away().equals(t) ? "A" : "")
                    .collect(Collectors.joining());
            assertThat(Pattern.compile("HHH|AAA").matcher(homeAway).results().count()).isEqualTo(0);
            assertThat(Pattern.compile("HH|AA").matcher(homeAway).results().count())
                    .isLessThanOrEqualTo(games);
            assertThat(Pattern.compile("H").matcher(homeAway).results().count())
                    .isCloseTo(Pattern.compile("A").matcher(homeAway).results().count(),
                            byLessThan(games * ((teamCount - 1) % 2) + 1L));
        });
    }

    static record Pair<T> (T first, T second)
    {
        static <T> Pair<T> of(Supplier<T> first, Supplier<T> second)
        {
            return new Pair<>(first.get(), second.get());
        }
    };

    static Stream<Arguments> testFixtureGenerator()
    {
        return Stream.of(arguments(10, 1), arguments(9, 1), arguments(4, 3));
    }
}
