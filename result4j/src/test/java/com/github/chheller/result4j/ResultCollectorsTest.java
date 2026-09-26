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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResultCollectorsTest {
    private static final List<Result<Integer, String>> MIXED =
            List.of(Result.ok(1), Result.err("a"), Result.ok(2), Result.err("b"));

    @Test
    public void nonAdaptedRuntimeExceptionFailure() {
        Catcher.ForFunctions<NumberFormatException> numberFormat =
                Catcher.of(NumberFormatException.class).forFunctions();
        Result<List<Integer>, NumberFormatException> result =
                Stream.of("123", "234", "xvxv", "456")
                        .map(numberFormat.catching(Integer::parseInt))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));
        Assertions.assertThrows(
                NumberFormatException.class,
                () -> result.orElseThrow(Function.identity())
        );
    }

    @Test
    public void nonAdaptedRuntimeExceptionSuccess() {
        Catcher.ForFunctions<NumberFormatException> numberFormat =
                Catcher.of(NumberFormatException.class).forFunctions();
        List<Integer> result =
                Stream.of("123", "234", "456")
                        .map(numberFormat.catching(Integer::parseInt))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()))
                        .orElseThrow(Function.identity());
        Assertions.assertEquals(List.of(123, 234, 456), result);
    }

    @Test
    public void adaptedRuntimeExceptionFailure() {
        AdaptingCatcher.ForFunctions<NumberFormatException, RuntimeException> numberFormat =
                Catcher.of(NumberFormatException.class).forFunctions().map(RuntimeException::new);
        Result<List<Integer>, RuntimeException> result =
                Stream.of("123", "234", "xvxv", "456")
                        .map(numberFormat.catching(Integer::parseInt))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));
        Assertions.assertThrows(
                RuntimeException.class,
                () -> result.orElseThrow(Function.identity())
        );
    }

    @Test
    public void adaptedRuntimeExceptionSuccess() {
        AdaptingCatcher.ForFunctions<NumberFormatException, RuntimeException> numberFormat =
                Catcher.of(NumberFormatException.class).forFunctions().map(RuntimeException::new);
        List<Integer> result =
                Stream.of("123", "234", "456")
                        .map(numberFormat.catching(Integer::parseInt))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()))
                        .orElseThrow(Function.identity());
        Assertions.assertEquals(List.of(123, 234, 456), result);
    }

    @Test
    public void multipleAdaptedCheckedExceptionsSuccess() throws PipelineException {
        AdaptingCatcher.ForFunctions<IOException, PipelineException> io =
                Catcher.of(IOException.class).map(PipelineException::new).forFunctions();
        AdaptingCatcher.ForFunctions<MLException, PipelineException> ml =
                Catcher.of(MLException.class).map(PipelineException::new).forFunctions();
        List<Animal> animals1 =
                List.of("cat.jpg", "dog.jpg")
                        .stream()
                        .map(io.catching(Fakes::readFile))
                        .map(Result.flatMapping(ml.catching(Fakes::recognizeImage)))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()))
                        .orElseThrow(Function.identity());
        Assertions.assertEquals(List.of(Animal.CAT, Animal.DOG), animals1);
    }

    @Test
    public void multipleAdaptedCheckedExceptionsFailure1() throws PipelineException {
        AdaptingCatcher.ForFunctions<IOException, PipelineException> io =
                Catcher.of(IOException.class).map(PipelineException::new).forFunctions();
        AdaptingCatcher.ForFunctions<MLException, PipelineException> ml =
                Catcher.of(MLException.class).map(PipelineException::new).forFunctions();
        Result<List<Animal>, PipelineException> animals =
                List.of("cat.jpg", "dog.jpg", "non-existent.jpg")
                        .stream()
                        .map(io.catching(Fakes::readFile))
                        .map(Result.flatMapping(ml.catching(Fakes::recognizeImage)))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));
        PipelineException exception =
                Assertions.assertThrows(
                        PipelineException.class,
                        () -> animals.orElseThrow(Function.identity())
                );
        Assertions.assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    public void multipleAdaptedCheckedExceptionsFailure2() throws PipelineException {
        AdaptingCatcher.ForFunctions<IOException, PipelineException> io =
                Catcher.of(IOException.class).map(PipelineException::new).forFunctions();
        AdaptingCatcher.ForFunctions<MLException, PipelineException> ml =
                Catcher.of(MLException.class).map(PipelineException::new).forFunctions();
        Result<List<Animal>, PipelineException> animals =
                List.of("cat.jpg", "dog.jpg", "corrupted.jpg")
                        .stream()
                        .map(io.catching(Fakes::readFile))
                        .map(Result.flatMapping(ml.catching(Fakes::recognizeImage)))
                        .collect(ResultCollectors.toSingleResult(Collectors.toList()));
        PipelineException exception =
                Assertions.assertThrows(
                        PipelineException.class,
                        () -> animals.orElseThrow(Function.identity())
                );
        Assertions.assertInstanceOf(MLException.class, exception.getCause());
    }

    enum Animal {
        DOG, CAT
    }

    static class Fakes {
        static String readFile(String name) throws IOException {
            return switch (name) {
                case "cat.jpg" -> "cat-image";
                case "dog.jpg" -> "dog-image";
                case "corrupted.jpg" -> "corrupted";
                default -> throw new FileNotFoundException("%s: not found".formatted(name));
            };
        }

        static Animal recognizeImage(String imageData) throws MLException {
            return switch (imageData) {
                case "cat-image" -> Animal.CAT;
                case "dog-image" -> Animal.DOG;
                default -> throw new MLException();
            };
        }
    }

    @SuppressWarnings("serial")
    static class MLException extends Exception {
    }

    @SuppressWarnings("serial")
    static class PipelineException extends Exception {
        PipelineException(IOException ex) {
            super(ex);
        }

        PipelineException(MLException ex) {
            super(ex);
        }
    }

    @Test
    public void partitioningSplitsOksAndErrs() {
        Result.Partitioned<List<Integer>, List<String>> parts =
                MIXED.stream()
                        .collect(ResultCollectors.partitioning());
        Assertions.assertEquals(List.of(1, 2), parts.oks());
        Assertions.assertEquals(List.of("a", "b"), parts.errs());
    }

    @Test
    public void partitioningWithDownstreamCollectors() {
        Result.Partitioned<Long, String> parts =
                MIXED.stream()
                        .collect(ResultCollectors.partitioning(
                                Collectors.counting(),
                                Collectors.joining(",")
                        ));
        Assertions.assertEquals(2L, parts.oks());
        Assertions.assertEquals("a,b", parts.errs());
    }

    @Test
    public void oksAndErrsExtractors() {
        List<Result<Integer, String>> results = List.of(Result.ok(1), Result.err("a"), Result.ok(2));
        Assertions.assertEquals(List.of(1, 2), results.stream().flatMap(Result.oks()).toList());
        Assertions.assertEquals(List.of("a"), results.stream().flatMap(Result.errs()).toList());
    }

    @Test
    public void recoveringAndBimapping() {
        List<Result<Integer, String>> results = List.of(Result.ok(1), Result.err("abc"));
        Assertions.assertEquals(
                List.of(1, 3),
                results.stream().map(Result.recovering(String::length)).toList()
        );
        Assertions.assertEquals(
                List.of(Result.ok("1"), Result.err(3)),
                results.stream().map(Result.<Integer, String, String, Integer>bimapping(
                        String::valueOf,
                        String::length
                )).toList()
        );
    }

    @Test
    public void kleisliShortCircuits() {
        Function<String, Result<Integer, String>> parse = s -> s.isEmpty()
                ? Result.err("empty")
                : Result.ok(s.length());
        Function<Integer, Result<Integer, String>> atLeastThree = n -> n >= 3
                ? Result.ok(n)
                : Result.err("short");
        Function<String, Result<Integer, String>> both = Result.kleisli(parse, atLeastThree);
        Assertions.assertEquals(Result.ok(3), both.apply("abc"));
        Assertions.assertEquals(Result.err("short"), both.apply("a"));
        Assertions.assertEquals(Result.err("empty"), both.apply(""));
    }

    @Test
    public void bimapTransformsBothSides() {
        Result<Integer, String> ok = Result.ok(2);
        Result<Integer, String> err = Result.err("abc");
        Assertions.assertEquals(Result.ok("2"), ok.bimap(String::valueOf, String::length));
        Assertions.assertEquals(Result.err(3), err.bimap(String::valueOf, String::length));
    }

    @Test
    public void thenCombineTwo() {
        Result<Integer, String> one = Result.ok(1);
        Result<Integer, String> two = Result.ok(2);
        Result<Integer, String> errA = Result.err("a");
        Result<Integer, String> errB = Result.err("b");
        Assertions.assertEquals(Result.ok(3), one.thenCombine(two, Integer::sum));
        Assertions.assertEquals(Result.err("a"), errA.thenCombine(two, Integer::sum));
        Assertions.assertEquals(Result.err("b"), one.thenCombine(errB, Integer::sum));
        Assertions.assertEquals(Result.err("a"), errA.thenCombine(errB, Integer::sum));
    }

    @Test
    public void thenCombineThree() {
        Result<Integer, String> one = Result.ok(1);
        Result<Integer, String> two = Result.ok(2);
        Result<Integer, String> three = Result.ok(3);
        Result<Integer, String> errB = Result.err("b");
        Result<Integer, String> errC = Result.err("c");
        Assertions.assertEquals(
                Result.ok("1-2-3"),
                one.thenCombine(two, three, (a, b, c) -> a + "-" + b + "-" + c)
        );
        Assertions.assertEquals(
                Result.err("b"),
                one.thenCombine(errB, errC, (a, b, c) -> a + b + c)
        );
        Assertions.assertEquals(
                Result.err("c"),
                one.thenCombine(two, errC, (a, b, c) -> a + b + c)
        );
    }
}
