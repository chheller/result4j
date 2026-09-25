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

import com.github.chheller.result4j.Result;

/**
 * Module to be used in tests to write assertions
 * for the {@link Result}-values in tests that use AssertJ.
 * <p>
 * Most of the functionality is contained in
 * the {@link com.github.chheller.assertj.result4j/com.github.chheller.assertj.result4j}-package.
 *
 * @see com.github.chheller.assertj.result4j/com.github.chheller.assertj.result4j
 * @see Result
 * @see <a href="https://assertj.github.io/doc/">AssertJ</a>
 * @since 1.2.0
 */
module com.github.chheller.assertj.result4j {
    requires transitive com.github.chheller.result4j;
    requires transitive org.assertj.core;

    exports com.github.chheller.assertj.result4j;
}
