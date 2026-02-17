package aQute.p2.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import aQute.bnd.osgi.resource.FilterParser;
import aQute.lib.io.IO;
import aQute.p2.api.Artifact;
import aQute.p2.api.Classifier;

/**
 * Test Eclipse product parsing from P2 content.xml
 */
public class ProductTest {

	/**
	 * Test parsing of products from content.xml
	 */
	@Test
	public void testParseProductsFromContentXml() throws Exception {
		File contentFile = IO.getFile("testdata/p2/products/content.xml");
		assertThat(contentFile).isFile();
		
		try (InputStream in = IO.stream(contentFile)) {
			ContentRepository repo = new ContentRepository(in, contentFile.toURI());
			
			// Verify we found products
			List<Product> products = repo.getProducts();
			assertThat(products).hasSize(2);
			
			// Verify first product
			Product product1 = products.get(0);
			assertThat(product1.getId()).isEqualTo("example-rcp-app-ui-feature");
			assertThat(product1.getVersion()).isEqualTo("1.0.0.202512011200");
			
			// Verify product properties
			assertThat(product1.getPropertiesMap())
				.containsEntry("org.eclipse.equinox.p2.name", "Example RCP Feature Product")
				.containsEntry("org.eclipse.equinox.p2.type.product", "true")
				.containsEntry("org.eclipse.equinox.p2.type.group", "true")
				.containsEntry("org.eclipse.equinox.p2.description", "Example RCP application product based on feature")
				.containsEntry("org.eclipse.equinox.p2.provider", "Example Inc.");
			
			// Verify requirements (excluding tooling)
			List<Product.Required> requires1 = product1.getRequires();
			assertThat(requires1).hasSizeGreaterThanOrEqualTo(4);
			
			// Check for specific requirements
			assertThat(requires1).anyMatch(req -> 
				"org.eclipse.equinox.p2.iu".equals(req.namespace) && 
				"example.feature.group".equals(req.name));
			assertThat(requires1).anyMatch(req -> 
				"org.eclipse.equinox.p2.iu".equals(req.namespace) && 
				"org.eclipse.platform.feature.group".equals(req.name));
			assertThat(requires1).anyMatch(req -> 
				"osgi.ee".equals(req.namespace) && 
				"JavaSE".equals(req.name));
			
			// Verify second product
			Product product2 = products.get(1);
			assertThat(product2.getId()).isEqualTo("example-rcp-app-ui-plugin");
			assertThat(product2.getVersion()).isEqualTo("1.0.0.202512011200");
			
			assertThat(product2.getPropertiesMap())
				.containsEntry("org.eclipse.equinox.p2.name", "Example RCP Plugin Product");
		}
	}
	
	/**
	 * Test conversion of product to OSGi resource
	 */
	@Test
	public void testProductToResource() throws Exception {
		File contentFile = IO.getFile("testdata/p2/products/content.xml");
		assertThat(contentFile).isFile();
		
		try (InputStream in = IO.stream(contentFile)) {
			ContentRepository repo = new ContentRepository(in, contentFile.toURI());
			
			List<Product> products = repo.getProducts();
			assertThat(products).isNotEmpty();
			
			// Convert first product to resource
			Product product = products.get(0);
			org.osgi.resource.Resource resource = product.toResource();
			
			// Verify identity capability exists
			List<org.osgi.resource.Capability> identities = resource.getCapabilities(
				org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE);
			assertThat(identities).hasSize(1);
			
			org.osgi.resource.Capability identity = identities.get(0);
			assertThat(identity.getAttributes())
				.containsEntry(org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE, 
					"example-rcp-app-ui-feature")
				.containsEntry(org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, 
					"org.eclipse.equinox.p2.type.product")
				.containsKey(org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE)
				.containsKey("name")
				.containsKey("description")
				.containsKey("provider");
			
			// Verify requirements exist
			List<org.osgi.resource.Requirement> requirements = resource.getRequirements(null);
			assertThat(requirements).isNotEmpty();
			
			// Verify filter directives are present
			FilterParser parser = new FilterParser();
			for (org.osgi.resource.Requirement req : requirements) {
				String filter = req.getDirectives().get("filter");
				assertThat(filter).isNotBlank();
				assertThat(filter).doesNotContain("(version[");
				assertThatCode(() -> parser.parse(filter)).doesNotThrowAnyException();
			}
		}
	}
	
	/**
	 * Test that artifacts are created for products
	 */
	@Test
	public void testProductArtifacts() throws Exception {
		File contentFile = IO.getFile("testdata/p2/products/content.xml");
		assertThat(contentFile).isFile();
		
		try (InputStream in = IO.stream(contentFile)) {
			ContentRepository repo = new ContentRepository(in, contentFile.toURI());
			
			// Verify artifacts were created
			List<Artifact> artifacts = repo.getArtifacts();
			assertThat(artifacts).hasSize(2);
			
			// Check first artifact
			Artifact artifact1 = artifacts.get(0);
			assertThat(artifact1.classifier).isEqualTo(Classifier.PRODUCT);
			assertThat(artifact1.id).isEqualTo("example-rcp-app-ui-feature");
			assertThat(artifact1.version).isNotNull();
			assertThat(artifact1.uri).isNull(); // Products don't have URIs
			
			// Check product properties are stored
			assertThat(artifact1.getProperties())
				.containsEntry("org.eclipse.equinox.p2.name", "Example RCP Feature Product")
				.containsKey("_product.requires"); // Requirements should be serialized
			
			// Check second artifact
			Artifact artifact2 = artifacts.get(1);
			assertThat(artifact2.classifier).isEqualTo(Classifier.PRODUCT);
			assertThat(artifact2.id).isEqualTo("example-rcp-app-ui-plugin");
		}
	}
}
