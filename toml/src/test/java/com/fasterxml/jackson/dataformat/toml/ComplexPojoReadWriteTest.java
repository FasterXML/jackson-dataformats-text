package com.fasterxml.jackson.dataformat.toml;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

// Copied from YAML modules "DatabindAdvancedTest"
public class ComplexPojoReadWriteTest extends TomlMapperTestBase
{
    enum Size { SMALL, LARGE; }

    static class MediaItem
    {
        private MediaContent _content;
        private List<Image> _images;

        public MediaItem() { }

        public MediaItem(MediaContent c) {
            _content = c;
        }

        public void addPhoto(Image p) {
            if (_images == null) {
                _images = new ArrayList<Image>();
            }
            _images.add(p);
        }

        public List<Image> getImages() { return _images; }
        public void setImages(List<Image> p) { _images = p; }

        public MediaContent getContent() { return _content; }
        public void setContent(MediaContent c) { _content = c; }
    }

    static class MediaContent
    {
        public enum Player { JAVA, FLASH;  }

        private Player _player;
        private String _uri;
        private String _title;
        private int _width;
        private int _height;
        private String _format;
        private long _duration;
        private long _size;
        private int _bitrate;
        private List<String> _persons;
        private String _copyright;

        public MediaContent() { }

        protected MediaContent(MediaContent src) {
            _player = src._player;
            _uri = src._uri;
            _title = src._title;
            _width = src._width;
            _height = src._height;
            _format = src._format;
            _duration = src._duration;
            _size = src._size;
            _bitrate = src._bitrate;
            _persons = src._persons;
            _copyright = src._copyright;
        }

        public void addPerson(String p) {
            if (_persons == null) {
                _persons = new ArrayList<String>();
            }
            _persons.add(p);
        }

        public Player getPlayer() { return _player; }
        public String getUri() { return _uri; }
        public String getTitle() { return _title; }
        public int getWidth() { return _width; }
        public int getHeight() { return _height; }
        public String getFormat() { return _format; }
        public long getDuration() { return _duration; }
        public long getSize() { return _size; }
        public int getBitrate() { return _bitrate; }
        public List<String> getPersons() { return _persons; }
        public String getCopyright() { return _copyright; }

        public void setPlayer(Player p) { _player = p; }
        public void setUri(String u) {  _uri = u; }
        public void setTitle(String t) {  _title = t; }
        public void setWidth(int w) {  _width = w; }
        public void setHeight(int h) {  _height = h; }
        public void setFormat(String f) {  _format = f;  }
        public void setDuration(long d) {  _duration = d; }
        public void setSize(long s) {  _size = s; }
        public void setBitrate(int b) {  _bitrate = b; }
        public void setPersons(List<String> p) {  _persons = p; }
        public void setCopyright(String c) {  _copyright = c; }
    }

    static class Image
    {
        private String _uri;
        private String _title;
        private int _width;
        private int _height;
        private Size _size;

        public Image() {}
        public Image(String uri, String title, int w, int h, Size s)
        {
          _uri = uri;
          _title = title;
          _width = w;
          _height = h;
          _size = s;
        }

        public String getUri() { return _uri; }
        public String getTitle() { return _title; }
        public int getWidth() { return _width; }
        public int getHeight() { return _height; }
        public Size getSize() { return _size; }

        public void setUri(String u) { _uri = u; }
        public void setTitle(String t) { _title = t; }
        public void setWidth(int w) { _width = w; }
        public void setHeight(int h) { _height = h; }
        public void setSize(Size s) { _size = s; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // For [dataformats-text#260]: resetting (or lack thereof) of "inlined" flag
    @Test
    public void testReadWriteComplexPojo() throws Exception
    {
        ObjectMapper mapper = newTomlMapper();

        MediaItem input = new MediaItem();
        MediaContent content = new MediaContent();
        content.setTitle("Databind test");
        content.addPerson("William");
        content.addPerson("Robert");
        content.setFormat("jpeg");
        content.setWidth(900);
        content.setHeight(120);
        content.setBitrate(256000);
        content.setDuration(3600 * 1000L);
        content.setCopyright("none");
        content.setPlayer(MediaContent.Player.FLASH);
        content.setUri("http://whatever.biz");
        input.setContent(content);
        input.addPhoto(new Image("http://a.com", "title1", 200, 100, Size.LARGE));
        input.addPhoto(new Image("http://b.org", "title2", 640, 480, Size.SMALL));

        final String toml = mapper.writeValueAsString(input);

        MediaItem result = mapper.readValue(toml, MediaItem.class);
        assertNotNull(result);
        assertNotNull(result.getContent());
        assertEquals(900, result.getContent().getWidth());
        assertEquals(120, result.getContent().getHeight());
        assertNotNull(result.getImages());
        assertEquals(2, result.getImages().size());
        assertEquals(Size.SMALL, result.getImages().get(1).getSize());
    }
}
