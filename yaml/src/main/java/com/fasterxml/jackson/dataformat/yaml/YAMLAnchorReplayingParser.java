package com.fasterxml.jackson.dataformat.yaml;

import java.io.Reader;
import java.io.IOException;

import java.util.*;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.events.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.io.IOContext;

/**
 * A parser that remembers the events of anchored parts in yaml and repeats them
 * to inline these parts when an alias if found instead of only returning an alias.
 *<p>
 * Note: this overwrites the getEvent() since the base `super.nextToken()` manages to much state and
 * it seems to be much simpler to re-emit the events.
 *
 * @since 2.19
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

    public YAMLAnchorReplayingParser(IOContext ctxt, int parserFeatures, int formatFeatures, LoaderOptions loaderOptions, ObjectCodec codec, Reader reader) {
        super(ctxt, parserFeatures, formatFeatures, loaderOptions, codec, reader);
    }

    private void finishContext(AnchorContext context) throws StreamConstraintsException {
        if (referencedObjects.size() + 1 > MAX_REFS) throw new StreamConstraintsException("too many references in the document");
        referencedObjects.put(context.anchor, context.events);
        if (!tokenStack.isEmpty()) {
            List<Event> events = tokenStack.peek().events;
            if (events.size() + context.events.size() > MAX_EVENTS) throw new StreamConstraintsException("too many events to replay");
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

    @Override
    protected Event getEvent() throws IOException {
        while(!refEvents.isEmpty()) {
            Event event = filterEvent(trackDepth(refEvents.removeFirst()));
            if (event != null) return event;
        }

        Event event = null;
        while (event == null) {
            event = trackDepth(super.getEvent());
            if (event == null) return null;
            event = filterEvent(event);
        }

        if (event instanceof AliasEvent) {
            AliasEvent alias = (AliasEvent) event;
            List<Event> events = referencedObjects.get(alias.getAnchor());
            if (events != null) {
                if (refEvents.size() + events.size() > MAX_EVENTS) throw new StreamConstraintsException("too many events to replay");
                refEvents.addAll(events);
                return refEvents.removeFirst();
            }
            throw new JsonParseException("invalid alias " + alias.getAnchor());
        }

        if (event instanceof NodeEvent) {
            String anchor = ((NodeEvent) event).getAnchor();
            if (anchor != null) {
                AnchorContext context = new AnchorContext(anchor);
                context.events.add(event);
                if (event instanceof CollectionStartEvent) {
                    if (tokenStack.size() + 1 > MAX_ANCHORS) throw new StreamConstraintsException("too many anchors in the document");
                    tokenStack.push(context);
                } else {
                    // directly store it
                    finishContext(context);
                }
                return event;
            }
        }

        if (event instanceof ScalarEvent) {
            ScalarEvent scalarEvent = (ScalarEvent) event;
            if (scalarEvent.getValue().equals( "<<")) {
                // expect next node to be a map
                Event next = getEvent();
                if (next instanceof MappingStartEvent) {
                    if (mergeStack.size() + 1 > MAX_MERGES) throw new StreamConstraintsException("too many merges in the document");
                    mergeStack.push(globalDepth);
                    return getEvent();
                }
                throw new JsonParseException("found field '<<' but value isn't a map");
            }
        }

        if (!tokenStack.isEmpty()) {
            AnchorContext context = tokenStack.peek();
            if (context.events.size() + 1 > MAX_EVENTS) throw new StreamConstraintsException("too many events to replay");
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
        return event;
    }
}
