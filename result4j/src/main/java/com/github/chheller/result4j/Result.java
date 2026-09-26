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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A result that is either a successful execution with the value or a failure with the error-value.
 * <p>
 * {@code Result}-type is created for interoperability between normal Java-code that
 * throws exception and more functional code.
 * You do not need {@code Result}-type most of the time in Java-code, where
 * you can directly throw exceptions.
 * But there are situations, where more functional-style is used.
 * In such situations pure-functions are expected that throw no exceptions.
 * Handling exception in such situations can be cumbersome and require a lot of boilerplate code.
 * {@code Result}-type and associated helper-classes (like {@link Catcher}) help with
 * exception handling and allow to write idiomatic functional code that can interact with
 * methods that throw exceptions.
 * <p>
 * Result can be either success or failure, but not both at the same time.
 * Enclosed successful value or error value can be of different types,
 * represented by generic type-parameters.
 * <p>
 * The {@link Result#ok(Object)} and {@link Result#err(Object)} methods
 * create a new Result-value, that is respectively either a successful result or an error.
 *
 * {@snippet lang = "java":
 *     Result<String, Object> suc = Result.ok("Hello, World!");
 *}
 * <p>
 * The above line declares successful result value.
 *
 * {@snippet lang = "java":
 *     Result<Object, Integer> err = Result.err(404);
 *}
 * <p>
 * The above line declares error-value.
 *
 * <p>
 * The {@link Result.Ok} and {@link Result.Err} records are
 * subtypes of the {@code Result}-type and allow to use pattern matching to distinguish between
 * a successful result and an error
 *
 * {@snippet lang="java" :
 *     Result<String, Integer> result = ...;
 *     switch (result) {
 *         case Result.Success<String, Integer>(String value) ->
 *                 System.out.println(value);
 *         case Result.Error<String, Integer>(Integer code) ->
 *                 throw new IOException("%s: error".formatted(code));
 *     }
 * }
 * <p>
 * Pattern matching can be used to check unknown result value as shown above.
 *
 * {@snippet lang="java" :
 *     Result<String, Integer> receivedResult = ...;
 *     String value =
 *             receivedResult.orOnErrorThrow(
 *                     code -> new IOException("%s: error".formatted(code))
 *             );
 *     System.out.println(value);
 * }
 * <p>
 * Instead of a low-level pattern-matching,
 * higher level helper-methods are available in {@code Result}-class.
 * In the snippet above {@link Result#orElseThrow(Function)} is used to throw exception when
 * {@code Result} contains error.
 *
 * {@snippet lang="java" :
 *     String concatenation =
 *             Stream.of("a.txt", "b.txt", "c.txt")
 *                     .map(name -> loadFile(name))
 *                     .collect(ResultCollectors.toSingleResult(Collectors.join()))
 *                     .orOnErrorThrow(Function.identity());
 * }
 * <p>
 * In the above example we expect that the {@code loadFile} method returns the {@code Result}-type
 * instead of throwing an exception.
 * This allows us to use more functional-style, by using this method in the lambda-expression that
 * is not expected to throw any exceptions.
 * Handling exception in such situations can be cumbersome and require a lot of boilerplate code.
 * <p>
 * {@link ResultCollectors} class provides helper-methods to
 * combine multiple {@code Result}s into a single one.
 * {@link Catcher} class allows to adapt exception-throwing methods to
 * return {@code Result}-type instead.
 *
 * @param <OkVal> type of successful result value
 * @param <ErrVal> type representing error
 * @see Catcher
 * @see ResultCollectors
 */
public sealed interface Result<OkVal, ErrVal> {
    /**
     * Produces function-object that transforms error-values of results.
     * <p>
     * Similarly to {@link Result#mapError(Function)},
     * this method allows to apply a transformation to an error-value, associated with a result,
     * to get a new result.
     * In contrast to {@link Result#mapError(Function)},
     * instead of applying the transformation to some particular result,
     * this method produces a {@link Function} that
     * can be later applied to some future not yet known result or results.
     * <p>
     * This method can be used to make multi-level transformation easier to read.
     *
     * {@snippet lang="java" :
     *     List<Result<OkVal, E>> results = ...;
     *     results.stream()
     *         .map(result -> result.mapError(e -> transform(e)))
     *         ...
     * }
     * <p>
     * Instead of the above code, one can write
     *
     * {@snippet lang="java" :
     *     List<Result<OkVal, E>> results = ...;
     *     results.stream()
     *         .map(Result.errorMapping(e -> transform(e)))
     *         ...
     * }
     *
     * @param transformation transformation to be applied to error-values
     * @param <OkVal> type of successful result value
     * @param <InErr> type representing error-value associated with input
     * @param <OutErr> type representing error-value associated with output
     * @see Result#mapError(Function)
     */
    static <OkVal, InErr, OutErr> Function<Result<OkVal, InErr>, Result<OkVal, OutErr>> errorMapping(
            Function<? super InErr, ? extends OutErr> transformation
    ) {
        return result1 -> result1.mapError(transformation);
    }

    /**
     * Produces function-object that transforms values of successful results.
     * <p>
     * Similarly to {@link Result#map(Function)},
     * this method allows to apply a transformation to a value, associated with a successful result,
     * to get a new result.
     * In contrast to {@link Result#map(Function)},
     * instead of applying the transformation to some particular result,
     * this method produces a {@link Function} that
     * can be later applied to some future not yet known result or results.
     * <p>
     * This method can be used to make multi-level transformation easier to read.
     *
     * {@snippet lang = "java":
     *     List<Result<OutOk, ErrVal>> results = ...;
     *     results.stream()
     *         .map(result -> result.map(value -> transform(value)))
     *         ...
     *}
     * <p>
     * Instead of the above code, one can write
     *
     * {@snippet lang = "java":
     *     List<Result<OutOk, ErrVal>> results = ...;
     *     results.stream()
     *         .map(Result.mapping(e -> transform(e)))
     *         ...
     *}
     *
     * @param transformation transformation to be applied to a value of a successful result
     * @param <InOk> type of successful input result value
     * @param <OutOk> type of successful output result value
     * @param <ErrVal> type representing error-value
     * @see Result#map(Function)
     */
    static <InOk, OutOk, ErrVal> Function<Result<InOk, ErrVal>, Result<OutOk, ErrVal>> mapping(
            Function<? super InOk, ? extends OutOk> transformation
    ) {
        return result1 -> result1.map(transformation);
    }

    /**
     * Produces function-object that transforms values of successful results.
     * <p>
     * Similarly to {@link Result#flatMap(Function)},
     * this method allows to apply a transformation to a value, associated with a successful result,
     * to get a new result.
     * Transformation itself is partial and can produce either a successful result or an error.
     * In contrast to {@link Result#flatMap(Function)},
     * instead of applying the transformation to some particular result,
     * this method produces a {@link Function} that
     * can be later applied to some future not yet known result or results.
     * <p>
     * This method can be used to make multi-level transformation easier to read.
     *
     * {@snippet lang = "java":
     *     List<Result<OutOk, E>> results = ...;
     *     results.stream()
     *         .map(result -> result.flatMap(value -> actOn(value)))
     *         ...
     *}
     * <p>
     * Instead of the above code, one can write
     *
     * {@snippet lang = "java":
     *     List<Result<OutOk, E>> results = ...;
     *     results.stream()
     *         .map(Result.flatMapping(e -> actOn(e)))
     *         ...
     *}
     *
     * @param transformation transformation to be applied to a value of a successful result
     * @param <InOk> type of successful input result value
     * @param <OutOk> type of successful output result value
     * @param <ErrVal> type representing error-value
     * @see Result#mapError(Function)
     */
    static <InOk, OutOk, ErrVal> Function<Result<InOk, ErrVal>, Result<OutOk, ErrVal>> flatMapping(
            Function<? super InOk, ? extends Result<OutOk, ErrVal>> transformation
    ) {
        return result1 -> result1.flatMap(transformation);
    }

    /**
     * Produces {@code Result}-value containing given successful result value.
     *
     * @param <OkVal> type of successful result value
     * @param <ErrVal> type representing error-value
     * @param result successful result value
     * @return {@code Result}-value containing given successful result value
     */
    static <ErrVal, OkVal> Result<OkVal, ErrVal> ok(OkVal result) {
        return new Result.Ok<>(result);
    }

    /**
     * Produces {@code Result}-value containing given error value.
     *
     * @param <OkVal> type of successful result value
     * @param <ErrVal> type representing error-value
     * @param error error value
     * @return {@code Result}-value containing given error value
     */
    static <OkVal, ErrVal> Result<OkVal, ErrVal> err(ErrVal error) {
        return new Result.Err<>(error);
    }

    /**
     * Produces {@code Result}-value with {@code Optional} successful result value.
     * <ul>
     *   <li>When input argument in non-{@link Optional#empty() empty} and
     *   contains a successful result,
     *   method result is also a successful result that
     *   contains a non-{@link Optional#empty() empty} value,
     *   the same as in input successful result.
     *
     *   <li>When input argument in non-{@link Optional#empty() empty} and
     *   contains an error result,
     *   method result is an error result with the same error value.
     *
     *   <li>When input argument in {@link Optional#empty() empty},
     *   method result is a successful result with {@link Optional#empty()} value.
     * </ul>
     *
     * @param <OkVal> type of successful result value
     * @param <ErrVal> type representing error-value
     * @param value {@code Optional}-value containing {@code Result}-value
     * @return {@code Result}-value with {@code Optional} successful result value.
     */
    static <OkVal, ErrVal> Result<Optional<OkVal>, ErrVal> fromOptionalResult(
            Optional<Result<OkVal, ErrVal>> value
    ) {
        return value.map(Result.mapping(Optional::of))
                .orElseGet(() -> Result.ok(Optional.empty()));
    }

    /**
     * Produces {@code Result}-value from {@code Optional} value.
     * <ul>
     *   <li>When input argument in non-{@link Optional#empty() empty},
     *   method result is a successful result that
     *   contains in an {@code Optional}-value.
     *
     *   <li>When input argument in {@link Optional#empty() empty},
     *   method result is an error result with the error value,
     *   provided as an argument to this method.
     * </ul>
     *
     * @param <OkVal> type of successful result value
     * @param <ErrVal> type representing error-value
     * @param optional {@code Optional}-value
     * @param error error value
     * @return {@code Result}-value with {@code Optional} successful result value,
     *      or given error value.
     */
    static <OkVal, ErrVal> Result<OkVal, ErrVal> fromOptional(Optional<OkVal> optional, ErrVal error) {
        Optional<Result<OkVal, ErrVal>> optionalResult = optional.map(Result::ok);
        return optionalResult.orElse(Result.err(error));
    }

    /** Checks that this value is an error. */
    boolean isErr();

    /** Checks that this value is Ok */
    boolean isOk();
    /**
     * Transforms a value of this result, when this is a successful result.
     * <p>
     * This method allows to apply a transformation to a value,
     * associated with a successful result, to get a new result.
     * <ul>
     *   <li>When this result is an error, then
     *   the result of this method is an error, with the same error-value.
     *   <li>When this result is a success, then
     *   the result of this method a new result with transformed value.
     * </ul>
     *
     * @param <OutOk> new type of successful result value
     * @param transformation transformation to be applied to values
     */
    <OutOk> Result<OutOk, ErrVal> map(Function<? super OkVal, ? extends OutOk> transformation);

    /**
     * Transforms an error-value of this result, when this is an error.
     * <p>
     * this method allows to apply a transformation to an error-value,
     * associated with a result, to get a new result.
     * <ul>
     *   <li>When this result is an error, then
     *   the result of this method is an error, with the transformed error-value.
     *   <li>When this result is successful, then
     *   the result of this method the same successful result with the same value.
     * </ul>
     *
     * @param <OutErr> new type of error-value
     * @param transformation transformation to be applied to error-values
     */
    <OutErr> Result<OkVal, OutErr> mapError(Function<? super ErrVal, ? extends OutErr> transformation);

    /**
     * Produces optional-value, that is present when this is a successful result.
     */
    Optional<OkVal> discardError();

    /**
     * Recovers from error and produces the value of the successful result.
     * <p>
     * Returns the value of this result when this is a successful result, or otherwise
     * produces the value by transforming error-value.
     *
     * @param transformation transformation that
     *     is applied to error-value to get a successful result value
     */
    OkVal recoverError(Function<? super ErrVal, ? extends OkVal> transformation);


    /**
     * Throws an exception, by converting error-value to an exception instance.
     * <p>
     * Returns the value of this result when this is a successful result, or otherwise
     * throws an exception, by creating exception instance from error-value.
     *
     * @param <Ex> type of thrown exception
     * @param errorToExceptionConverter function to convert error-value to an exception
     * @return the value of this result, when it is a successful result
     * @throws Ex when this is an error result
     * @since 1.2.0
     */
    default <Ex extends Exception> OkVal orElseThrow(Function<? super ErrVal, Ex> errorToExceptionConverter)
            throws Ex {
        return switch (this) {
            case Result.Ok(OkVal ok) -> ok;
            case Result.Err(ErrVal err) -> throw errorToExceptionConverter.apply(err);
        };
    }


    /**
     * Invokes given consumer for an error-value of this result, and returns the same result.
     *
     * @param consumer consumer to be invoked for an error-value
     * @since 1.1.0
     */
    Result<OkVal, ErrVal> ifErr(Consumer<ErrVal> consumer);

    /**
     * Invokes given consumer for a success-value of this result, and returns the same result.
     *
     * @param consumer consumer to be invoked for an success-value
     * @since 1.1.0
     */
    Result<OkVal, ErrVal> ifOk(Consumer<OkVal> consumer);

    /**
     * Transforms a value of this result, when this is a successful result.
     * <p>
     * This method allows to apply a transformation to a value,
     * associated with a successful result, to get a new result.
     * Transformation itself is partial and can produce either a successful result or an error.
     * <ul>
     *   <li>When this result is an error, then
     *   the result of this method is an error, with the same error-value.
     *   <li>When this result is a success, then
     *   the result of this method the result of applying transformation to the argument.
     * </ul>
     *
     * @param <OutOk> new type of successful result value
     * @param transformation transformation to be applied to values
     */
    default <OutOk> Result<OutOk, ErrVal> flatMap(
            Function<? super OkVal, ? extends Result<OutOk, ErrVal>> transformation
    ) {
        Result<? extends Result<OutOk, ErrVal>, ErrVal> inflated = this.map(transformation);
        return switch (inflated) {
            case Result.Ok(var result) -> result;
            case Result.Err<?, ErrVal> err -> err.safeCast();
        };
    }

    record Ok<OkVal, ErrVal>(OkVal result) implements Result<OkVal, ErrVal> {
        @Override
        public <U> Result<U, ErrVal> map(
                Function<? super OkVal, ? extends U> transformation
        ) {
            return Result.ok(transformation.apply(result));
        }
        @Override
        public <E1> Result<OkVal, E1> mapError(
                Function<? super ErrVal, ? extends E1> transformation
        ) {
            return safeCast();
        }

        @SuppressWarnings("unchecked")
        private <ErrNever> Result.Ok<OkVal, ErrNever> safeCast() {
            return (Result.Ok<OkVal, ErrNever>) this;
        }

        @Override
        public Optional<OkVal> discardError() {
            return Optional.of(result);
        }

        @Override
        public boolean isErr() {
            return false;
        }

        @Override
        public boolean isOk() { return true; }

        @Override
        public OkVal recoverError(
                Function<? super ErrVal, ? extends OkVal> transformation
        ) {
            return result;
        }

        @Override
        public Result<OkVal, ErrVal> ifErr(Consumer<ErrVal> consumer) {
            return this;
        }

        @Override
        public Result<OkVal, ErrVal> ifOk(Consumer<OkVal> consumer) {
            consumer.accept(result);
            return this;
        }
    }

    record Err<OkVal, ErrVal>(ErrVal error) implements Result<OkVal, ErrVal> {
        @Override
        public <U> Result<U, ErrVal> map(
                Function<? super OkVal, ? extends U> transformation
        ) {
            return safeCast();
        }

        @Override
        public <OutErr> Result<OkVal, OutErr> mapError(
                Function<? super ErrVal, ? extends OutErr> transformation
        ) {
            return Result.err(transformation.apply(error));
        }

        @SuppressWarnings("unchecked")
        private <R1> Result.Err<R1, ErrVal> safeCast() {
            return (Result.Err<R1, ErrVal>) this;
        }

        @Override
        public Optional<OkVal> discardError() {
            return Optional.empty();
        }

        @Override
        public boolean isErr() {
            return true;
        }

        @Override
        public boolean isOk() { return false; }

        @Override
        public OkVal recoverError(
                Function<? super ErrVal, ? extends OkVal> transformation
        ) {
            return transformation.apply(error);
        }


        @Override
        public Result<OkVal, ErrVal> ifErr(Consumer<ErrVal> consumer) {
            consumer.accept(error);
            return this;
        }

        @Override
        public Result<OkVal, ErrVal> ifOk(Consumer<OkVal> consumer) {
            return this;
        }
    }
}
