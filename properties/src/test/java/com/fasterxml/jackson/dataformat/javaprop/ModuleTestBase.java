package com.fasterxml.jackson.dataformat.javaprop;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class ModuleTestBase
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
    }

    @JsonPropertyOrder({ "x", "y" })
    protected static class Point {
        public int x, y;
        
        protected Point() { }
        public Point(int x0, int y0) {
            x = x0;
            y = y0;
        }
    }

    protected static class Points {
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

    protected JavaPropsFactoryBuilder propertiesFactoryBuilder() {
        return JavaPropsFactory.builder();
    }

    protected JavaPropsMapper newPropertiesMapper() {
        return propertiesMapperBuilder().build();
    }
    
    protected JavaPropsMapper.Builder propertiesMapperBuilder() {
        return JavaPropsMapper.builder();
    }

    protected JavaPropsMapper.Builder propertiesMapperBuilder(JavaPropsFactory f) {
        return JavaPropsMapper.builder(f);
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

    public String q(String str) {
        return '"'+str+'"';
    }

    protected String a2q(String json) {
        return json.replace("'", "\"");
    }

    public byte[] utf8(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    protected void assertToken(JsonToken expToken, JsonToken actToken) {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }
    
    protected void assertToken(JsonToken expToken, JsonParser p) {
        assertToken(expToken, p.getCurrentToken());
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
    protected String getAndVerifyText(JsonParser p) throws IOException
    {
        // Ok, let's verify other accessors
        int actLen = p.getTextLength();
        char[] ch = p.getTextCharacters();
        String str2 = new String(ch, p.getTextOffset(), actLen);
        String str = p.getText();

        if (str.length() !=  actLen) {
            fail("Internal problem (p.token == "+p.getCurrentToken()+"): p.getText().length() ['"+str+"'] == "+str.length()+"; p.getTextLength() == "+actLen);
        }
        assertEquals("String access via getText(), getTextXxx() must be the same", str, str2);

        return str;
    }

    protected void verifyFieldName(JsonParser p, String expName)
        throws IOException
    {
        assertEquals(expName, p.getText());
        assertEquals(expName, p.currentName());
    }
    
    protected void verifyIntValue(JsonParser p, long expValue)
        throws IOException
    {
        // First, via textual
        assertEquals(String.valueOf(expValue), p.getText());
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
