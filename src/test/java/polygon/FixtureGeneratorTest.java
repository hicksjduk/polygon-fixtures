package polygon;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

import java.util.Collection;
import java.util.List;
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
        var count = matches.size();
        assertThat(count % games).isEqualTo(0);
        var matchesPerRound = count / games;
        var rounds = IntStream
                .iterate(0, i -> i < count, i -> i + matchesPerRound)
                .mapToObj(i -> teams.subList(i, i + matchesPerRound))
                .toList();
        teams.forEach(t ->
        {
            var playedPerRound = rounds
                    .stream()
                    .map(Collection::stream)
//                    .map(m -> List.of(m.home(), m.away()))
            // .mapToLong(r -> r.stream()
            // .filter(m -> Stream.of(m.home(), m.away()).anyMatch(x.equals(t)))
            // .count());
            ;
        });
    }

    static Stream<Arguments> testFixtureGenerator()
    {
        return Stream.of(arguments(10, 1));
    }
}
