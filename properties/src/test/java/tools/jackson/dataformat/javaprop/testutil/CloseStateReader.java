package tools.jackson.dataformat.javaprop.testutil;

import java.io.*;

public class CloseStateReader extends FilterReader
{
    public boolean closed = false;

    public CloseStateReader(Reader r) { super(r); }

    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
    }

    public static CloseStateReader forString(String input) throws IOException {
        return new CloseStateReader(new StringReader(input));
    }
}
