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
 * @since 2.19 / 3.1
 */
public class YAMLAnchorReplayingParser extends YAMLParser
{
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
     *
     * @deprecated Since 2.21.6 / 3.1.6 not used for anything: this counted anchors nested inside
     *   each other, and since every such anchor is an open collection, the (configurable)
     *   {@link tools.jackson.core.StreamReadConstraints#getMaxNestingDepth()} is
     *   always reached first.
     */
    @Deprecated
    public static final int MAX_ANCHORS = 9999;

    /**
     * the maximum limit of merges to follow
     *
     * @deprecated Since 2.21.6 / 3.1.6 not used for anything: nesting of merge keys is limited by
     *   {@link tools.jackson.core.StreamReadConstraints#getMaxNestingDepth()},
     *   which is both configurable and reached first (merge nesting can never exceed
     *   the nesting depth of the document it occurs in).
     */
    @Deprecated
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
        if (referencedObjects.size() >= MAX_REFS) {
            throw constraintsException("too many references in the document",
                    referencedObjects.size() + 1, MAX_REFS, "MAX_REFS");
        }
        referencedObjects.put(context.anchor, context.events);
        if (!tokenStack.isEmpty()) {
            List<Event> events = tokenStack.peek().events;
            if (events.size() + context.events.size() > MAX_EVENTS) {
                throw constraintsException("too many events to replay",
                        events.size() + context.events.size(), MAX_EVENTS, "MAX_EVENTS");
            }
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
	    if (context.events.size() >= MAX_EVENTS) {
	        throw constraintsException("too many events to replay",
	                context.events.size() + 1, MAX_EVENTS, "MAX_EVENTS");
	    }
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
        return nextEvent(true);
    }

    // @since 3.1.1
    protected Event nextEvent(boolean recordEvents) {
        // 2.21.6 / 3.1.6: merge keys used to be followed by recursing into this method,
        //   which let a document with deeply nested `<<` keys exhaust the stack (with
        //   `StackOverflowError`) before any of the limits here was reached. Both the
        //   merge and the alias continuation are tail calls, so loop instead.
        while (true) {
            while (!refEvents.isEmpty()) {
                Event event = filterEvent(trackDepth(refEvents.removeFirst()));
                if (event != null) {
                    if (recordEvents) {
                        recordEvent(event);
                    }
                    return event;
                }
            }

            Event event = null;
            while (event == null) {
                event = trackDepth(super.nextEvent());
                if (event == null) {
                    return null;
                }
                event = filterEvent(event);
            }

            if (event instanceof AliasEvent alias) {
                List<Event> events = referencedObjects.get(alias.getAlias().getValue());
                if (events != null) {
                    if (refEvents.size() + events.size() > MAX_EVENTS) {
                        throw constraintsException("too many events to replay",
                                refEvents.size() + events.size(), MAX_EVENTS, "MAX_EVENTS");
                    }
                    refEvents.addAll(events);
                    continue;
                }
                _reportError("invalid alias: " + alias.getAlias());
            }

            if (event instanceof NodeEvent nodeEvent) {
                String anchor = nodeEvent.getAnchor().map(Anchor::getValue).orElse(null);
                if (anchor != null) {
                    AnchorContext context = new AnchorContext(anchor);
                    context.events.add(event);
                    if (event instanceof CollectionStartEvent) {
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
                    // this mapping start event must not be registered by anchors; it is dropped so the contents are merged
                    Event next = nextEvent(false);
                    if (next instanceof MappingStartEvent) {
                        // 2.21.6 / 3.1.6: [dataformats-text#707] the `MappingStartEvent` of a
                        //   merged map is consumed here and its `MappingEndEvent` filtered out,
                        //   so merge nesting never reaches the usual nesting depth accounting
                        //   and has to be validated explicitly. Use `globalDepth`, which counts
                        //   all collection starts of the underlying event stream: structural
                        //   and merge nesting then share one limit.
                        streamReadConstraints().validateNestingDepth(globalDepth);
                        mergeStack.push(globalDepth);
                        // and then continue with the first event of the merged map
                        continue;
                    }
                    _reportError("found field '<<' but value isn't a map");
                }
            }

            if (recordEvents) {
                recordEvent(event);
            }
            return event;
        }
    }

    /**
     * Builds exception for one of the {@code MAX_xxx} limits of this class, including both
     * the count that tripped it and the limit itself (limits are constants, not settings,
     * so the constant is named to make it discoverable).
     */
    private StreamConstraintsException constraintsException(String problem,
            int count, int maxAllowed, String constantName)
    {
        return new StreamConstraintsException(String.format(
                "%s: %d exceeds the maximum allowed (%d, from `%s.%s`)",
                problem, count, maxAllowed,
                YAMLAnchorReplayingParser.class.getSimpleName(), constantName));
    }
}
