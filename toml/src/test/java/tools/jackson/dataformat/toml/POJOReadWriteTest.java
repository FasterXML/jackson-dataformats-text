package tools.jackson.dataformat.toml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

// Composed of pieces from other format modules' tests
public class POJOReadWriteTest extends TomlMapperTestBase
{
    @JsonPropertyOrder({ "topLeft", "bottomRight" })
    protected static class Rectangle {
        public Point topLeft;
        public Point bottomRight;

        protected Rectangle() { }
        public Rectangle(Point p1, Point p2) {
            topLeft = p1;
            bottomRight = p2;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Rectangle)) return false;
            Rectangle other = (Rectangle) o;
            return Objects.equals(this.topLeft, other.topLeft)
                    && Objects.equals(this.bottomRight, other.bottomRight);
        }

        @Override
        public String toString() {
            return "Rectangle(topLeft="+topLeft+"/bottomRight="+bottomRight+")";
        }
    }

    @JsonPropertyOrder({ "x", "y" })
    protected static class Point {
        public int x, y;

        protected Point() { }
        public Point(int x0, int y0) {
            x = x0;
            y = y0;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Point)) return false;
            Point other = (Point) o;
            return (this.x == other.x) && (this.y == other.y);
        }

        @Override
        public String toString() {
            return "Point(x="+x+",y="+y+")";
        }
    }

    protected static class PointWrapper {
        public Point point;

        public PointWrapper(Point p) { point = p; }
        protected PointWrapper() { }
    }

    @JsonPropertyOrder({ "ids", "points" })
    protected static class PointListBean {
        public List<String> ids;
        public List<Point> points;

        protected PointListBean() { }
        protected PointListBean(List<String> ids, List<Point> points) {
            this.ids = ids;
            this.points = points;
        }
    }

    public enum Gender { MALE, FEMALE };

    /**
     * Slightly modified sample class from Jackson tutorial ("JacksonInFiveMinutes")
     */
    @JsonPropertyOrder({"firstName", "lastName", "gender" ,"verified", "userImage"})
    protected static class FiveMinuteUser
    {
        private Gender _gender;

        public String firstName, lastName;

        private boolean _isVerified;
        private byte[] _userImage;

        public FiveMinuteUser() { }

        public FiveMinuteUser(String first, String last, boolean verified, Gender g, byte[] data)
        {
            firstName = first;
            lastName = last;
            _isVerified = verified;
            _gender = g;
            _userImage = data;
        }

        public boolean isVerified() { return _isVerified; }
        public Gender getGender() { return _gender; }
        public byte[] getUserImage() { return _userImage; }

        public void setVerified(boolean b) { _isVerified = b; }
        public void setGender(Gender g) { _gender = g; }
        public void setUserImage(byte[] b) { _userImage = b; }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) return true;
            if (o == null || o.getClass() != getClass()) return false;
            FiveMinuteUser other = (FiveMinuteUser) o;
            if (_isVerified != other._isVerified) return false;
            if (_gender != other._gender) return false;
            if (!firstName.equals(other.firstName)) return false;
            if (!lastName.equals(other.lastName)) return false;
            byte[] otherImage = other._userImage;
            if (otherImage.length != _userImage.length) return false;
            for (int i = 0, len = _userImage.length; i < len; ++i) {
                if (_userImage[i] != otherImage[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            // not really good but whatever:
            return firstName.hashCode();
        }
    }

    private final ObjectMapper MAPPER = newTomlMapper();
    private final ObjectMapper JSON_MAPPER = new JsonMapper();

    @Test
    public void testSimpleEmployee() throws Exception
    {
        final FiveMinuteUser input = new FiveMinuteUser("Bob", "Palmer", true, Gender.MALE,
                new byte[] { 1, 2, 3, 4 });
        final String EXP = "firstName = 'Bob'\n"
            +"lastName = 'Palmer'\n"
            +"gender = 'MALE'\n"
            +"verified = true\n"
            +"userImage = 'AQIDBA=='\n";

        String output = MAPPER.writeValueAsString(input);
        assertEquals(EXP, output);

        // So far so good; let's read back
        FiveMinuteUser result = MAPPER.readerFor(FiveMinuteUser.class)
                .readValue(output);
        assertEquals(input, result);

        // and with conversion too
        JsonNode treeFromToml = MAPPER.readTree(output);
        assertTrue(treeFromToml.isObject());
        assertEquals(5, treeFromToml.size());

        // Ensure that POJO->TOML->Tree is same as POJO->JSON->Tree
        final String json = JSON_MAPPER.writeValueAsString(input);
        final JsonNode treeViaJson = JSON_MAPPER.readTree(json);

        assertEquals(treeViaJson.toPrettyString(),
                treeFromToml.toPrettyString());

        assertEquals(treeViaJson.get("firstName"),
                treeFromToml.get("firstName"));
        assertEquals(treeViaJson.get("lastName"),
                treeFromToml.get("lastName"));
        assertEquals(treeViaJson.get("gender"),
                treeFromToml.get("gender"));
        assertEquals(treeViaJson.get("verified"),
                treeFromToml.get("verified"));
        assertEquals(treeViaJson.get("userImage"),
                treeFromToml.get("userImage"));

        assertEquals(treeViaJson, treeFromToml);
    }

    @Test
    public void testSimpleRectangle() throws Exception
    {
        final Rectangle input = new Rectangle(new Point(1, -2), new Point(5, 10));
        byte[] toml = MAPPER.writeValueAsBytes(input);
        assertEquals("topLeft.x = 1\n"
                +"topLeft.y = -2\n"
                +"bottomRight.x = 5\n"
                +"bottomRight.y = 10\n"
                , new String(toml, StandardCharsets.UTF_8));

        Rectangle result;

        // Read first from static byte[]
        result = MAPPER.readerFor(Rectangle.class)
                .readValue(toml);
        assertNotNull(result.topLeft);
        assertNotNull(result.bottomRight);
        assertEquals(input, result);

        // and then via InputStream
        result = MAPPER.readerFor(Rectangle.class)
                .readValue(new ByteArrayInputStream(toml));
        assertNotNull(result.topLeft);
        assertNotNull(result.bottomRight);
        assertEquals(input, result);
    }

    // Check to see if Objects in Arrays work wrt generation or not
    @Test
    public void testPOJOListBean() throws Exception
    {
        final PointListBean input = new PointListBean(
                Arrays.asList("a", "b"),
                Arrays.asList(new Point(1, 2),
                        new Point(3, 4))
        );

        final String toml = MAPPER.writeValueAsString(input);
        PointListBean result = MAPPER.readerFor(PointListBean.class)
                .readValue(toml);
        assertNotNull(result.points);
        assertEquals(input.points, result.points);
    }
}
