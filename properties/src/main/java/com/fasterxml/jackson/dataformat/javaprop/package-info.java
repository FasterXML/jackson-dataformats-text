/**
Basic API types to use with this module:
<ul>
  <li>{@link com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory}
 is the {@link com.fasterxml.jackson.core.JsonFactory} implementation used to
 create Java Properties parsers and generators
    <li>{@link com.fasterxml.jackson.dataformat.javaprop.JavaPropsGenerator}
 is the matching {@link com.fasterxml.jackson.core.JsonGenerator} implementation to use
      </li>
    <li>{@link com.fasterxml.jackson.dataformat.javaprop.JavaPropsParser}
 is the matching {@link com.fasterxml.jackson.core.JsonParser} implementation to use
      </li>
  </li>
  <li>{@link com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper} is a convenience
 sub-class of {@link com.fasterxml.jackson.databind.ObjectMapper} that is both configured
 to use {@link com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory}, and
 adds additional methods for using alternate content sources and targets for improved
 interoperability with {@link java.util.Properties}, System Properties, and env properties
    </li>
  <li>{@link com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema} is the
 {@link com.fasterxml.jackson.core.FormatSchema} implementation to use with Java Properties
 and defines details of how flat Java Properties keys are mapped to structured names
 of logical properties, POJOs, as well as other variations within possible Properties
 file notation (like indentation, key/value separator, linefeed to use)
    </li>
</ul>
*/

package com.fasterxml.jackson.dataformat.javaprop;
