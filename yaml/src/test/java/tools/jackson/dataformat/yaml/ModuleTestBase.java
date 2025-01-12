package tools.jackson.dataformat.yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import tools.jackson.core.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class ModuleTestBase
{
    /**
     * Slightly modified sample class from Jackson tutorial ("JacksonInFiveMinutes")
     */
    protected static class FiveMinuteUser {
        public enum Gender { MALE, FEMALE };

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
    }
    
    protected ModuleTestBase() { }

    /*
    /**********************************************************************
    /* Helper methods, setup
    /**********************************************************************
     */

    protected YAMLFactoryBuilder streamFactoryBuilder() {
        return YAMLFactory.builder();
    }

    protected YAMLMapper newObjectMapper() {
        return mapperBuilder().build();
    }

    protected YAMLMapper.Builder mapperBuilder() {
        return YAMLMapper.builder();
    }
    
    protected YAMLMapper.Builder mapperBuilder(YAMLFactory f) {
        return YAMLMapper.builder(f);
    }

    /*
    /**********************************************************
    /* Helper methods; low-level
    /**********************************************************
     */

    public String quote(String str) {
        return '"'+str+'"';
    }

    public byte[] utf8(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }
    
    protected void assertToken(JsonToken expToken, JsonParser jp)
    {
        assertToken(expToken, jp.currentToken());
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

    protected void assertYAML(String expOrig, String actOrig)
    {
        String exp = trimDocMarker(expOrig).trim();
        String act = trimDocMarker(actOrig).trim();
        if (!exp.equals(act)) {
            // use std assert to show more accurately where differences are:
            assertEquals(expOrig, actOrig);
        }
    }
    
    /**
     * Method that gets textual contents of the current token using
     * available methods, and ensures results are consistent, before
     * returning them
     */
    protected String getAndVerifyText(JsonParser jp)
    {
        // Ok, let's verify other accessors
        int actLen = jp.getStringLength();
        char[] ch = jp.getStringCharacters();
        String str2 = new String(ch, jp.getStringOffset(), actLen);
        String str = jp.getString();

        if (str.length() !=  actLen) {
            fail("Internal problem (jp.token == "+jp.currentToken()+"): jp.getText().length() ['"+str+"'] == "+str.length()+"; jp.getTextLength() == "+actLen);
        }
        assertEquals("String access via getText(), getTextXxx() must be the same", str, str2);

        return str;
    }

    protected void verifyFieldName(JsonParser p, String expName)
    {
        assertEquals(expName, p.getString());
        assertEquals(expName, p.currentName());
    }
    
    protected void verifyIntValue(JsonParser jp, long expValue)
    {
        // First, via textual
        assertEquals(String.valueOf(expValue), jp.getString());
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

    protected static String trimDocMarker(String doc)
    {
        if (doc.startsWith("---")) {
            doc = doc.substring(3);
        }
        return doc.trim();
    }

    protected byte[] readResource(String ref)
    {
       ByteArrayOutputStream bytes = new ByteArrayOutputStream();
       final byte[] buf = new byte[4000];

       try (InputStream in = getClass().getResourceAsStream(ref)) {
           if (in != null) {
               int len;
               while ((len = in.read(buf)) > 0) {
                   bytes.write(buf, 0, len);
               }
           }
       } catch (IOException e) {
           throw new RuntimeException("Failed to read resource '"+ref+"': "+e);
       }
       if (bytes.size() == 0) {
           throw new IllegalArgumentException("Failed to read resource '"+ref+"': empty resource?");
       }
       return bytes.toByteArray();
    }
}
