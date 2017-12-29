package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;

/**
 * {@link com.fasterxml.jackson.core.TokenStreamFactory.TSFBuilder}
 * implementation for constructing {@link YAMLFactory}
 * instances.
 *
 * @since 3.0
 */
public class YAMLFactoryBuilder extends DecorableTSFBuilder<YAMLFactory, YAMLFactoryBuilder>
{
    public YAMLFactoryBuilder() {
        super();
    }

    public YAMLFactoryBuilder(YAMLFactory base) {
        super(base);
    }

    @Override
    public YAMLFactory build() {
        // 28-Dec-2017, tatu: No special settings beyond base class ones, so:
        return new YAMLFactory(this);
    }
}
