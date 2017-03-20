package perf;

import java.io.IOException;
import java.io.OutputStream;

public class BogusOutputStream extends OutputStream
{
    protected int _bytes;

    @Override
    public void write(byte[] buf) { write(buf, 0, buf.length); }
    @Override
    public void write(byte[] buf, int offset, int len) {
        _bytes += len;
    }

    @Override
    public void write(int b) throws IOException {
        _bytes++;
    }

    public int length() { return _bytes; }
}
