package tools.jackson.dataformat.yaml;

import java.io.Reader;

import java.util.*;

import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.Anchor;
import org.snakeyaml.engine.v2.events.*;

import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.BufferRecycler;

/**
 * A parser that remembers the events of anchored parts in yaml and repeats them
 * to inline these parts when an alias if found instead of only returning an alias.
 * <p>
 * Note: this overwrites the nextEvent() since the base {@code super.nextToken()}
 * manages too much state, and it seems to be much simpler to re-emit the events.
 *
 * @since 2.19
 */
public class YAMLAnchorReplayingParser extends YAMLParser {
    private static class AnchorContext {
        public final String anchor;
        public final List<Event> events = new ArrayList<>();
        public int depth = 1;

        public AnchorContext(String anchor) {
            this.anchor = anchor;
        }
    }

    /**
     *  the maximum number of events that can be replayed
     */
    public static final int MAX_EVENTS = 9999;

    /**
     * the maximum limit of anchors to remember
     */
    public static final int MAX_ANCHORS = 9999;

    /**
     * the maximum limit of merges to follow
     */
    public static final int MAX_MERGES = 9999;

    /**
     * the maximum limit of references to remember
     */
    public static final int MAX_REFS = 9999;

    /**
     * Remembers when a merge has been started in order to skip the corresponding
     * sequence end which needs to be excluded
     */
    private final ArrayDeque<Integer> mergeStack = new ArrayDeque<>();

    /**
     * Collects nested anchor definitions
     */
    private final ArrayDeque<AnchorContext> tokenStack = new ArrayDeque<>();

    /**
     * Keeps track of the last sequentially found definition of each anchor
     */
    private final Map<String, List<Event>> referencedObjects = new HashMap<>();

    /**
     * Keeps track of events that have been insert when processing alias
     */
    private final ArrayDeque<Event> refEvents = new ArrayDeque<>();

    /**
     * keeps track of the global depth of nested collections
     */
    private int globalDepth = 0;

    public YAMLAnchorReplayingParser(ObjectReadContext readCtxt, IOContext ioCtxt, BufferRecycler br,
                      int streamReadFeatures, int formatFeatures,
                      LoadSettings loadSettings, Reader reader) {
        super(readCtxt, ioCtxt, br, streamReadFeatures, formatFeatures, loadSettings, reader);
    }

    private void finishContext(AnchorContext context) throws StreamConstraintsException {
        if (referencedObjects.size() + 1 > MAX_REFS)
			throw new StreamConstraintsException("too many references in the document");
        referencedObjects.put(context.anchor, context.events);
        if (!tokenStack.isEmpty()) {
            List<Event> events = tokenStack.peek().events;
            if (events.size() + context.events.size() > MAX_EVENTS)
				throw new StreamConstraintsException("too many events to replay");
            events.addAll(context.events);
        }
    }

    protected Event trackDepth(Event event) {
        if (event instanceof CollectionStartEvent) {
            ++globalDepth;
        } else if (event instanceof CollectionEndEvent) {
            --globalDepth;
        }
        return event;
    }

    protected Event filterEvent(Event event) {
        if (event instanceof MappingEndEvent) {
            if (!mergeStack.isEmpty()) {
                if (mergeStack.peek() > globalDepth) {
                    mergeStack.pop();
                    return null;
                }
            }
        }
        return event;
    }

    protected void recordEvent(Event event) {
	    if (tokenStack.isEmpty()) return;
	    AnchorContext context = tokenStack.peek();
	    if (context.events.size() + 1 > MAX_EVENTS)
	        throw new StreamConstraintsException("too many events to replay");
	    context.events.add(event);
	    if (event instanceof CollectionStartEvent) {
	        ++context.depth;
	    } else if (event instanceof CollectionEndEvent) {
	        --context.depth;
	        if (context.depth == 0) {
	            tokenStack.pop();
	            finishContext(context);
	        }
	    }
    }

    @Override
    protected Event nextEvent() {
        while (!refEvents.isEmpty()) {
            Event event = filterEvent(trackDepth(refEvents.removeFirst()));
            if (event != null) {
                recordEvent(event);
                return event;
            }
        }

        Event event = null;
        while (event == null) {
            event = trackDepth(super.nextEvent());
            if (event == null) return null;
            event = filterEvent(event);
        }

        if (event instanceof AliasEvent alias) {
            List<Event> events = referencedObjects.get(alias.getAlias().getValue());
            if (events != null) {
                if (refEvents.size() + events.size() > MAX_EVENTS)
					throw new StreamConstraintsException("too many events to replay");
                refEvents.addAll(events);
                return nextEvent();
            }
            _reportError("invalid alias: " + alias.getAlias());
        }

        if (event instanceof NodeEvent nodeEvent) {
            String anchor = nodeEvent.getAnchor().map(Anchor::getValue).orElse(null);
            if (anchor != null) {
                AnchorContext context = new AnchorContext(anchor);
                context.events.add(event);
                if (event instanceof CollectionStartEvent) {
                    if (tokenStack.size() + 1 > MAX_ANCHORS)
						throw new StreamConstraintsException("too many anchors in the document");
                    tokenStack.push(context);
                } else {
                    // directly store it
                    finishContext(context);
                }
                // no need to record this event as it was handled above
                return event;
            }
        }

        if (event instanceof ScalarEvent scalarEvent) {
            if (scalarEvent.getValue().equals("<<")) {
                // expect next node to be a map
                Event next = nextEvent();
                if (next instanceof MappingStartEvent) {
                    if (mergeStack.size() + 1 > MAX_MERGES)
						throw new StreamConstraintsException("too many merges in the document");
                    mergeStack.push(globalDepth);
                    return nextEvent();
                }
                _reportError("found field '<<' but value isn't a map");
            }
        }

        recordEvent(event);
        return event;
    }
}
