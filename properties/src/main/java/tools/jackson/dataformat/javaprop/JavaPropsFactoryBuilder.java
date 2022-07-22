package tools.jackson.dataformat.javaprop;

import tools.jackson.core.base.DecorableTSFactory.DecorableTSFBuilder;

/**
 * {@link tools.jackson.core.TokenStreamFactory.TSFBuilder}
 * implementation for constructing {@link JavaPropsFactory}
 * instances.
 *
 * @since 3.0
 */
public class JavaPropsFactoryBuilder extends DecorableTSFBuilder<JavaPropsFactory, JavaPropsFactoryBuilder>
{
    public JavaPropsFactoryBuilder() {
        // No format-specific features yet so:
        super(0, 0);
    }

    public JavaPropsFactoryBuilder(JavaPropsFactory base) {
        super(base);
    }

    @Override
    public JavaPropsFactory build() {
        return new JavaPropsFactory(this);
    }
}
