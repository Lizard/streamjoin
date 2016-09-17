package de.infonautika.streamjoin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static de.infonautika.streamjoin.streamutils.StreamCollector.toStream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class FunctionalJoin {
    public static <Y, L, R, K> Stream<Y> joinWithGrouper(
            Stream<L> left,
            Function<L, K> leftKeyFunction,
            Stream<R> right,
            Function<R, K> rightKeyFunction,
            BiFunction<L, Stream<R>, Y> grouper) {

        return join(
                left,
                leftKeyFunction,
                right,
                rightKeyFunction,
                (leftElements, rightElements) ->
                            leftElements
                                .map(l -> grouper.apply(l, rightElements.stream())));
    }

    public static <Y, L, R, K> Stream<Y> joinWithCombiner(
            Stream<L> left,
            Function<L, K> leftKeyFunction,
            Stream<R> right,
            Function<R, K> rightKeyFunction,
            BiFunction<L, R, Y> combiner) {


        return join(
                left,
                leftKeyFunction,
                right,
                rightKeyFunction,
                (leftElements, rightElements) -> leftElements
                        .map(l -> rightElements.stream().map(r -> combiner.apply(l, r)))
                        .flatMap(identity())
        );
    }

    private static <L, R, K, Y> Stream<Y> join(
            Stream<L> left,
            Function<L, K> leftKeyFunction,
            Stream<R> right,
            Function<R, K> rightKeyFunction,
            BiFunction<Stream<L>, List<R>, Stream<Y>> resultPart) {

        Stream.Builder<Stream<Y>> builder = Stream.builder();
        joinWithIndexedRightAndMatcher(
                left,
                leftKeyFunction,
                getMap(right, rightKeyFunction, toList())::get,
                (leftElements, rightElements) ->
                        builder.accept(resultPart.apply(leftElements, rightElements)));

        return builder.build().flatMap(identity());
    }

    private static <L, K, R> void joinWithIndexedRightAndMatcher(
            Stream<L> left,
            Function<L, K> leftKeyFunction,
            Function<K, List<R>> rightIndexed,
            BiConsumer<Stream<L>, List<R>> matcher) {

        getMap(left, leftKeyFunction, toStream())
                .forEach((key, leftDownstream) ->
                        Optional.ofNullable(rightIndexed.apply(key))
                                .ifPresent(rightDownstream -> matcher.accept(leftDownstream, rightDownstream)));
    }

    private static <T, K, D> Map<K, D> getMap(Stream<T> elements, Function<T, K> classifier, Collector<T, ?, D> downstream) {
        Map<K, D> map = elements.collect(
                groupingBy(
                        nullTolerantClassifier(classifier),
                        downstream));

        map.remove(nullKey());
        return map;
    }

    private static <T, K> Function<? super T, ? extends K> nullTolerantClassifier(Function<T, K> classifier) {
        return element -> Optional.ofNullable(classifier.apply(element)).orElse(nullKey());
    }

    private static final Object NULLKEY =new Object();


    private static <K> K nullKey() {
        return (K) NULLKEY;
    }



}
