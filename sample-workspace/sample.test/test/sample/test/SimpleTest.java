package sample.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Simple JUnit tests (non-OSGi).
 * 
 * These tests can be run without an OSGi framework:
 *   bnd junit -p sample.test
 */
public class SimpleTest {

    @Test
    public void testSimple() {
        assertTrue("Simple test should pass", true);
    }
    
    @Test
    public void testMath() {
        assertEquals("2 + 2 should equal 4", 4, 2 + 2);
    }
    
    @Test
    public void testString() {
        String hello = "Hello";
        String world = "World";
        assertEquals("String concatenation", "HelloWorld", hello + world);
    }
}
