package tools.jackson.dataformat.yaml.impl;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * VarHandle-based array access with fallback to direct array indexing.
 * VarHandles can enable the JIT to skip bounds checks on hot loops.
 *
 * @since 3.3
 */
public final class BufferAccess {
    private static final VarHandle BYTE_ARRAY;
    private static final VarHandle CHAR_ARRAY;

    static {
        VarHandle bh, ch;
        try {
            bh = MethodHandles.arrayElementVarHandle(byte[].class);
            ch = MethodHandles.arrayElementVarHandle(char[].class);
        } catch (Exception e) {
            bh = null;
            ch = null;
        }
        BYTE_ARRAY = bh;
        CHAR_ARRAY = ch;
    }

    public static byte getByte(byte[] array, int index) {
        if (BYTE_ARRAY != null) {
            return (byte) BYTE_ARRAY.get(array, index);
        }
        return array[index];
    }

    public static void setByte(byte[] array, int index, byte value) {
        if (BYTE_ARRAY != null) {
            BYTE_ARRAY.set(array, index, value);
        } else {
            array[index] = value;
        }
    }

    public static char getChar(char[] array, int index) {
        if (CHAR_ARRAY != null) {
            return (char) CHAR_ARRAY.get(array, index);
        }
        return array[index];
    }

    public static void setChar(char[] array, int index, char value) {
        if (CHAR_ARRAY != null) {
            CHAR_ARRAY.set(array, index, value);
        } else {
            array[index] = value;
        }
    }

    private BufferAccess() {}
}
