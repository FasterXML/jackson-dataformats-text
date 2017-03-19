package com.fasterxml.jackson.dataformat.csv.impl;

import java.io.IOException;

/**
 * Helper class used for holding values for a while until output
 * can proceed in expected order.
 */
public abstract class BufferedValue
{
    protected BufferedValue() { }

    public abstract void write(CsvEncoder w) throws IOException;

    public static BufferedValue buffered(String v) { return new TextValue(v); }
    public static BufferedValue bufferedRaw(String v) { return new RawValue(v); }
    public static BufferedValue buffered(int v) { return new IntValue(v); }
    public static BufferedValue buffered(long v) { return new LongValue(v); }
    public static BufferedValue buffered(double v) { return new DoubleValue(v); }
    public static BufferedValue buffered(boolean v) {
        return v ? BooleanValue.TRUE : BooleanValue.FALSE;
    }

    public static BufferedValue bufferedNull() {
        return NullValue.std;
    }

    protected final static class TextValue extends BufferedValue
    {
        private final String _value;
        
        public TextValue(String v) { _value = v; }

        @Override
        public void write(CsvEncoder w) throws IOException {
            w.appendValue(_value);
        }
    }

    /**
     * @since 2.5
     */
    protected final static class RawValue extends BufferedValue
    {
        private final String _value;
        
        public RawValue(String v) { _value = v; }

        @Override
        public void write(CsvEncoder w) throws IOException {
            w.appendRawValue(_value);
        }
    }
    
    protected final static class IntValue extends BufferedValue
    {
        private final int _value;
        
        public IntValue(int v) { _value = v; }

        @Override
        public void write(CsvEncoder w) throws IOException {
            w.appendValue(_value);
        }
    }

    protected final static class LongValue extends BufferedValue
    {
        private final long _value;
        
        public LongValue(long v) { _value = v; }

        @Override
        public void write(CsvEncoder w) throws IOException {
            w.appendValue(_value);
        }
    }

    protected final static class DoubleValue extends BufferedValue
    {
        private final double _value;
        
        public DoubleValue(double v) { _value = v; }

        @Override
        public void write(CsvEncoder w) throws IOException {
            w.appendValue(_value);
        }
    }

    protected final static class BooleanValue extends BufferedValue
    {
        public final static BooleanValue FALSE = new BooleanValue(false);
        public final static BooleanValue TRUE = new BooleanValue(true);

        private final boolean _value;
        
        public BooleanValue(boolean v) { _value = v; }

        @Override
        public void write(CsvEncoder w) throws IOException {
            w.appendValue(_value);
        }
    }

    protected final static class NullValue extends BufferedValue {
        public final static NullValue std = new NullValue();
        
        private NullValue() { }

        @Override
        public void write(CsvEncoder w) throws IOException {
            w.appendNull();
        }
    }
}
