package com.fasterxml.jackson.dataformat.csv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

public abstract class ModuleTestBase extends junit.framework.TestCase
{
    public enum Gender { MALE, FEMALE };

    protected static class Address {
        private String streetName;

        private String city;

        public Address(String streetName, String city) {
            this.streetName = streetName;
            this.city = city;
        }

        public String getStreetName() {
            return streetName;
        }

        public void setStreetName(String streetName) {
            this.streetName = streetName;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    protected static class LocalizedValue {
        private String value;

        public LocalizedValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

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

    public static class TenMinuteUser extends FiveMinuteUser {

        private Address _address;

        public TenMinuteUser(String first, String last, boolean verified, Gender g, byte[] data, Address address)
        {
            super(first, last, verified, g, data);
            _address = address;
        }

        public Address getAddress() {
            return _address;
        }

        public void setAddress(Address address) {
            this._address = address;
        }
    }

    public static class FifteenMinuteUser extends FiveMinuteUser {

        private Map<Locale, LocalizedValue> localizedName;

        public FifteenMinuteUser(String first, String last, boolean verified, Gender g, byte[] data, Map<Locale, LocalizedValue> localizedName) {
            super(first, last, verified, g, data);
            this.localizedName = localizedName;
        }

        public Map<Locale, LocalizedValue> getLocalizedName() {
            return localizedName;
        }

        public void setLocalizedName(Map<Locale, LocalizedValue> localizedName) {
            this.localizedName = localizedName;
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

    protected ModuleTestBase() { }

    /*
    /**********************************************************************
    /* Helper methods, setup
    /**********************************************************************
     */

    protected CsvFactoryBuilder streamFactoryBuilder() {
        return CsvFactory.builder();
    }

    protected CsvMapper mapperForCsv() {
        return new CsvMapper();
    }

    protected CsvMapper newObjectMapper() {
        return CsvMapper.builder().build();
    }
    
    protected CsvMapper.Builder mapperBuilder() {
        return CsvMapper.builder();
    }
    
    protected CsvMapper.Builder mapperBuilder(CsvFactory f) {
        return CsvMapper.builder(f);
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

    protected String aposToQuotes(String json) {
        return json.replace("'", "\"");
    }

    protected void assertToken(JsonToken expToken, JsonToken actToken) {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }
    
    protected void assertToken(JsonToken expToken, JsonParser jp) {
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
            fail("Internal problem (jp.token == "+jp.currentToken()+"): jp.getText().length() ['"+str+"'] == "+str.length()+"; jp.getTextLength() == "+actLen);
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
