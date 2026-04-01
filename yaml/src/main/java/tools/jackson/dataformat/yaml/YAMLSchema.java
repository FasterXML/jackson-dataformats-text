package tools.jackson.dataformat.yaml;

/**
 * Enum of YAML 1.2 schemas that can be used for parsing YAML documents.
 *
 * @since 3.2
 */
public enum YAMLSchema {
    /**
     * Schema guaranteed to work with any document.
     * @see https://yaml.org/spec/1.2.2/#101-failsafe-schema
     */
    FAILSAFE,
    /**
     * Schema for parsing JSON-compatible YAML documents.
     * @see https://yaml.org/spec/1.2.2/#102-json-schema
     */
    JSON,
    /**
     * Schema for parsing standard YAML documents.
     * @see https://yaml.org/spec/1.2.2/#103-core-schema
     */
    CORE,
    ;
}
