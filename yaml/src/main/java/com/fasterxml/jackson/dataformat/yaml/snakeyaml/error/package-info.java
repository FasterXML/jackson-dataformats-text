/**
Replacement for shaded-in package that Jackson versions up to and including 2.7
had; with 2.8 (and later 2.x) we unfortunately fake to sort of fake formerly
relocated types. This is needed to improve backwards compatibility with
frameworks like DropWizard.
<p>
New code based on Jackson 2.8 and later should NOT use types in this package
but instead rely on {@link com.fasterxml.jackson.dataformat.yaml.JacksonYAMLParseException}
*/

package com.fasterxml.jackson.dataformat.yaml.snakeyaml.error;
