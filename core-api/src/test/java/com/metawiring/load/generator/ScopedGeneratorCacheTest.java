/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.load.generator;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScopedGeneratorCacheTest {

    private GeneratorInstantiator generatorInstantiator = new GeneratorInstantiator(SimpleGeneratorForTesting.class);
    private static String genspec = SimpleGeneratorForTesting.class.getSimpleName() + ":genresult1";
    private static String fqGenspec = SimpleGeneratorForTesting.class.getCanonicalName() + ":genresult2";


    @Test(expectedExceptions = {RuntimeException.class}, expectedExceptionsMessageRegExp = ".*CACHE-LEVEL-REFERENCE-ERROR.*")
    public void shouldThrowErrorWhenNamedCacheLevelNotFound() {
        ScopedGeneratorCache sgc = new ScopedGeneratorCache(generatorInstantiator, RuntimeScope.process);
        Generator generator = sgc.getGenerator(genspec);
        assertThat(generator).isNotNull();
    }

    @Test(expectedExceptions = {RuntimeException.class}, expectedExceptionsMessageRegExp = ".*CACHE-SCOPE-PRECEDENCE-ERROR.*")
    public void shouldThrowErrorWhenEnteringSubscopeInWrongOrder() {
        ScopedGeneratorCache sgc = new ScopedGeneratorCache(generatorInstantiator, RuntimeScope.process);
        sgc.enterScope(RuntimeScope.definition);
    }

    @Test
    public void shouldReturnSameGeneratorForSameNameSameScope() {
        ScopedGeneratorCache sgc = new ScopedGeneratorCache(generatorInstantiator, RuntimeScope.process);
        ScopedGeneratorCache activityScopedCache = sgc.enterScope(RuntimeScope.activity);
        Generator generator1 = activityScopedCache.getGenerator("activity " + genspec);
        assertThat(generator1).isNotNull();
        Generator generator2 = activityScopedCache.getGenerator("activity " + genspec);

        assertThat(generator2).isNotNull();
        assertThat(generator1).isSameAs(generator2);
    }

    @Test
    public void shouldReturnDifferentGeneratorForSameNameDifferentScope() {
        ScopedGeneratorCache sgc = new ScopedGeneratorCache(generatorInstantiator, RuntimeScope.process);

        ScopedGeneratorCache phaseScopedCache = sgc.enterScope(RuntimeScope.phase);
        Generator generator1 = phaseScopedCache.getGenerator("phase " + genspec);
        assertThat(generator1).isNotNull();

        ScopedGeneratorCache activityScopedCache = phaseScopedCache.enterScope(RuntimeScope.activity);
        Generator generator2 = activityScopedCache.getGenerator("activity " + genspec);
        assertThat(generator2).isNotNull();

        assertThat(generator1).isNotSameAs(generator2);
    }

}