package tools.jackson.dataformat.javaprop;

import org.junit.jupiter.api.Test;

import tools.jackson.dataformat.javaprop.io.JPropWriteContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify that {@link JPropWriteContext} properly resets
 * internal state when child contexts are recycled.
 */
public class WriteContextResetTest
{
    // Verify that recycled child contexts reset _gotName properly,
    // so that writeName() works on the first call after recycling.
    @Test
    public void testGotNameResetOnContextRecycling()
    {
        JPropWriteContext root = JPropWriteContext.createRootContext(0);

        // Create a child object context and write a name (sets _gotName = true)
        JPropWriteContext child1 = root.createChildObjectContext(null, 0);
        assertTrue(child1.writeName("key1"));
        // Leave _gotName = true by not calling writeValue()

        // Recycle the child as a new object context
        JPropWriteContext child2 = root.createChildObjectContext(null, 0);
        // writeName() must succeed — _gotName should have been reset to false
        assertTrue(child2.writeName("key2"),
                "_gotName was not reset when context was recycled");
    }
}
