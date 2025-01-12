package perf;

import tools.jackson.core.*;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Micro-benchmark for comparing performance of bean deserialization
 */
public final class YAMLDeserPerf
{
    /*
    /**********************************************************
    /* Actual test
    /**********************************************************
     */

    private final int REPS;

    private YAMLDeserPerf() {
        // Let's try to guestimate suitable size
        REPS = 9000;
    }

    private MediaItem buildItem()
    {
        MediaItem.Content content = new MediaItem.Content();
        content.setPlayer(MediaItem.Player.JAVA);
        content.setUri("http://javaone.com/keynote.mpg");
        content.setTitle("Javaone Keynote");
        content.setWidth(640);
        content.setHeight(480);
        content.setFormat("video/mpeg4");
        content.setDuration(18000000L);
        content.setSize(58982400L);
        content.setBitrate(262144);
        content.setCopyright("None");
        content.addPerson("Bill Gates");
        content.addPerson("Steve Jobs");

        MediaItem item = new MediaItem(content);

        item.addPhoto(new MediaItem.Photo("http://javaone.com/keynote_large.jpg", "Javaone Keynote", 1024, 768, MediaItem.Size.LARGE));
        item.addPhoto(new MediaItem.Photo("http://javaone.com/keynote_small.jpg", "Javaone Keynote", 320, 240, MediaItem.Size.SMALL));

        return item;
    }

    public void test() throws Exception
    {
        int sum = 0;

        final MediaItem item = buildItem();
//        JsonFactory jsonF = new JsonFactory();
//        final ObjectMapper jsonMapper = new ObjectMapper(jsonF);
        final ObjectMapper yamlMapper = new YAMLMapper();
        
//        final ObjectMapper jsonMapper = new ObjectMapper(jsonF);
//        jsonMapper.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);

        // Use Jackson?
//        byte[] json = jsonMapper.writeValueAsBytes(item);
        byte[] yaml = yamlMapper.writeValueAsBytes(item);
        
        System.out.println("Warmed up: data size is "+yaml.length+" bytes; "+REPS+" reps -> "
                +((REPS * yaml.length) >> 10)+" kB per iteration");
        System.out.println();

// for debugging:
// System.err.println("JSON = "+jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(item));
        
        int round = 0;
        while (true) {
//            try {  Thread.sleep(100L); } catch (InterruptedException ie) { }
//            int round = 2;

            long curr = System.currentTimeMillis();
            String msg;
            round = (++round % 2);

//if (true) round = 3; 
            
            boolean lf = (round == 0);

            switch (round) {
            case 0:
            case 1:
                msg = "Deserialize, bind, YAML";
                sum += testDeser(yamlMapper, yaml, REPS);
                break;

            /*
            case 0:
                msg = "Deserialize, manual, YAML";
                sum += testDeserUsingParser(yamlMapper, yaml, REPS);
                break;
                */

            default:
                throw new Error("Internal error");
            }

            curr = System.currentTimeMillis() - curr;
            if (lf) {
                System.out.println();
            }
            System.out.println("Test '"+msg+"' -> "+curr+" msecs ("
                               +(sum & 0xFF)+").");
        }
    }

    protected int testDeser(ObjectMapper mapper, byte[] input, int reps)
        throws Exception
    {
        JavaType type = mapper.constructType(MediaItem.class);
        MediaItem item = null;
        for (int i = 0; i < reps; ++i) {
            item = mapper.readValue(input, 0, input.length, type);
        }
        return item.hashCode(); // just to get some non-optimizable number
    }

    protected int testDeserUsingParser(ObjectMapper mapper, byte[] input, int reps)
        throws Exception
    {
        MediaItem item = null;
        for (int i = 0; i < reps; ++i) {
            JsonParser p = mapper.createParser(input);
            item = MediaItem.deserialize(p);
            p.close();
        }
        return item.hashCode(); // just to get some non-optimizable number
    }
    
    public static void main(String[] args) throws Exception
    {
        new YAMLDeserPerf().test();
    }
}
