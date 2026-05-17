package tools.jackson.dataformat.toml;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Custom {@link JsonNodeFactory} that produces {@link TomlObjectNode} and
 * {@link TomlArrayNode} instances so that origin metadata (set by
 * {@link TomlParser}) survives round-trips through the tree model.
 */
@SuppressWarnings("serial")
class TomlNodeFactory extends JsonNodeFactory {
    static final TomlNodeFactory INSTANCE = new TomlNodeFactory();

    public TomlNodeFactory() {
        super();
    }

    @Override
    public ArrayNode arrayNode() {
        return new TomlArrayNode(this);
    }

    @Override
    public ArrayNode arrayNode(int capacity) {
        return new TomlArrayNode(this, capacity);
    }

    @Override
    public ObjectNode objectNode() {
        return new TomlObjectNode(this);
    }
}
