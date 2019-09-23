/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.config.mockup;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Helidon Class loader.
 */
public class ConfigMockupClassLoader extends ClassLoader {

    static final String DEFAULT_SOURCE_PACKAGE = "io.helidon.config.mockup";
    static final String DEFAULT_TARGET_PACKAGE = "org.eclipse.microprofile.config";

    private final String sourcePackage;
    private final String targetPackage;
    private final String sourcePackagePath;
    private final String targetPackagePath;

    /**
     * Constructor using default packages.
     */
    public ConfigMockupClassLoader() {
        this.sourcePackage = DEFAULT_SOURCE_PACKAGE;
        this.targetPackage = DEFAULT_TARGET_PACKAGE;
        this.sourcePackagePath = DEFAULT_SOURCE_PACKAGE.replace('.', '/');
        this.targetPackagePath = DEFAULT_TARGET_PACKAGE.replace('.', '/');
    }

    /**
     * Constructor using custom packages.
     * @param sourcePackage Source package name.
     * @param targetPackage Target package name.
     */
    public ConfigMockupClassLoader(String sourcePackage, String targetPackage) {
        this.sourcePackage = sourcePackage;
        this.targetPackage = targetPackage;
        this.sourcePackagePath = sourcePackage.replace('.', '/');
        this.targetPackagePath = targetPackage.replace('.', '/');
    }

    @Override
    public Class<?> loadClass(String typeName) throws ClassNotFoundException {
        String baseName = findBaseName(typeName, '.');
        String packageName = findClassPackage(typeName, '.');

        // Non-mappable class loaded by parent
        if (!packageName.equals(targetPackage)) {
            return super.loadClass(typeName);
        }

        byte[] mappedBytecode;
        String sourceClassName = sourcePackage + "." + baseName;
        try {
            mappedBytecode = mapPackageNames(sourceClassName);
        } catch (IOException e) {
            throw new RuntimeException("Unable to map class " + typeName);
        }

        return defineClass(typeName, mappedBytecode, 0, mappedBytecode.length);
    }

    private byte[] mapPackageNames(String typeName) throws IOException {
        ClassReader classReader = new ClassReader(typeName);
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        Remapper mapper = new PackageNameMapper();
        classReader.accept(new ClassRemapper(classWriter, mapper), 0);
        return classWriter.toByteArray();
    }

    private class PackageNameMapper extends Remapper {

        @Override
        public String map(String typeName) {
            String packageName = findClassPackage(typeName, '/');
            if (packageName.endsWith(sourcePackagePath)) {
                String baseName = findBaseName(typeName, '/');
                return targetPackagePath + "/" + baseName;
            }
            return typeName;
        }
    }

    private static String findClassPackage(String className, char delim) {
        final int k = className.lastIndexOf(delim);
        return k > 0 ? className.substring(0, k) : className;
    }

    private static String findBaseName(String className, char delim) {
        final int k = className.lastIndexOf(delim);
        return k > 0 ? className.substring(k + 1) : className;
    }
}
