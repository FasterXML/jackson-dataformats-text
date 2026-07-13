package tools.jackson.dataformat.toml;

import tools.jackson.databind.JsonNode;

/**
 * Package-internal predicates over TOML-aware tree nodes.
 */
final class TomlNodes {
    private TomlNodes() {}

    static boolean isTableHeader(Object v) {
        return v instanceof TomlObjectNode && ((TomlObjectNode) v).fromTableHeader;
    }

    static boolean isArrayOfTables(Object v) {
        return v instanceof TomlArrayNode && ((TomlArrayNode) v).fromArrayOfTables;
    }

    /**
     * True when the value is itself table-like, or transitively contains a
     * table-like descendant. Used by the reordering pass so an ancestor of a
     * nested {@code [server.database]} (or {@code [[items]]}) gets deferred
     * before later scalars at its parent level — TOML rejects scalars after
     * sub-tables at the same level.
     */
    static boolean containsTableLike(JsonNode v) {
        if (isTableHeader(v) || isArrayOfTables(v)) {
            return true;
        }
        if (v instanceof TomlObjectNode) {
            for (JsonNode child : ((TomlObjectNode) v).values()) {
                if (containsTableLike(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
