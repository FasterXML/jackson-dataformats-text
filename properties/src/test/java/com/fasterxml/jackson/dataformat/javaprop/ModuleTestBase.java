package com.fasterxml.jackson.dataformat.javaprop;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

public abstract class ModuleTestBase extends junit.framework.TestCase
{
    @JsonPropertyOrder({ "topLeft", "bottomRight" })
    static class Rectangle {
        public Point topLeft;
        public Point bottomRight;

        protected Rectangle() { }
        public Rectangle(Point p1, Point p2) {
            topLeft = p1;
            bottomRight = p2;
        }
    }

    @JsonPropertyOrder({ "x", "y" })
    static class Point {
        public int x, y;
        
        protected Point() { }
        public Point(int x0, int y0) {
            x = x0;
            y = y0;
        }
    }

    static class Points {
        public List<Point> p;

        protected Points() { }
        public Points(Point... p0) {
            p = Arrays.asList(p0);
        }
    }

    public enum Gender { MALE, FEMALE };

    /**
     * Slightly modified sample class from Jackson tutorial ("JacksonInFiveMinutes")
     */
    @JsonPropertyOrder({"firstName", "lastName", "gender" ,"verified", "userImage"})
    protected static class FiveMinuteUser {

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

    @JsonPropertyOrder({"id", "desc"})
    protected static class IdDesc {
        public String id, desc;

        protected IdDesc() { }
        public IdDesc(String id, String desc) {
            this.id = id;
            this.desc = desc;
        }
    }
    
    protected ModuleTestBase() { }

    /*
    /**********************************************************************
    /* Helper methods, setup
    /**********************************************************************
     */

    protected JavaPropsMapper mapperForProps()
    {
        return new JavaPropsMapper();
    }

    /*
    /**********************************************************
    /* Helper methods; read helpers
    /**********************************************************
     */

    protected final <T> T _mapFrom(ObjectMapper mapper, String input, Class<T> type,
            boolean useBytes)
        throws IOException
    {
        if (useBytes) {
            InputStream in = new ByteArrayInputStream(input.getBytes("ISO-8859-1"));
            return mapper.readValue(in, type);
        }
        return mapper.readValue(new StringReader(input), type);
    }

    protected final <T> T _mapFrom(ObjectReader reader, String input, Class<T> type,
            boolean useBytes)
        throws IOException
    {
        if (useBytes) {
            InputStream in = new ByteArrayInputStream(input.getBytes("ISO-8859-1"));
            return reader.forType(type).readValue(in);
        }
        return reader.forType(type).readValue(new StringReader(input));
    }

    /*
    /**********************************************************
    /* Helper methods; low-level
    /**********************************************************
     */

    public String quote(String str) {
        return '"'+str+'"';
    }

    protected String aposToQuotes(String json) {
        return json.replace("'", "\"");
    }

    protected void assertToken(JsonToken expToken, JsonToken actToken) {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }
    
    protected void assertToken(JsonToken expToken, JsonParser jp) {
        assertToken(expToken, jp.getCurrentToken());
    }

    protected void assertType(Object ob, Class<?> expType)
    {
        if (ob == null) {
            fail("Expected an object of type "+expType.getName()+", got null");
        }
        Class<?> cls = ob.getClass();
        if (!expType.isAssignableFrom(cls)) {
            fail("Expected type "+expType.getName()+", got "+cls.getName());
        }
    }

    /**
     * Method that gets textual contents of the current token using
     * available methods, and ensures results are consistent, before
     * returning them
     */
    protected String getAndVerifyText(JsonParser jp) throws IOException
    {
        // Ok, let's verify other accessors
        int actLen = jp.getTextLength();
        char[] ch = jp.getTextCharacters();
        String str2 = new String(ch, jp.getTextOffset(), actLen);
        String str = jp.getText();

        if (str.length() !=  actLen) {
            fail("Internal problem (jp.token == "+jp.getCurrentToken()+"): jp.getText().length() ['"+str+"'] == "+str.length()+"; jp.getTextLength() == "+actLen);
        }
        assertEquals("String access via getText(), getTextXxx() must be the same", str, str2);

        return str;
    }

    protected void verifyFieldName(JsonParser jp, String expName)
        throws IOException
    {
        assertEquals(expName, jp.getText());
        assertEquals(expName, jp.getCurrentName());
    }
    
    protected void verifyIntValue(JsonParser jp, long expValue)
        throws IOException
    {
        // First, via textual
        assertEquals(String.valueOf(expValue), jp.getText());
    }
    
    protected void verifyException(Throwable e, String... matches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("+Arrays.asList(matches)+"): got one with message \""+msg+"\"");
    }
}
