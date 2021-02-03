package com.fasterxml.jackson.dataformat.yaml.util;

import org.yaml.snakeyaml.DumperOptions;

/**
 * Helper interface to customize note styles of object and arrays while exporting YAML objects.
 */
public interface NodeStyleResolver {

    /**
     * Defines which style to apply to a given object or array.
     *
     * @see DumperOptions.FlowStyle
     * @see <a href="http://www.yaml.org/spec/current.html#id2509255">3.2.3.1.
     * Node Styles (http://yaml.org/spec/1.1)</a>
     */
    enum NodeStyle {
        /**
         * Block style. i.e.
         * <pre>
         *   foo:
         *   - bar
         * </pre>
         * <pre>
         *   key:
         *     foo: bar
         * </pre>
         */
        BLOCK,
        /**
         * Flow style. i.e.
         * <pre>
         *   foo: [bar]
         * </pre>
         * <pre>
         *   key: {foo: bar}
         * </pre>
         */
        FLOW;

        public DumperOptions.FlowStyle getSnakeYamlFlowStyle() {
            switch (this) {
                case BLOCK:
                    return DumperOptions.FlowStyle.BLOCK;
                case FLOW:
                    return DumperOptions.FlowStyle.FLOW;
                default:
                    throw new IllegalStateException("Unexpected value: " + this);
            }
        }
    }

    NodeStyleResolver DEFAULT_INSTANCE = new NodeStyleResolver() {
        @Override
        public NodeStyle resolveStyle(String fieldName) {
            // default behaviour uses YAMLGenerator._outputOptions.getDefaultFlowStyle() (currently set to BLOCK)
            return null;
        }
    };

    /**
     * Resolve a node style for given fieldName.
     *
     * @param fieldName parent field name of the current object or array. can be null if there is no parent field (i.e.
     *                  typically root object)
     * @return the desired {@link NodeStyle} or null to use default value (currently 'BLOCK')
     */
    NodeStyle resolveStyle(String fieldName);

}
