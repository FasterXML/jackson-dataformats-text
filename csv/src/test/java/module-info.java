// CSV unit test Module descriptor
module tools.jackson.dataformat.csv
{
    // Since we are not split from Main artifact, will not
    // need to depend on Main artifact -- but need its dependencies
    
    requires tools.jackson.core;
    requires tools.jackson.databind;

    // Additional test lib/framework dependencies
    requires org.assertj.core;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;

    // Further, need to open up some packages for JUnit et al
    opens tools.jackson.dataformat.csv;
    opens tools.jackson.dataformat.csv.deser;
    opens tools.jackson.dataformat.csv.filter;
    opens tools.jackson.dataformat.csv.fuzz;
    opens tools.jackson.dataformat.csv.schema;
    opens tools.jackson.dataformat.csv.ser;
    opens tools.jackson.dataformat.csv.ser.dos;
    opens tools.jackson.dataformat.csv.testutil;
    opens tools.jackson.dataformat.csv.testutil.failure;
    opens tools.jackson.dataformat.csv.tofix;
}
