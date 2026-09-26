/*
 * #%L
 * %%
 * Copyright (C) 2024 The result4j Contributors (https://github.com/sviperll/result4j)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.github.chheller.result4j;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class ResultCollectors {
    /**
     * Splits a stream of results into successful values and error-values.
     * <p>
     * Unlike {@link #toSingleResult(Collector)}, this collector never short-circuits:
     * every result ends up in exactly one of the two downstream collectors.
     * The outcome is a typed {@link Result.Partitioned} rather than a {@code Map<Boolean, ...>}.
     *
     * @param okCollector collector for successful values
     * @param errCollector collector for error-values
     * @param <OkVal> type of successful result value
     * @param <ErrVal> type representing error-value
     * @param <OkR> type of the collected successful values
     * @param <ErrR> type of the collected error-values
     */
    public static <OkVal, ErrVal, OkR, ErrR>
    Collector<Result<OkVal, ErrVal>, ?, Result.Partitioned<OkR, ErrR>> partitioning(
            Collector<? super OkVal, ?, OkR> okCollector,
            Collector<? super ErrVal, ?, ErrR> errCollector
    ) {
        return Collectors.teeing(
                Collectors.flatMapping(Result.<OkVal, ErrVal>oks(), okCollector),
                Collectors.flatMapping(Result.<OkVal, ErrVal>errs(), errCollector),
                Result.Partitioned::new
        );
    }

    /** Splits a stream of results into a list of successful values and a list of error-values. */
    public static <OkVal, ErrVal>
    Collector<Result<OkVal, ErrVal>, ?, Result.Partitioned<List<OkVal>, List<ErrVal>>> partitioning() {
        return partitioning(Collectors.toList(), Collectors.toList());
    }

    public static <T, R, E> Collector<Result<T, E>, ?, Result<R, E>> toSingleResult(
            Collector<? super T, ?, R> collector
    ) {
        return toSingleResultTyped(collector);
    }

    private static <T, U, R, E> Collector<Result<T, E>, ?, Result<R, E>> toSingleResultTyped(
            Collector<? super T, U, R> collector
    ) {
        return Collector.of(
                () -> new ResultWrappedCollection<>(
                        collector.supplier().get(),
                        collector.accumulator(),
                        collector.combiner()
                ),
                ResultWrappedCollection::add,
                (c1, c2) -> {
                    c1.addAll(c2);
                    return c1;
                },
                (ResultWrappedCollection<U, T, E> c) ->
                        c.build().map(collector.finisher()),
                collector.characteristics()
                        .stream()
                        .filter(c -> c != Collector.Characteristics.IDENTITY_FINISH)
                        .toArray(Collector.Characteristics[]::new)
        );
    }

    private ResultCollectors() {
        throw new UnsupportedOperationException("Should not be instantiated");
    }

    private static class ResultWrappedCollection<C, T, E> {

        private Result<C, E> result;
        private final BiConsumer<C, ? super T> accumulator;
        private final BinaryOperator<C> combiner;

        private ResultWrappedCollection(
                C collection,
                BiConsumer<C, ? super T> accumulator,
                BinaryOperator<C> combiner
        ) {
            this.result = Result.ok(collection);
            this.accumulator = accumulator;
            this.combiner = combiner;
        }

        void add(Result<T, E> element) {
            result = result.flatMap(
                    c -> element.map(
                            e -> {
                                accumulator.accept(c, e);
                                return c;
                            }
                    )
            );
        }
        void addAll(ResultWrappedCollection<C, T, E> that) {
            result = result.flatMap(
                    c1 -> that.result.map(c2 -> combiner.apply(c1, c2))
            );
        }

        private Result<C, E> build() {
            return result;
        }
    }
}
