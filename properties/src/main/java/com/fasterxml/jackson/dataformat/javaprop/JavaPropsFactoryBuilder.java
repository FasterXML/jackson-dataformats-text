package com.fasterxml.jackson.dataformat.javaprop;

import com.fasterxml.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;

/**
 * {@link com.fasterxml.jackson.core.TokenStreamFactory.TSFBuilder}
 * implementation for constructing {@link JavaPropsFactory}
 * instances.
 *
 * @since 3.0
 */
public class JavaPropsFactoryBuilder extends DecorableTSFBuilder<JavaPropsFactory, JavaPropsFactoryBuilder>
{
    public JavaPropsFactoryBuilder() {
        super();
    }

    public JavaPropsFactoryBuilder(JavaPropsFactory base) {
        super(base);
    }

    @Override
    public JavaPropsFactory build() {
        // 28-Dec-2017, tatu: No special settings beyond base class ones, so:
        return new JavaPropsFactory(this);
    }
}
