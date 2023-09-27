package polygon;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FixtureGenerator<T>
{
    public static void main(String[] args)
    {
        FixtureGenerator.teamCount(10).games(1).generate().forEach(System.out::println);
    }

    public static Builder<String> teamCount(int teams)
    {
        return new Builder<>(generateNames(teams), null);
    }

    public static <T> Builder<T> teams(Stream<T> teams)
    {
        return new Builder<>(teams, null);
    }

    public static <T> Builder<T> teams(Collection<T> teams)
    {
        return new Builder<>(teams.stream(), null);
    }

    @SuppressWarnings("unchecked")
    public static <T> Builder<T> teams(T... teams)
    {
        return new Builder<>(Stream.of(teams), null);
    }

    public static Builder<Void> games(int games)
    {
        return new Builder<>(Stream.empty(), games);
    }

    public static class Builder<T>
    {
        private final Stream<T> teams;
        private final Integer games;

        private Builder(Stream<T> teams, Integer games)
        {
            this.teams = teams;
            this.games = games;
        }

        public Builder<String> teamCount(int teams)
        {
            return new Builder<>(generateNames(teams), games);
        }

        public <U> Builder<U> teams(Stream<U> teams)
        {
            return new Builder<>(teams, games);
        }

        public <U> Builder<U> teams(Collection<U> teams)
        {
            return new Builder<>(teams.stream(), games);
        }

        @SuppressWarnings("unchecked")
        public <U> Builder<U> teams(U... teams)
        {
            return new Builder<>(Stream.of(teams), games);
        }

        public Builder<T> games(int games)
        {
            return new Builder<>(teams, games);
        }

        public FixtureGenerator<T> build()
        {
            return new FixtureGenerator<>(teams, games);
        }

        public Stream<Match<T>> generate()
        {
            return build().generate();
        }
    }

    @FunctionalInterface
    public static interface TriFunction<A, B, C, R>
    {
        R apply(A a, B b, C c);
    }

    public static record Match<T> (T home, T away)
    {
        public static <T> Match<T> match(T t1, T t2, boolean flip)
        {
            return flip ? new Match<>(t2, t1) : new Match<>(t1, t2);
        }

        @Override
        public String toString()
        {
            return "%s v %s".formatted(home, away);
        }
    }

    private final List<T> teams;
    private final Integer games;
    final int roundsPerPhase;
    final int matchesPerRound;

    private FixtureGenerator(Stream<T> teams, Integer games)
    {
        this.teams = teams.toList();
        this.games = games;
        var count = this.teams.size();
        roundsPerPhase = count - 1 + count % 2;
        matchesPerRound = count / 2;
    }

    private static Stream<String> generateNames(int teams)
    {
        return IntStream.rangeClosed(1, teams).mapToObj("Team %d"::formatted);
    }

    public Stream<Match<T>> generate()
    {
        var schedule = generatePhase().toList();
        var reverse = reverse(schedule);
        var phases = Stream
                .generate(() -> Stream.of(schedule, reverse))
                .flatMap(Function.identity());
        if (games != null)
            phases = phases.limit(games);
        return phases.flatMap(Collection::stream);
    }

    private List<Match<T>> reverse(List<Match<T>> schedule)
    {
        return reverseStream(schedule).map(m -> new Match<>(m.away(), m.home())).toList();
    }

    private Stream<Match<T>> generatePhase()
    {
        var polygons = Stream.iterate(teams, this::rotate);
        var flipExtras = alternatingBooleans(false);
        return zipWith(this::generateRound, polygons, flipExtras)
                .limit(roundsPerPhase)
                .flatMap(Function.identity());
    }

    private List<T> rotate(List<T> teams)
    {
        var answer = new LinkedList<>(teams);
        var notToRotate = teams.size() % 2 == 0 ? answer.removeLast() : null;
        answer.addFirst(answer.removeLast());
        if (notToRotate != null)
            answer.addLast(notToRotate);
        return answer;
    }

    private Stream<Match<T>> generateRound(List<T> teams, boolean flipExtra)
    {
        var count = teams.size();
        var polygonMatchCount = (count - 1) / 2;
        // System.out.println("%s %d %d".formatted(teams, count, polygonMatchCount));
        var teams1 = Stream
                .concat(teams.stream().limit(polygonMatchCount),
                        reverseStream(teams).limit(1 - count % 2));
        var teams2 = Stream
                .concat(reverseStream(teams).skip(2 - count % 2).limit(polygonMatchCount),
                        reverseStream(teams).skip(1).limit(1 - count % 2));
        var flips = Stream
                .concat(alternatingBooleans(false).limit(polygonMatchCount), Stream.of(flipExtra));
        return zipWith(Match::match, teams1, teams2, flips);
    }

    public static <A, B, R> Stream<R> zipWith(BiFunction<A, B, R> f, Stream<A> as, Stream<B> bs)
    {
        var bi = bs.iterator();
        return as.takeWhile(a -> bi.hasNext()).map(a -> f.apply(a, bi.next()));
    }

    public static <A, B, C, R> Stream<R> zipWith(TriFunction<A, B, C, R> f, Stream<A> as,
            Stream<B> bs, Stream<C> cs)
    {
        var bi = bs.iterator();
        var ci = cs.iterator();
        return as
                .takeWhile(a -> bi.hasNext() && ci.hasNext())
                .map(a -> f.apply(a, bi.next(), ci.next()));
    }

    private static Stream<Boolean> alternatingBooleans(boolean first)
    {
        return Stream.iterate(first, v -> !v);
    }

    private static <T> Stream<T> reverseStream(List<T> list)
    {
        return IntStream.iterate(list.size() - 1, i -> i >= 0, i -> i - 1).mapToObj(list::get);
    }
}
