package tools.jackson.dataformat.toml;

import tools.jackson.core.ObjectReadContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.TreeTraversingParser;

/**
 * Marker {@link TreeTraversingParser} used by {@link TomlFactory} so that
 * {@link TomlMapper#_readTreeAndClose} can recognize parsers it produced and
 * short-circuit their tree reconstruction (preserving {@code TomlObjectNode}
 * / {@code TomlArrayNode} flags). Externally supplied tree parsers — which
 * may be positioned at a sub-token and whose source the caller does not
 * expect us to mutate — go through the standard deserializer path.
 */
final class TomlTreeTraversingParser extends TreeTraversingParser {
    TomlTreeTraversingParser(JsonNode n, ObjectReadContext ctxt) {
        super(n, ctxt);
    }
}
