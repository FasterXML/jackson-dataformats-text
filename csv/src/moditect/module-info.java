// Generated 27-Mar-2019 using Moditect maven plugin
module com.fasterxml.jackson.dataformat.csv {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    exports com.fasterxml.jackson.dataformat.csv;
//    exports com.fasterxml.jackson.dataformat.csv.impl;

    provides com.fasterxml.jackson.core.TokenStreamFactory with
        com.fasterxml.jackson.dataformat.csv.CsvFactory;
    provides com.fasterxml.jackson.databind.ObjectMapper with
        com.fasterxml.jackson.dataformat.csv.CsvMapper;
}
