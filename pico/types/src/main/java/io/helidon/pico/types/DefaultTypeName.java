/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.pico.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import io.helidon.common.LazyValue;

/**
 * Default implementation for {@link io.helidon.pico.types.TypeName}.
 */
public class DefaultTypeName implements TypeName {
    private static final boolean CALC_NAME = false;
    private final LazyValue<String> name = CALC_NAME ? LazyValue.create(this::calcName) : null;
    private final LazyValue<String> fqName = CALC_NAME ? LazyValue.create(this::calcFQName) : null;
    private final String packageName;
    private final String className;
    private final boolean primitive;
    private final boolean array;
    private final boolean wildcard;
    private final boolean generic;
    private final List<TypeName> typeArguments;

    /**
     * Ctor.
     *
     * @param b the builder
     * @see #builder()
     */
    protected DefaultTypeName(Builder b) {
        this.packageName = b.packageName;
        this.className = b.className;
        this.primitive = b.primitive;
        this.array = b.array;
        this.wildcard = b.wildcard;
        this.generic = b.generic;
        this.typeArguments = Objects.isNull(b.typeArguments)
                ? Collections.emptyList() : Collections.unmodifiableList(b.typeArguments);
    }

    /**
     * @return the {@link #fqName()}.
     */
    @Override
    public String toString() {
        return fqName();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TypeName)) {
            return false;
        }
        TypeName other = (TypeName) o;
        return Objects.equals(name(), other.name())
                && primitive == other.primitive()
                && array == other.array();
    }

    @Override
    public int compareTo(TypeName o) {
        return name().compareTo(o.name());
    }

    /**
     * Creates a type name from a package name and simple class name.
     *
     * @param packageName the package name
     * @param className   the simple class type name
     * @return the TypeName for the provided package and class names
     */
    public static DefaultTypeName create(String packageName, String className) {
        return DefaultTypeName.builder().packageName(packageName).className(className).build();
    }

    /**
     * Creates a type name from a class.
     *
     * @param classType the class type
     * @return the TypeName for the provided class type
     */
    public static DefaultTypeName create(Class<?> classType) {
        return builder().type(classType).build();
    }

    /**
     * Creates a type name from a generic alias type name.
     *
     * @param genericAliasTypeName the generic alias type name
     * @return the TypeName for the provided type name
     */
    public static DefaultTypeName createFromGenericDeclaration(String genericAliasTypeName) {
        if (!AnnotationAndValue.hasNonBlankValue(genericAliasTypeName)) {
            return null;
        }

        return builder()
                .generic(true)
                .className(genericAliasTypeName)
                .build();
    }

    /**
     * Creates a type name from a fully qualified class name.
     *
     * @param typeName the FQN of the class type
     * @return the TypeName for the provided type name
     */
    public static DefaultTypeName createFromTypeName(String typeName) {
        if (typeName.startsWith("? extends ")) {
            return createFromTypeName(typeName.substring(10).trim())
                    .toBuilder()
                    .wildcard(true)
                    .generic(true)
                    .build();
        }

        // a.b.c.SomeClass
        // a.b.c.SomeClass.InnerClass.Builder
        String className = typeName;
        List<String> packageElements = new LinkedList<>();

        while (true) {
            if (Character.isUpperCase(className.charAt(0))) {
                break;
            }
            int dot = className.indexOf('.');
            if (dot == -1) {
                // no more dots, we have the class name
                break;
            }
            packageElements.add(className.substring(0, dot));
            className = className.substring(dot + 1);
        }

        if (packageElements.isEmpty()) {
            return create(null, typeName);
        }

        String packageName = String.join(".", packageElements);
        return create(packageName, className);
    }

    /**
     * Throws an exception if the provided type name is not fully qualified, having a package and class name representation.
     *
     * @param name the type name to check
     */
    public static void ensureIsFQN(TypeName name) {
        if (null == name
                || !AnnotationAndValue.hasNonBlankValue(name.packageName())
                || !AnnotationAndValue.hasNonBlankValue(name.className())
                || !AnnotationAndValue.hasNonBlankValue(name.name())) {
            throw new AssertionError("needs to be a fully qualified name: " + name);
        }
    }

    /**
     * Returns true if the provided type name is fully qualified, having a package and class name representation.
     *
     * @param name the type name to check
     * @return true if the provided name is fully qualified
     */
    public static boolean isFQN(TypeName name) {
        if (null == name
                || !AnnotationAndValue.hasNonBlankValue(name.packageName())
                || !AnnotationAndValue.hasNonBlankValue(name.className())
                || !AnnotationAndValue.hasNonBlankValue(name.name())) {
            return false;
        }
        return true;
    }

    @Override
    public String packageName() {
        return packageName;
    }

    @Override
    public String className() {
        return className;
    }

    @Override
    public boolean primitive() {
        return primitive;
    }

    @Override
    public boolean array() {
        return array;
    }

    @Override
    public boolean generic() {
        return generic;
    }

    @Override
    public boolean wildcard() {
        return wildcard;
    }

    @Override
    public List<TypeName> typeArguments() {
        return typeArguments;
    }

    @Override
    public String name() {
        return (null == name) ? calcName() : name.get();
    }

    @Override
    public String declaredName() {
        return array() ? (name() + "[]") : name();
    }

    @Override
    public String fqName() {
        return (null == fqName) ? calcFQName() : fqName.get();
    }

    /**
     * Calculates the name - this is lazily deferred until referenced in {@link #name}.
     *
     * @return the name
     */
    protected String calcName() {
        return (primitive || Objects.isNull(packageName()))
                ? className() : packageName() + "." + className();
    }

    /**
     * Calculates the fully qualified name - this is lazily deferred until referenced in {@link #fqName()}.
     *
     * @return the fully qualified name
     */
    protected String calcFQName() {
        String name = wildcard() ? "? extends " + name() : name();

        if (null != typeArguments && !typeArguments.isEmpty()) {
            name += "<";
            int i = 0;
            for (TypeName param : typeArguments) {
                if (i > 0) {
                    name += ", ";
                }
                name += param.fqName();
                i++;
            }
            name += ">";
        }

        if (array()) {
            name += "[]";
        }

        return name;
    }


    /**
     * Creates a builder for {@link io.helidon.pico.types.TypeName}.
     *
     * @return a fluent builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder initialized with a value passed.
     *
     * @param val the value
     * @return a fluent builder
     */
    public static Builder toBuilder(TypeName val) {
        return new Builder(val);
    }

    /**
     * Creates a builder initialized with the current values.
     *
     * @return a fluent builder
     */
    public Builder toBuilder() {
        return toBuilder(this);
    }


    /**
     * The fluent builder.
     */
    public static class Builder {
        private String packageName;
        private String className;
        private boolean primitive;
        private boolean array;
        private boolean wildcard;
        private boolean generic;
        private List<TypeName> typeArguments;

        /**
         * Default ctor.
         */
        protected Builder() {
        }

        /**
         * Ctor taking the typeName for initialization.
         *
         * @param val   the typeName
         */
        protected Builder(TypeName val) {
            this.packageName = val.packageName();
            this.className = val.className();
            this.primitive = val.primitive();
            this.array = val.array();
            this.wildcard = val.wildcard();
            this.generic = val.generic();
            this.typeArguments = new ArrayList<>(val.typeArguments());
        }

        /**
         * Set the package name.
         *
         * @param val   the package name
         * @return this fluent builder
         */
        public Builder packageName(String val) {
            this.packageName = val;
            return this;
        }

        /**
         * Set the simple class name.
         *
         * @param val  the simple class name
         * @return the fluent builder
         */
        public Builder className(String val) {
            this.className = val;
            return this;
        }

        /**
         * Sets the package and class name, as well as whether it is primitive or an array.
         *
         * @param classType  the class
         * @return the fluent builder
         */
        public Builder type(Class<?> classType) {
            Class<?> componentType = classType.isArray() ? classType.getComponentType() : classType;
            packageName(componentType.getPackageName());
            className(componentType.getSimpleName());
            primitive(componentType.isPrimitive());
            return array(classType.isArray());
        }

        /**
         * Sets the array flag for this type.
         *
         * @param val   the array flag value
         * @return the fluent builder
         */
        public Builder array(boolean val) {
            this.array = val;
            return this;
        }

        /**
         * Sets the primitive flag for this type.
         *
         * @param val   the primitive flag value
         * @return the fluent builder
         */
        public Builder primitive(boolean val) {
            this.primitive = val;
            return this;
        }

        /**
         * Sets the generic flag for this type.
         *
         * @param val   the generic flag value
         * @return the fluent builder
         */
        public Builder generic(boolean val) {
            this.generic = val;
            return this;
        }

        /**
         * Sets the wildcard flag for this type, and conditionally the generic flag if the value passed is true.
         *
         * @param val   the array flag value
         * @return the fluent builder
         */
        public Builder wildcard(boolean val) {
            this.wildcard = val;
            if (val) {
                this.generic = true;
            }
            return this;
        }

        /**
         * Sets the generic type arguments to the collection passed, and if not empty will set the generic flag to true.
         *
         * @param val   the generic type arguments
         * @return the fluent builder
         */
        public Builder typeArguments(Collection<TypeName> val) {
            this.typeArguments = new ArrayList<>(val);
            return this;
        }

        /**
         * Builds the instance.
         *
         * @return the built instance
         */
        public DefaultTypeName build() {
            return new DefaultTypeName(this);
        }
    }

}