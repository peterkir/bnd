package bndtools.views.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import aQute.p2.provider.Product;

public class ResolutionViewProductRequirementTest {

	@Test
	public void testFeatureRequirementClassification() {
		Product.Required required = new Product.Required();
		required.namespace = "org.eclipse.equinox.p2.iu";
		required.name = "example.feature.group";

		assertTrue(ResolutionView.isProductFeatureRequirement(required));
		assertFalse(ResolutionView.isProductBundleRequirement(required));
	}

	@Test
	public void testBundleRequirementClassificationForOsgiBundle() {
		Product.Required required = new Product.Required();
		required.namespace = "osgi.bundle";
		required.name = "org.example.bundle";

		assertFalse(ResolutionView.isProductFeatureRequirement(required));
		assertTrue(ResolutionView.isProductBundleRequirement(required));
	}

	@Test
	public void testBundleRequirementClassificationForP2IUWithoutFeatureSuffix() {
		Product.Required required = new Product.Required();
		required.namespace = "org.eclipse.equinox.p2.iu";
		required.name = "org.example.bundle";

		assertFalse(ResolutionView.isProductFeatureRequirement(required));
		assertTrue(ResolutionView.isProductBundleRequirement(required));
	}

	@Test
	public void testProductRequirementsOnlyBundles() {
		List<Product.Required> requirements = List.of(required("osgi.bundle", "org.example.bundle.a"),
			required("org.eclipse.equinox.p2.iu", "org.example.bundle.b"));

		long featureCount = requirements.stream()
			.filter(ResolutionView::isProductFeatureRequirement)
			.count();
		long bundleCount = requirements.stream()
			.filter(ResolutionView::isProductBundleRequirement)
			.count();

		assertEquals(0, featureCount);
		assertEquals(2, bundleCount);
	}

	@Test
	public void testProductRequirementsOnlyFeatures() {
		List<Product.Required> requirements = List.of(
			required("org.eclipse.equinox.p2.iu", "org.example.feature.a.feature.group"),
			required("org.eclipse.equinox.p2.iu", "org.example.feature.b.feature.group"));

		long featureCount = requirements.stream()
			.filter(ResolutionView::isProductFeatureRequirement)
			.count();
		long bundleCount = requirements.stream()
			.filter(ResolutionView::isProductBundleRequirement)
			.count();

		assertEquals(2, featureCount);
		assertEquals(0, bundleCount);
	}

	@Test
	public void testProductRequirementsBundlesAndFeatures() {
		List<Product.Required> requirements = List.of(
			required("org.eclipse.equinox.p2.iu", "org.example.feature.a.feature.group"),
			required("osgi.bundle", "org.example.bundle.a"),
			required("org.eclipse.equinox.p2.iu", "org.example.bundle.b"),
			required("org.eclipse.equinox.p2.iu", "org.example.feature.b.feature.group"));

		long featureCount = requirements.stream()
			.filter(ResolutionView::isProductFeatureRequirement)
			.count();
		long bundleCount = requirements.stream()
			.filter(ResolutionView::isProductBundleRequirement)
			.count();

		assertEquals(2, featureCount);
		assertEquals(2, bundleCount);
	}

	private static Product.Required required(String namespace, String name) {
		Product.Required required = new Product.Required();
		required.namespace = namespace;
		required.name = name;
		return required;
	}
}
