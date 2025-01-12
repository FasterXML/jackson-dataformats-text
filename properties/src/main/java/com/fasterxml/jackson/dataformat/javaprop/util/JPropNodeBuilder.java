package com.fasterxml.jackson.dataformat.javaprop.util;

import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsSchema;

public class JPropNodeBuilder
{
    /**
     * @deprecated Since 2.10
     */
    @Deprecated // since 2.10
    public static JPropNode build(JavaPropsSchema schema,
            Properties props)
    {
        return build(props, schema);

    }

    public static JPropNode build(Map<?,?> content, JavaPropsSchema schema) 
    {
        JPropNode root = new JPropNode();
        JPropPathSplitter splitter = schema.pathSplitter();
        for (Map.Entry<?,?> entry : content.entrySet()) {
            // these should be Strings; but due to possible "compromised" properties,
            // let's play safe, coerce if and as necessary
            String key = String.valueOf(entry.getKey());
            String value = String.valueOf(entry.getValue());

            splitter.splitAndAdd(root, key, value);
        }
        return root;
    }
}
