package aQute.p2.provider;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.p2.api.Artifact;
import aQute.p2.api.Classifier;

/**
 * Parser for P2 content.xml files. This class parses the content repository
 * metadata and extracts product units along with bundle and feature units.
 * 
 * @formatter:off
 * <pre>
 * <?xml version='1.0' encoding='UTF-8'?>
 * <?metadataRepository version='1.2.0'?>
 * <repository name='Example' type='org.eclipse.equinox.internal.p2.metadata.repository.LocalMetadataRepository' version='1'>
 *   <properties size='2'>
 *     <property name='p2.timestamp' value='1638360000000'/>
 *     <property name='p2.compressed' value='false'/>
 *   </properties>
 *   <units size='3'>
 *     <unit id='org.eclipse.platform.ide' version='4.38.0.I20251201-0920'>
 *       <properties size='5'>
 *         <property name='org.eclipse.equinox.p2.name' value='Eclipse Platform'/>
 *         <property name='org.eclipse.equinox.p2.type.product' value='true'/>
 *         <property name='org.eclipse.equinox.p2.type.group' value='true'/>
 *         <property name='org.eclipse.equinox.p2.description' value='4.38 Release of the Eclipse Platform.'/>
 *         <property name='org.eclipse.equinox.p2.provider' value='Eclipse.org'/>
 *       </properties>
 *       <provides size='1'>
 *         <provided namespace='org.eclipse.equinox.p2.iu' name='org.eclipse.platform.ide' version='4.38.0.I20251201-0920'/>
 *       </provides>
 *       <requires size='8'>
 *         <required namespace='org.eclipse.equinox.p2.iu' name='org.eclipse.platform.feature.group' range='[4.38.0,4.38.0]'/>
 *       </requires>
 *       <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
 *       <touchpointData size='1'>
 *         <instructions size='1'>
 *           <instruction key='configure'>
 *             mkdir(path:${installFolder}/dropins);
 *           </instruction>
 *         </instructions>
 *       </touchpointData>
 *     </unit>
 *   </units>
 * </repository>
 * </pre>
 * @formatter:on
 */
class ContentRepository extends XMLBase {

	private URI					base;
	private List<Product>		products	= new ArrayList<>();
	private List<Artifact>		artifacts	= new ArrayList<>();

	ContentRepository(InputStream in, URI base) throws Exception {
		super(getDocument(in));
		this.base = base;
		parse();
	}

	void parse() throws Exception {
		// Get all unit nodes
		NodeList unitNodes = getNodes("repository/units/unit");
		
		for (int i = 0; i < unitNodes.getLength(); i++) {
			Node unitNode = unitNodes.item(i);
			
			// Check if this is a product unit by looking at properties
			boolean isProduct = isProductUnit(unitNode);
			
			if (isProduct) {
				// Parse as product
				Product product = new Product(unitNode);
				product.parse(unitNode);
				products.add(product);
				
				// Create an artifact entry for the product
				String id = getAttribute(unitNode, "id");
				String version = getAttribute(unitNode, "version");
				
				Artifact artifact = new Artifact();
				artifact.classifier = Classifier.PRODUCT;
				artifact.id = id;
				artifact.version = org.osgi.framework.Version.parseVersion(version);
				// Products don't have URIs as they're metadata-only
				artifact.uri = null;
				artifact.md5 = null;
				artifact.download_size = 0;
				
				// Store product metadata in artifact properties for later use
				// This allows P2Indexer to reconstruct the full Product
				artifact.setProperties(product.getPropertiesMap());
				
				// Store product requirements as a serialized string
				// Format: namespace|name|range|optional;namespace|name|range|optional;...
				StringBuilder reqsBuilder = new StringBuilder();
				for (Product.Required req : product.getRequires()) {
					if (reqsBuilder.length() > 0) {
						reqsBuilder.append(";");
					}
					reqsBuilder.append(req.namespace != null ? req.namespace : "");
					reqsBuilder.append("|");
					reqsBuilder.append(req.name != null ? req.name : "");
					reqsBuilder.append("|");
					reqsBuilder.append(req.range != null ? req.range : "");
					reqsBuilder.append("|");
					reqsBuilder.append(req.optional ? "true" : "false");
					reqsBuilder.append("|");
					reqsBuilder.append(req.filter != null ? req.filter : "");
				}
				artifact.getProperties().put("_product.requires", reqsBuilder.toString());
				
				artifacts.add(artifact);
			}
		}
	}

	/**
	 * Check if a unit is a product by examining its properties
	 */
	private boolean isProductUnit(Node unitNode) throws Exception {
		// A product unit has org.eclipse.equinox.p2.type.product property set to
		// "true"
		NodeList propertyNodes = getNodes(unitNode, "properties/property");
		for (int i = 0; i < propertyNodes.getLength(); i++) {
			Node propNode = propertyNodes.item(i);
			String name = getAttribute(propNode, "name");
			String value = getAttribute(propNode, "value");
			
			if ("org.eclipse.equinox.p2.type.product".equals(name) && "true".equals(value)) {
				return true;
			}
		}
		return false;
	}

	public List<Product> getProducts() {
		return products;
	}

	public List<Artifact> getArtifacts() {
		return artifacts;
	}
}
