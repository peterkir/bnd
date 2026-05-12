package sample.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import sample.api.SampleService;

/**
 * OSGi Integration Tests for SampleService.
 * 
 * These tests run inside an OSGi framework and can access
 * the BundleContext to interact with services.
 * 
 * Run with:
 *   bnd test -p sample.test
 *   bnd runtests sample.test
 */
public class SampleServiceTest {

    private volatile BundleContext context;

    /**
     * Injection point for the BundleContext.
     * Called by the OSGi test runner.
     */
    public void setBundleContext(BundleContext context) {
        this.context = context;
    }

    @Test
    public void testServiceAvailable() {
        assertNotNull("BundleContext should be injected", context);
        
        ServiceReference<SampleService> ref = context.getServiceReference(SampleService.class);
        assertNotNull("SampleService should be registered", ref);
        
        SampleService service = context.getService(ref);
        assertNotNull("SampleService should be available", service);
    }

    @Test
    public void testGreet() {
        ServiceReference<SampleService> ref = context.getServiceReference(SampleService.class);
        SampleService service = context.getService(ref);
        
        String greeting = service.greet("World");
        assertEquals("Hello, World!", greeting);
    }

    @Test
    public void testGetName() {
        ServiceReference<SampleService> ref = context.getServiceReference(SampleService.class);
        SampleService service = context.getService(ref);
        
        String name = service.getName();
        assertNotNull("Name should not be null", name);
        assertEquals("DefaultSampleService", name);
    }

    @Test
    public void testCalculate() {
        ServiceReference<SampleService> ref = context.getServiceReference(SampleService.class);
        SampleService service = context.getService(ref);
        
        int result = service.calculate(2, 3);
        assertEquals(5, result);
    }
}
