package tools.jackson.dataformat.csv.schema;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;

import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import tools.jackson.dataformat.csv.ModuleTestBase;

import static org.junit.jupiter.api.Assertions.*;

// Trying to reproduce [dataformats-text#207], with mixed success
public class SchemaFromBuilder207Test extends ModuleTestBase
{
    @JsonDeserialize(builder=SimpleBuilderXY.class)
    static class ValueClassXY
    {
        final int _x, _y, _z;

        protected ValueClassXY(int x, int y, int z) {
            _x = x+1;
            _y = y+1;
            _z = z+1;
        }

        public int getX() { return _x; }
        public int getY() { return _y; }

        public void setZ(int z) { }
    }

    static class SimpleBuilderXY
    {
        public int x, y, z;
     
        public SimpleBuilderXY withX(int x0) {
              this.x = x0;
              return this;
        }

        public SimpleBuilderXY withY(int y0) {
              this.y = y0;
              return this;
        }

        public SimpleBuilderXY withZ(int z0) {
            this.z = z0;
            return this;
        }

        public ValueClassXY build() {
              return new ValueClassXY(x, y, z);
        }
    }

    private final CsvMapper MAPPER = newObjectMapper();

    @Test
    public void testSimple() throws Exception
    {
        CsvSchema schema = MAPPER.schemaFor(ValueClassXY.class).withHeader();        
        String output = MAPPER.writer().with(schema)
                .writeValueAsString(new ValueClassXY(1, 3, 5));
        assertEquals("x,y\n2,4", output.trim());
    }
}
