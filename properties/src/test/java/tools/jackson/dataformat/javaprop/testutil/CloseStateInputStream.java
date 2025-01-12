package tools.jackson.dataformat.javaprop.testutil;

import java.io.*;

public class CloseStateInputStream extends FilterInputStream
{
    public boolean closed = false;

    public CloseStateInputStream(InputStream in) { super(in); }

    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
    }

    public static CloseStateInputStream forString(String input) throws IOException {
        return new CloseStateInputStream(new ByteArrayInputStream(
                input.getBytes("UTF-8")));
    }
}
