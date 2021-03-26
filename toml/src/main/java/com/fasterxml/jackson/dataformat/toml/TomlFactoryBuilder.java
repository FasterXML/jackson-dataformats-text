package com.fasterxml.jackson.dataformat.toml;

import com.fasterxml.jackson.core.base.DecorableTSFactory;

/**
 * {@link com.fasterxml.jackson.core.TokenStreamFactory.TSFBuilder}
 * implementation for constructing {@link TomlFactory}
 * instances.
 *
 * @since 3.0
 */
public class TomlFactoryBuilder extends DecorableTSFactory.DecorableTSFBuilder<TomlFactory, TomlFactoryBuilder> {
    TomlFactoryBuilder() {
        super(0, 0);
    }

    TomlFactoryBuilder(TomlFactory base) {
        super(base);
    }

    @Override
    public TomlFactory build() {
        return new TomlFactory(this);
    }
}
