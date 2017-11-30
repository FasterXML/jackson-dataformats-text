package com.fasterxml.jackson.dataformat.yaml.deser;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ParserDupHandlingTest extends ModuleTestBase
{
    private final static String YAML_WITH_DUPS =
"name:\n"
+"  first: Bob\n"
+"  first: Dup\n";

    public void testDupChecksDisabled() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.STRICT_DUPLICATE_DETECTION));

        ObjectMapper mapper = new ObjectMapper(f);
        _verifyDupsOk(mapper, YAML_WITH_DUPS, false);
        _verifyDupsOk(mapper, YAML_WITH_DUPS, true);
    }

    public void testDupChecksEnabled() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        f.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        ObjectMapper mapper = new ObjectMapper(f);
        _verifyDupsFail(mapper, YAML_WITH_DUPS, false);
        _verifyDupsFail(mapper, YAML_WITH_DUPS, true);
    }

    private void _verifyDupsOk(ObjectMapper mapper, String doc, boolean useBytes) throws Exception
    {
        JsonParser p = useBytes ? mapper.getFactory().createParser(new ByteArrayInputStream(doc.getBytes("UTF-8")))
                : mapper.getFactory().createParser(new StringReader(doc));
        _stream(p);
        p.close();
    }

    private void _verifyDupsFail(ObjectMapper mapper, String doc, boolean useBytes) throws Exception
    {
        JsonParser p = useBytes ? mapper.getFactory().createParser(new ByteArrayInputStream(doc.getBytes("UTF-8")))
                : mapper.getFactory().createParser(new StringReader(doc));
        try {
            _stream(p);
        } catch (JsonProcessingException e) {
            verifyException(e, "Duplicate field 'first'");
        }
        p.close();
    }

    private void _stream(JsonParser p) throws Exception
    {
        while (p.nextToken() != null) { }
    }
}
