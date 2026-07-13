package tools.jackson.dataformat.toml;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

@SuppressWarnings("serial")
class TomlArrayNode extends ArrayNode {
    boolean closed = false;

    /**
     * True when this array was opened by a {@code [[path]]} array-of-tables
     * header during parsing. Used by {@link TomlGenerator} to preserve
     * array-of-tables syntax on round-trip.
     */
    boolean fromArrayOfTables = false;

    TomlArrayNode(JsonNodeFactory nf) {
        super(nf);
    }

    TomlArrayNode(JsonNodeFactory nf, int capacity) {
        super(nf, capacity);
    }

    @Override
    public TomlArrayNode deepCopy() {
        TomlArrayNode copy = (TomlArrayNode) super.deepCopy();
        copy.closed = this.closed;
        copy.fromArrayOfTables = this.fromArrayOfTables;
        return copy;
    }
}
