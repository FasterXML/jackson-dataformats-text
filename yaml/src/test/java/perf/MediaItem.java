package perf;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.core.*;

/**
 * Value class for performance tests
 */
@JsonPropertyOrder({"content", "images"})
public class MediaItem
{
    final static String NAME_IMAGES = "images";
    final static String NAME_CONTENT = "content";
    
    public enum Player { JAVA, FLASH;  }
    public enum Size { SMALL, LARGE; }

    private List<Photo> _photos;
    private Content _content;

    public MediaItem() { }

    public MediaItem(Content c)
    {
        _content = c;
    }

    public void addPhoto(Photo p) {
        if (_photos == null) {
            _photos = new ArrayList<Photo>();
        }
        _photos.add(p);
    }
    
    public List<Photo> getImages() { return _photos; }
    public void setImages(List<Photo> p) { _photos = p; }

    public Content getContent() { return _content; }
    public void setContent(Content c) { _content = c; }

    // Custom deser
    public static MediaItem deserialize(JsonParser p) throws IOException
    {
        if (p.nextToken() != JsonToken.START_OBJECT) {
            throw new IOException("Need START_OBJECT for MediaItem");
        }
        MediaItem item = new MediaItem();
        while (p.nextToken() == JsonToken.PROPERTY_NAME) {
            String name = p.currentName();
            if (name == "images") {
                item._photos = deserializeImages(p);
            } else if (name == "content") {
                item._content = Content.deserialize(p);
            } else throw new IOException("Unknown field");
        }
        if (p.currentToken() != JsonToken.END_OBJECT) {
            throw new IOException("Need END_OBJECT to complete MediaItem");
        }
        return item;
    }
    
    private static List<Photo> deserializeImages(JsonParser p) throws IOException
    {
        if (p.nextToken() != JsonToken.START_ARRAY) {
            throw new IOException("Need START_ARRAY for List of Photos");
        }
        ArrayList<Photo> images = new ArrayList<Photo>(4);
        while (p.nextToken() == JsonToken.START_OBJECT) {
            images.add(Photo.deserialize(p));
        }
        if (p.currentToken() != JsonToken.END_ARRAY) {
            throw new IOException("Need END_ARRAY to complete List of Photos");
        }
        return images;
    }
    
    // Custom serializer
    public void serialize(JsonGenerator g) throws IOException
    {
        g.writeStartObject();

        g.writeName("content");
        if (_content == null) {
            g.writeNull();
        } else {
            _content.serialize(g);
        }
        if (_photos == null) {
            g.writeNullProperty("images");
        } else {
            g.writeArrayPropertyStart("images");
            for (Photo photo : _photos) {
                photo.serialize(g);
            }
            g.writeEndArray();
        }

        g.writeEndObject();
    }
    
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */
    
    @JsonPropertyOrder({"uri","title","width","height","size"})
    public static class Photo
    {
        public final static int F_URI = 1;
        public final static int F_TITLE = 2;
        public final static int F_WIDTH = 3;
        public final static int F_HEIGHT = 4;
        public final static int F_SIZE = 5;
        
        public final static HashMap<String,Integer> sFields = new HashMap<String,Integer>();
        static {
            // MediaItem fields
            sFields.put("uri", F_URI);
            sFields.put("title", F_TITLE);
            sFields.put("width", F_WIDTH);
            sFields.put("height", F_HEIGHT);
            sFields.put("size", F_SIZE);
        }
        
      private String _uri;
      private String _title;
      private int _width;
      private int _height;
      private Size _size;
    
      public Photo() {}
      public Photo(String uri, String title, int w, int h, Size s)
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

      private static Size findSize(String id)
      {
          if (id.charAt(0) == 'L') {
              if ("LARGE".equals(id)) {
                  return Size.LARGE;
              }
          } else if ("SMALL".equals(id)) {
              return Size.SMALL;
          }
          throw new IllegalArgumentException();
      }
      
      public static Photo deserialize(JsonParser p) throws IOException
      {
          Photo photo = new Photo();
          while (p.nextToken() == JsonToken.PROPERTY_NAME) {
              String name = p.currentName();
              p.nextToken();
              Integer I = sFields.get(name);
              if (I != null) {
                  switch (I.intValue()) {
                  case F_URI:
                      photo.setUri(p.getString());
                      continue;
                  case F_TITLE:
                      photo.setTitle(p.getString());
                      continue;
                  case F_WIDTH:
                      photo.setWidth(p.getIntValue());
                      continue;
                  case F_HEIGHT:
                      photo.setHeight(p.getIntValue());
                      continue;
                  case F_SIZE:
                      photo.setSize(findSize(p.getString()));
                      continue;
                  }
              }
              throw new IOException("Unknown field '"+name+"'");
          }
          if (p.currentToken() != JsonToken.END_OBJECT) {
              throw new IOException("Need END_OBJECT to complete Photo");
          }
          return photo;
      }
      
