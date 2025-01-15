// Properties unit test Module descriptor
module tools.jackson.dataformat.javaprop
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
    opens tools.jackson.dataformat.javaprop;
    opens tools.jackson.dataformat.javaprop.constraints;
    opens tools.jackson.dataformat.javaprop.deser;
    opens tools.jackson.dataformat.javaprop.deser.convert;
    opens tools.jackson.dataformat.javaprop.filter;
    opens tools.jackson.dataformat.javaprop.ser.dos;
    opens tools.jackson.dataformat.javaprop.testutil;
    opens tools.jackson.dataformat.javaprop.testutil.failure;
    opens tools.jackson.dataformat.javaprop.util;
}
