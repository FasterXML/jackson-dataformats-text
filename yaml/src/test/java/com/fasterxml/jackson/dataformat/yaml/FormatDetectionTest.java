package com.fasterxml.jackson.dataformat.yaml;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.format.DataFormatDetector;
import com.fasterxml.jackson.core.format.DataFormatMatcher;
import com.fasterxml.jackson.core.format.MatchStrength;

public class FormatDetectionTest extends ModuleTestBase
{
   /**
    * One nifty thing YAML has is the "---" start-doc indicator, which
    * makes it possible to auto-detect format...
    */
   public void testFormatDetection() throws Exception
   {
       YAMLFactory yamlF = new YAMLFactory();
       JsonFactory jsonF = new JsonFactory();
       DataFormatDetector det = new DataFormatDetector(new JsonFactory[] { yamlF, jsonF });
       // let's accept about any match; but only if no "solid match" found
       det = det.withMinimalMatch(MatchStrength.WEAK_MATCH).withOptimalMatch(MatchStrength.SOLID_MATCH);

       // First, give a JSON document...
       DataFormatMatcher match = det.findFormat("{ \"name\" : \"Bob\" }".getBytes("UTF-8"));
       assertNotNull(match);
       assertEquals(jsonF.getFormatName(), match.getMatchedFormatName());
       // and verify we can parse it
       JsonParser p = match.createParserWithMatch();
       assertToken(JsonToken.START_OBJECT, p.nextToken());
       assertToken(JsonToken.FIELD_NAME, p.nextToken());
       assertEquals("name", p.getCurrentName());
       assertToken(JsonToken.VALUE_STRING, p.nextToken());
       assertEquals("Bob", p.getText());
       assertToken(JsonToken.END_OBJECT, p.nextToken());
       p.close();

       // then YAML
       match = det.findFormat("---\nname: Bob\n".getBytes("UTF-8"));
       assertNotNull(match);
       assertEquals(yamlF.getFormatName(), match.getMatchedFormatName());
       // and parsing
       p = match.createParserWithMatch();
       assertToken(JsonToken.START_OBJECT, p.nextToken());
       assertToken(JsonToken.FIELD_NAME, p.nextToken());
       assertEquals("name", p.getCurrentName());
       assertToken(JsonToken.VALUE_STRING, p.nextToken());
       assertEquals("Bob", p.getText());
       assertToken(JsonToken.END_OBJECT, p.nextToken());
       p.close();
   }
}
