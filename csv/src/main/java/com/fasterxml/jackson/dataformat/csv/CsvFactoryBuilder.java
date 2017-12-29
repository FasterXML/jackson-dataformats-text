package com.fasterxml.jackson.dataformat.csv;

import com.fasterxml.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;

/**
 * {@link com.fasterxml.jackson.core.TokenStreamFactory.TSFBuilder}
 * implementation for constructing {@link CsvFactory}
 * instances.
 *
 * @since 3.0
 */
public class CsvFactoryBuilder extends DecorableTSFBuilder<CsvFactory, CsvFactoryBuilder>
{
    public CsvFactoryBuilder() {
        super();
    }

    public CsvFactoryBuilder(CsvFactory base) {
        super(base);
    }

    @Override
    protected CsvFactory build() {
        // 28-Dec-2017, tatu: No special settings beyond base class ones, so:
        return new CsvFactory(this);
    }
}
