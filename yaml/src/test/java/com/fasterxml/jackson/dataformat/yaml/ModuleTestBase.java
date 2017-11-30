package com.fasterxml.jackson.dataformat.yaml;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;

public abstract class ModuleTestBase extends junit.framework.TestCase
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

    protected YAMLMapper newObjectMapper()
    {
        return new YAMLMapper();
    }
    
    /*
    /**********************************************************
    /* Helper methods; low-level
    /**********************************************************
     */

    public String quote(String str) {
        return '"'+str+'"';
    }

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }
    
    protected void assertToken(JsonToken expToken, JsonParser jp)
    {
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
        throws IOException, JsonParseException
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

    protected static String trimDocMarker(String doc)
    {
        if (doc.startsWith("---")) {
            doc = doc.substring(3).trim();
        }
        return doc;
    }
}
