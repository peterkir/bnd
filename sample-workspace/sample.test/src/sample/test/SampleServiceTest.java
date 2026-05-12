package sample.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
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
    private ServiceReference<SampleService> serviceRef;
    private SampleService service;

    /**
     * Injection point for the BundleContext.
     * Called by the OSGi test runner.
     */
    public void setBundleContext(BundleContext context) {
        this.context = context;
    }

    @Before
    public void setUp() {
        assertNotNull("BundleContext should be injected", context);
        serviceRef = context.getServiceReference(SampleService.class);
        assertNotNull("SampleService should be registered", serviceRef);
        service = context.getService(serviceRef);
        assertNotNull("SampleService should be available", service);
    }

    @After
    public void tearDown() {
        if (serviceRef != null && context != null) {
            context.ungetService(serviceRef);
        }
    }

    @Test
    public void testServiceAvailable() {
        // Service availability is verified in setUp()
        assertNotNull("SampleService should be available", service);
    }

    @Test
    public void testGreet() {
        String greeting = service.greet("World");
        assertEquals("Hello, World!", greeting);
    }

    @Test
    public void testGetName() {
        String name = service.getName();
        assertNotNull("Name should not be null", name);
        assertEquals("DefaultSampleService", name);
    }

    @Test
    public void testCalculate() {
        int result = service.calculate(2, 3);
        assertEquals(5, result);
    }
}
