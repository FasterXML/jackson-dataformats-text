package tools.jackson.dataformat.toml.testutil;

import java.util.concurrent.atomic.AtomicInteger;

import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.RecyclerPool;

/**
 * {@link RecyclerPool} that hands out {@link BufferRecycler}s which count buffer
 * allocations and releases, so tests can assert that buffers taken from the recycler
 * are actually handed back (instead of being leaked).
 */
public class CountingRecyclerPool implements RecyclerPool<BufferRecycler>
{
    private static final long serialVersionUID = 1L;

    private final AtomicInteger _readIOAllocs = new AtomicInteger();
    private final AtomicInteger _readIOReleases = new AtomicInteger();
    private final AtomicInteger _tokenAllocs = new AtomicInteger();
    private final AtomicInteger _tokenReleases = new AtomicInteger();

    @Override
    public BufferRecycler acquirePooled() {
        return new CountingBufferRecycler();
    }

    @Override
    public void releasePooled(BufferRecycler recycler) { }

    public int readIOAllocs() { return _readIOAllocs.get(); }
    public int readIOReleases() { return _readIOReleases.get(); }
    public int tokenAllocs() { return _tokenAllocs.get(); }
    public int tokenReleases() { return _tokenReleases.get(); }

    @Override
    public String toString() {
        return "[readIO: allocs="+readIOAllocs()+", releases="+readIOReleases()
            +"; token: allocs="+tokenAllocs()+", releases="+tokenReleases()+"]";
    }

    private class CountingBufferRecycler extends BufferRecycler
    {
        @Override
        public byte[] allocByteBuffer(int ix, int minSize) {
            if (ix == BufferRecycler.BYTE_READ_IO_BUFFER) {
                _readIOAllocs.incrementAndGet();
            }
            return super.allocByteBuffer(ix, minSize);
        }

        @Override
        public void releaseByteBuffer(int ix, byte[] buffer) {
            if (ix == BufferRecycler.BYTE_READ_IO_BUFFER) {
                _readIOReleases.incrementAndGet();
            }
            super.releaseByteBuffer(ix, buffer);
        }

        @Override
        public char[] allocCharBuffer(int ix, int minSize) {
            if (ix == BufferRecycler.CHAR_TOKEN_BUFFER) {
                _tokenAllocs.incrementAndGet();
            }
            return super.allocCharBuffer(ix, minSize);
        }

        @Override
        public void releaseCharBuffer(int ix, char[] buffer) {
            if (ix == BufferRecycler.CHAR_TOKEN_BUFFER) {
                _tokenReleases.incrementAndGet();
            }
            super.releaseCharBuffer(ix, buffer);
        }
    }
}
