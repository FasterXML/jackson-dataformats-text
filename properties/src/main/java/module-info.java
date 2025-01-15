// Properties Main artifact Module descriptor
module tools.jackson.dataformat.javaprop
{
    requires tools.jackson.core;
    requires tools.jackson.databind;

    exports tools.jackson.dataformat.javaprop;
    // exports tools.jackson.dataformat.javaprop.impl;
    exports tools.jackson.dataformat.javaprop.io;
    exports tools.jackson.dataformat.javaprop.util;

    provides tools.jackson.core.TokenStreamFactory with
        tools.jackson.dataformat.javaprop.JavaPropsFactory;
    provides tools.jackson.databind.ObjectMapper with
        tools.jackson.dataformat.javaprop.JavaPropsMapper;
}
