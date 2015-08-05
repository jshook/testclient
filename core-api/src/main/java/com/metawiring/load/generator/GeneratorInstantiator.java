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

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

public class GeneratorInstantiator implements GeneratorInstanceSource {
    private final static Logger logger = LoggerFactory.getLogger(GeneratorInstantiator.class);
    private List<GeneratorPackageManifest> generatorPackageManifestList;

    public GeneratorInstantiator() {}

    public GeneratorInstantiator(GeneratorPackageManifest directlyConfiguredManifest) {
        this.generatorPackageManifestList = new ArrayList<GeneratorPackageManifest>() {{
        add(directlyConfiguredManifest);
        }} ;
    }

    public GeneratorInstantiator(Class<? extends Generator<?>> directlyConfiguredManifestByClass) {
        this.generatorPackageManifestList = new ArrayList<GeneratorPackageManifest>() {{
            add(new GeneratorPackageManifest() {
                @Override
                public List<Package> getGeneratorPackages() {
                    return new ArrayList<Package>() {{
                        add(directlyConfiguredManifestByClass.getPackage());
                    }};
                }
            });
        }};
    }

    @SuppressWarnings("unchecked")
    public synchronized Generator getGenerator(String generatorSpec) {

        Class<Generator> generatorClass = (Class<Generator>) resolveGeneratorClass(generatorSpec);
        Object[] generatorArgs = parseGeneratorArgs(generatorSpec);

        try {
            return ConstructorUtils.invokeConstructor(generatorClass, generatorArgs);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @SuppressWarnings("unchecked")
    private Class<Generator> resolveGeneratorClass(String generatorSpec) {
        Class<Generator> generatorClass = null;
        String className = (generatorSpec.split(":"))[0];

        for (GeneratorPackageManifest generatorPackageManifest : generatorPackageManifestList) {
            for (Package generatorPackage : generatorPackageManifest.getGeneratorPackages()) {

                if (!className.contains(".")) {
                    className = generatorPackage.getName() + "." + className;
                }

                try {
                    generatorClass = (Class<Generator>) Class.forName(className);
                    logger.debug("Initialized class:" + generatorClass.getSimpleName() + " for generator type: " + generatorSpec);
                } catch (ClassNotFoundException ignored) {
                }

                if (generatorClass!=null) {
                    return generatorClass;
                }
            }

        }
        logger.error("Unable to map generator class " + generatorSpec);
        throw new RuntimeException("Unable to find a generator class for generator spec:" + generatorSpec);
    }

    private static Object[] parseGeneratorArgs(String generatorType) {
        String[] parts = generatorType.split(":");
        return Arrays.copyOfRange(parts, 1, parts.length);
    }

    private List<GeneratorPackageManifest> getGeneratorPackageManifests() {
        if (generatorPackageManifestList == null) {
            synchronized (this) {
                if (generatorPackageManifestList == null) {
                    ServiceLoader<GeneratorPackageManifest> manifestsLoader =
                            ServiceLoader.load(GeneratorPackageManifest.class);
                    List<GeneratorPackageManifest> manifests = new ArrayList<>();
                    manifestsLoader.forEach(manifests::add);
                    generatorPackageManifestList = manifests;
                }
            }
        }
        return generatorPackageManifestList;
    }
}
