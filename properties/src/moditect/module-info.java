// Generated 27-Mar-2019 using Moditect maven plugin
module com.fasterxml.jackson.dataformat.javaprop {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    exports com.fasterxml.jackson.dataformat.javaprop;
//    exports com.fasterxml.jackson.dataformat.javaprop.impl;
    exports com.fasterxml.jackson.dataformat.javaprop.io;
    exports com.fasterxml.jackson.dataformat.javaprop.util;

    provides com.fasterxml.jackson.core.JsonFactory with
        com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
    provides com.fasterxml.jackson.core.ObjectCodec with
        com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
}
