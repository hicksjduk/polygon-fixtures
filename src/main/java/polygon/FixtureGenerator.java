package polygon;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FixtureGenerator<T>
{
    public static void main(String[] args)
    {
        FixtureGenerator
                .newGenerator(10)
                .generate()
                .map(Object::toString)
                .map("%s\n"::formatted)
                .forEach(System.out::println);
    }

    public static FixtureGenerator<String> newGenerator(int count)
    {
        return new FixtureGenerator<>(
                IntStream.rangeClosed(1, count).mapToObj("Team %d"::formatted));
    }

    public static <T> FixtureGenerator<T> newGenerator(Stream<T> teams)
    {
        return new FixtureGenerator<>(teams);
    }

    public static <T> FixtureGenerator<T> newGenerator(Collection<T> teams)
    {
        return new FixtureGenerator<>(teams.stream());
    }

    @SuppressWarnings("unchecked")
    public static <T> FixtureGenerator<T> newGenerator(T... teams)
    {
        return new FixtureGenerator<>(Stream.of(teams));
    }

    private final List<T> teams;

    private FixtureGenerator(Stream<T> teams)
    {
        this.teams = teams.toList();
    }

    public Stream<Round<T>> generate()
    {
        var count = teams.size();
        if (count % 2 == 1)
            return generateFromOdd(teams);
        var otherTeam = teams.get(count - 1);
        var rounds = generateFromOdd(teams.subList(0, count - 1));
        var flip = alternatingBooleans();
        return zipWith(roundWithExtraMatch(otherTeam), rounds, flip);
    }

    private BiFunction<Round<T>, Boolean, Round<T>> roundWithExtraMatch(T otherTeam)
    {
        return (round, flip) ->
        {
            var extraMatch = Match.match(otherTeam, round.bye, flip);
            var matches = Stream.concat(round.matches, Stream.of(extraMatch));
            return new Round<>(matches, null);
        };
    }

    private Stream<Round<T>> generateFromOdd(List<T> teams)
    {
        var polygons = Stream.iterate(teams, this::rotate).limit(teams.size());
        return polygons.map(this::generate);
    }

    private List<T> rotate(List<T> list)
    {
        var count = list.size();
        var str1 = Stream.of(list.get(count - 1));
        var str2 = list.subList(0, count - 1).stream();
        return Stream.concat(str1, str2).toList();
    }

    private Round<T> generate(List<T> teams)
    {
        var count = teams.size();
        var bye = teams.get(count - 1);
        var fromStart = teams.stream();
        var fromEnd = reverseStream(teams.subList(0, count - 1));
        var flip = alternatingBooleans();
        var matches = zipWith(Match::match, fromStart, fromEnd, flip).limit(count / 2);
        return new Round<>(matches, bye);
    }

    private static Stream<Boolean> alternatingBooleans()
    {
        return Stream.iterate(false, v -> !v);
    }

    public static <A, B, R> Stream<R> zipWith(BiFunction<A, B, R> f, Stream<A> as, Stream<B> bs)
    {
        var bi = bs.iterator();
        return as.takeWhile(a -> bi.hasNext()).map(a -> f.apply(a, bi.next()));
    }

    @FunctionalInterface
    public static interface TriFunction<A, B, C, R>
    {
        R apply(A a, B b, C c);
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

    private static <T> Stream<T> reverseStream(List<T> list)
    {
        return IntStream.iterate(list.size() - 1, i -> i >= 0, i -> i - 1).mapToObj(list::get);
    }

    public static record Round<T> (Stream<Match<T>> matches, T bye)
    {
        @Override
        public String toString()
        {
            return Stream
                    .concat(matches.map(Object::toString),
                            bye == null ? Stream.empty() : Stream.of("Bye: %s".formatted(bye)))
                    .collect(Collectors.joining("\n"));
        }
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
}
