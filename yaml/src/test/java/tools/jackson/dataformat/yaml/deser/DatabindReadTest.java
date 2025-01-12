package tools.jackson.dataformat.yaml.deser;

import java.io.ByteArrayInputStream;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.dataformat.yaml.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for checking functioning of the databinding
 * on top of YAML layer.
 */
public class DatabindReadTest extends ModuleTestBase
{
    static class EmptyBean {
    }

    static class Outer {
        public Name name;
        public int age;
    }

    static class Name {
        public String first, last;
    }

    // [dataformats-text#205]
    static class Release205 {
        public String version;
        public List<Artifact205> artifacts;
    }

    static class Artifact205 {
        @JsonProperty("jira-project")
        private String jiraProject;
        private String name;
        private String version;
        public String getJiraProject() {
            return jiraProject;
        }

        public void setJiraProject(String jiraProject) {
            this.jiraProject = jiraProject;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newObjectMapper();
    
    @Test
    public void testSimpleNested() throws Exception
    {
        final String YAML =
 "name:\n"
+"  first: Bob\n"
+"  last: De Burger\n"
+"age: 28"
;
        // first, no doc marker
        Outer outer = MAPPER.readValue(YAML, Outer.class);
        assertNotNull(outer);
        assertNotNull(outer.name);
        assertEquals("Bob", outer.name.first);
        assertEquals("De Burger", outer.name.last);
        assertEquals(28, outer.age);

        // then with
        Outer outer2 = MAPPER.readValue("---\n"+YAML, Outer.class);
        assertNotNull(outer2);
        assertNotNull(outer2.name);
        assertEquals(outer.name.first, outer2.name.first);
        assertEquals(outer.name.last, outer2.name.last);
        assertEquals(outer.age, outer2.age);
        
    }
    
    @Test
    public void testBasicUntyped() throws Exception
    {
        final String YAML =
 "template: Hello, %s!\n"
+"database:\n"
+"  driverClass: org.h2.Driver\n"
+"  user: scott\n"
+"  password: tiger\n"
+"  extra: [1,2]"
;
        Map<?,?> result = MAPPER.readValue(YAML, Map.class);
        // sanity check first:
        assertEquals(2, result.size());
        // then literal comparison; easiest to just write as JSON...
        ObjectMapper jsonMapper = new ObjectMapper();
        String json = jsonMapper.writeValueAsString(result);
        String EXP = "{\"template\":\"Hello, %s!\",\"database\":{"
                +"\"driverClass\":\"org.h2.Driver\",\"user\":\"scott\",\"password\":\"tiger\","
                +"\"extra\":[1,2]}}";
        assertEquals(EXP, json);
    }

    @Test
    public void testBasicPOJO() throws Exception
    {
        final String YAML =
"firstName: Billy\n"
+"lastName: Baggins\n"                
+"gender: MALE\n"        
+"verified: true\n"
+"userImage: AQIDBAU=" // [1,2,3,4,5]
;
        FiveMinuteUser user = MAPPER.readValue(YAML, FiveMinuteUser.class);
        assertEquals("Billy", user.firstName);
        assertEquals("Baggins", user.lastName);
        assertEquals(FiveMinuteUser.Gender.MALE, user.getGender());
        assertTrue(user.isVerified());
        byte[] data = user.getUserImage();
        assertNotNull(data);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, data);
    }

    @Test
    public void testIssue1() throws Exception
    {
        final byte[] YAML = "firstName: Billy".getBytes("UTF-8");
        FiveMinuteUser user = new FiveMinuteUser();
        user.firstName = "Bubba";
        MAPPER.readerForUpdating(user).readValue(new ByteArrayInputStream(YAML));
        assertEquals("Billy", user.firstName);
    }

    @Test
    public void testUUIDs() throws Exception
    {
        UUID uuid = new UUID(0, 0);
        String yaml = MAPPER.writeValueAsString(uuid);
        UUID result = MAPPER.readValue(yaml, UUID.class);
        
        assertEquals(uuid, result);
    }

    @Test
    public void testEmptyBean() throws Exception
    {
        String yaml = MAPPER.writer()
                .without(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .writeValueAsString(new EmptyBean());
        yaml = yaml.trim();
        
        // let's be bit more robust; may or may not get doc marker
        if (yaml.startsWith("---")) {
            yaml = yaml.substring(3);
        }
        yaml = yaml.trim();
        assertEquals("{}", yaml);
    }

    // [dataformats-text#205]
    @Test
    public void testRenamingIssue205() throws Exception
    {
        final String YAML =
"version: 2.3.0.RELEASE\n" +
"artifacts:\n" +
" - jira-project: https://issues.redhat.com/projects/EAPSUP/issues\n" +
"   name: hibernate\n" +
"   version: 5.0.15";

        Release205 release = MAPPER.readValue(YAML, Release205.class);
        assertNotNull(release);
        assertNotNull(release.artifacts);
        assertEquals(release.artifacts.get(0).getVersion(),"5.0.15");
    }
}
