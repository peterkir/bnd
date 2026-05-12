package sample.impl;

import org.osgi.service.component.annotations.Component;

import sample.api.SampleService;

/**
 * Default implementation of the SampleService.
 * 
 * This is a Declarative Services component that implements
 * the SampleService interface and is automatically registered
 * as an OSGi service when the bundle starts.
 */
@Component(
    name = "sample.default",
    service = SampleService.class,
    property = {
        "service.vendor=Sample Workspace",
        "service.ranking:Integer=100"
    }
)
public class DefaultSampleServiceImpl implements SampleService {

    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    @Override
    public String getName() {
        return "DefaultSampleService";
    }

    @Override
    public int calculate(int a, int b) {
        return a + b;
    }
}
