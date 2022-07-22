// Generated 27-Mar-2019 using Moditect maven plugin
module tools.jackson.dataformat.csv {
    requires tools.jackson.core;
    requires tools.jackson.databind;

    exports tools.jackson.dataformat.csv;
//    exports tools.jackson.dataformat.csv.impl;

    provides tools.jackson.core.TokenStreamFactory with
        tools.jackson.dataformat.csv.CsvFactory;
    provides tools.jackson.databind.ObjectMapper with
        tools.jackson.dataformat.csv.CsvMapper;
}
