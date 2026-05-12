package sample.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bundle Activator for the sample implementation.
 * 
 * This activator demonstrates the bundle lifecycle callbacks
 * and is useful for debugging when bundles start and stop.
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("[sample.impl] Bundle started - " + context.getBundle().getSymbolicName());
        System.out.println("[sample.impl] Bundle ID: " + context.getBundle().getBundleId());
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("[sample.impl] Bundle stopped - " + context.getBundle().getSymbolicName());
    }
}
