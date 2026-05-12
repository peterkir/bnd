package sample.api;

/**
 * Sample Service interface.
 * 
 * This is a simple OSGi service interface that demonstrates
 * how to define services in an API bundle.
 */
public interface SampleService {
    
    /**
     * Returns a greeting message.
     * 
     * @param name the name to greet
     * @return a greeting message
     */
    String greet(String name);
    
    /**
     * Returns the service name.
     * 
     * @return the name of this service
     */
    String getName();
    
    /**
     * Performs a calculation.
     * 
     * @param a first number
     * @param b second number
     * @return the sum of a and b
     */
    int calculate(int a, int b);
}
