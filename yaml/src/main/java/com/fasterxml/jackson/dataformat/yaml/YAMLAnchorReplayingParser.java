package com.fasterxml.jackson.dataformat.yaml;

import java.io.Reader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.events.AliasEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.NodeEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.CollectionEndEvent;
import org.yaml.snakeyaml.events.CollectionStartEvent;
import org.yaml.snakeyaml.nodes.MappingNode;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;

/**
 * A parser that remembers the events of anchored parts in yaml and repeats them
 * to inline these parts when an alias if found instead of only returning an alias.
 *
 * Note: this overwrites the getEvent() since the base `super.nextToken()` manages to much state and
 * it seems to be much simpler to re-emit the events.
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

    private void finishContext(AnchorContext context) {
        referencedObjects.put(context.anchor, context.events);
        if (!tokenStack.isEmpty()) {
            tokenStack.peek().events.addAll(context.events);
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
    protected Event getEvent() {
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
                refEvents.addAll(events);
                return refEvents.removeFirst();
            }
            throw new IllegalStateException("invalid alias " + alias.getAnchor());
        }

        if (event instanceof NodeEvent) {
            String anchor = ((NodeEvent) event).getAnchor();
            if (anchor != null) {
                AnchorContext context = new AnchorContext(anchor);
                context.events.add(event);
                if (event instanceof CollectionStartEvent) {
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
                    mergeStack.push(globalDepth);
                    return getEvent();
                }
                throw new IllegalStateException("found field '<<' but value isn't a map");
            }
        }

        if (!tokenStack.isEmpty()) {
            AnchorContext context = tokenStack.peek();
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
