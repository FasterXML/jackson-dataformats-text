package tools.jackson.dataformat.toml;

import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@SuppressWarnings("serial")
class TomlObjectNode extends ObjectNode {
    boolean closed = false;
    // True for both explicit headers and dotted-key super-tables; gates "Table redefined".
    boolean defined = false;
    // Subset of `defined`: set only by [table] or [[array-of-tables]] headers,
    // not by dotted-key super-tables.
    // Gates rejection of dotted-key extensions of an already-explicit table.
    boolean explicitlyDefined = false;

    /**
     * True when this node was opened by a {@code [path]} table header during
     * parsing. Used by {@link TomlGenerator} to preserve table syntax on
     * round-trip.
     */
    boolean fromTableHeader = false;

    TomlObjectNode(JsonNodeFactory nc) {
        super(nc);
    }

    TomlObjectNode(JsonNodeFactory nc, Map<String, JsonNode> kids) {
        super(nc, kids);
    }

    @Override
    public TomlObjectNode deepCopy() {
        TomlObjectNode copy = (TomlObjectNode) super.deepCopy();
        copy.closed = this.closed;
        copy.defined = this.defined;
        copy.explicitlyDefined = this.explicitlyDefined;
        copy.fromTableHeader = this.fromTableHeader;
        return copy;
    }

    /**
     * TOML strict-ordering rule: at any table level, scalars (and dotted-key
     * sub-objects) must precede standard tables and arrays of tables. We
     * cannot rewind the generator, so reorder children up front.
     *
     * <p>The classification is recursive: a child that is not itself
     * table-flagged but contains a table-flagged descendant (e.g. parent
     * {@code server} of a parsed {@code [server.database]}) must also be
     * deferred. Otherwise a later scalar sibling at this level would land
     * after the nested {@code [server.database]} header — which TOML
     * re-interprets as belonging to that section.
     */
    @Override
    public void serialize(JsonGenerator g, SerializationContext ctxt)
            throws JacksonException
    {
        int n = size();
        if (!(g instanceof TomlGenerator) || n == 0) {
            super.serialize(g, ctxt);
            return;
        }
        // Cheap pre-scan: avoid allocating snapshot arrays for the common
        // leaf-table case where nothing needs deferral.
        boolean anyDeferred = false;
        for (Map.Entry<String, JsonNode> en : properties()) {
            if (TomlNodes.containsTableLike(en.getValue())) {
                anyDeferred = true;
                break;
            }
        }
        if (!anyDeferred) {
            super.serialize(g, ctxt);
            return;
        }
        g.writeStartObject(this, n);
        // Two passes over properties() rather than snapshotting: each
        // re-classification short-circuits and properties() iteration is O(1)
        // per entry, so this saves the Map.Entry[] + boolean[] allocations.
        for (Map.Entry<String, JsonNode> en : properties()) {
            if (!TomlNodes.containsTableLike(en.getValue())) {
                g.writeName(en.getKey());
                en.getValue().serialize(g, ctxt);
            }
        }
        for (Map.Entry<String, JsonNode> en : properties()) {
            if (TomlNodes.containsTableLike(en.getValue())) {
                g.writeName(en.getKey());
                en.getValue().serialize(g, ctxt);
            }
        }
        g.writeEndObject();
    }
}