      public void serialize(JsonGenerator g) throws IOException
      {
          g.writeStartObject();
          g.writeStringProperty("uri", _uri);
          g.writeStringProperty("title", _title);
          g.writeNumberProperty("width", _width);
          g.writeNumberProperty("height", _height);
          g.writeStringProperty("size", (_size == null) ? null : _size.name());
          g.writeEndObject();
      }          
    }

    @JsonPropertyOrder({"player","uri","title","width","height","format","duration","size","bitrate","persons","copyright"})
    public static class Content
    {
        public final static int F_PLAYER = 0;
        public final static int F_URI = 1;
        public final static int F_TITLE = 2;
        public final static int F_WIDTH = 3;
        public final static int F_HEIGHT = 4;
        public final static int F_FORMAT = 5;
        public final static int F_DURATION = 6;
        public final static int F_SIZE = 7;
        public final static int F_BITRATE = 8;
        public final static int F_PERSONS = 9;
        public final static int F_COPYRIGHT = 10;
        
        public final static HashMap<String,Integer> sFields = new HashMap<String,Integer>();
        static {
            sFields.put("player", F_PLAYER);
            sFields.put("uri", F_URI);
            sFields.put("title", F_TITLE);
            sFields.put("width", F_WIDTH);
            sFields.put("height", F_HEIGHT);
            sFields.put("format", F_FORMAT);
            sFields.put("duration", F_DURATION);
            sFields.put("size", F_SIZE);
            sFields.put("bitrate", F_BITRATE);
            sFields.put("persons", F_PERSONS);
            sFields.put("copyright", F_COPYRIGHT);
        }
        
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
    
        public Content() { }

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

        private static Player findPlayer(String id)
        {
            if ("JAVA".equals(id)) {
                return Player.JAVA;
            }
            if ("FLASH".equals(id)) {
                return Player.FLASH;
            }
            throw new IllegalArgumentException("Weird Player value of '"+id+"'");
        }
        
        public static Content deserialize(JsonParser p) throws IOException
        {
            if (p.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Need START_OBJECT for Content");
            }
            Content content = new Content();

            while (p.nextToken() == JsonToken.PROPERTY_NAME) {
                String name = p.currentName();
                p.nextToken();
                Integer I = sFields.get(name);
                if (I != null) {
                    switch (I.intValue()) {
                    case F_PLAYER:
                        content.setPlayer(findPlayer(p.getString()));
                    case F_URI:
                        content.setUri(p.getString());
                        continue;
                    case F_TITLE:
                        content.setTitle(p.getString());
                        continue;
                    case F_WIDTH:
                        content.setWidth(p.getIntValue());
                        continue;
                    case F_HEIGHT:
                        content.setHeight(p.getIntValue());
                        continue;
                    case F_FORMAT:
                        content.setCopyright(p.getString());
                        continue;
                    case F_DURATION:
                        content.setDuration(p.getLongValue());
                        continue;
                    case F_SIZE:
                        content.setSize(p.getLongValue());
                        continue;
                    case F_BITRATE:
                        content.setBitrate(p.getIntValue());
                        continue;
                    case F_PERSONS:
                        content.setPersons(deserializePersons(p));
                        continue;
                    case F_COPYRIGHT:
                        content.setCopyright(p.getString());
                        continue;
                    }
                }
                throw new IOException("Unknown field '"+name+"'");
            }
            if (p.currentToken() != JsonToken.END_OBJECT) {
                throw new IOException("Need END_OBJECT to complete Content");
            }
            return content;
        }
        
        private static List<String> deserializePersons(JsonParser p) throws IOException
        {
            if (p.currentToken() != JsonToken.START_ARRAY) {
                throw new IOException("Need START_ARRAY for List of Persons (got "+p.currentToken()+")");
            }
            ArrayList<String> persons = new ArrayList<String>(4);
            while (p.nextToken() == JsonToken.VALUE_STRING) {
                persons.add(p.getString());
            }
            if (p.currentToken() != JsonToken.END_ARRAY) {
                throw new IOException("Need END_ARRAY to complete List of Persons");
            }
            return persons;
        }
        
        public void serialize(JsonGenerator g) throws IOException
        {
            g.writeStartObject();
            g.writeStringProperty("player", (_player == null) ? null : _player.name());
            g.writeStringProperty("uri", _uri);
            g.writeStringProperty("title", _title);
            g.writeNumberProperty("width", _width);
            g.writeNumberProperty("height", _height);
            g.writeStringProperty("format", _format);
            g.writeNumberProperty("duration", _duration);
            g.writeNumberProperty("size", _size);
            g.writeNumberProperty("bitrate", _bitrate);
            g.writeStringProperty("copyright", _copyright);
            if (_persons == null) {
                g.writeNullProperty("persons");
            } else {
                g.writeArrayPropertyStart("persons");
                for (String p : _persons) {
                    g.writeString(p);
                }
                g.writeEndArray();
            }
            g.writeEndObject();
        }          
    }
}
