package aQute.p2.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;

/**
 * Parser for Eclipse product units from P2 content.xml files. This class
 * parses Eclipse products and creates OSGi Resource representations with
 * capabilities and requirements in the osgi.identity namespace with
 * type=org.eclipse.equinox.p2.type.product.
 */
public class Product extends XMLBase {

	/**
	 * Represents a required capability in a product
	 */
	public static class Required {
		public String	namespace;
		public String	name;
		public String	range;
		public boolean	optional;
		public String	filter;
		public boolean	greedy	= true;

		@Override
		public String toString() {
			return namespace + ":" + name + ":" + range + (optional ? " (optional)" : "");
		}
	}

	// Product unit properties from content.xml
	public String					id;
	public String					version;

	// Properties map from content.xml
	private Map<String, String>		properties	= new HashMap<>();

	// Requirements list
	private List<Required>			requires	= new ArrayList<>();

	// Provides list (namespace, name, version)
	private List<Provided>			provides	= new ArrayList<>();

	// Touchpoint data
	private Map<String, String>		touchpointData	= new HashMap<>();

	public static class Provided {
		public String	namespace;
		public String	name;
		public String	version;

		@Override
		public String toString() {
			return namespace + ":" + name + ":" + version;
		}
	}

	public Product(Document document) {
		super(document);
	}

	public Product(Node unitNode) {
		super(nodeToDocument(unitNode));
	}

	private static Document nodeToDocument(Node node) {
		return node.getOwnerDocument();
	}

	/**
	 * Parse the unit element from content.xml and populate properties
	 */
	public void parse(Node unitNode) throws Exception {
		// Parse unit attributes
		id = getAttribute(unitNode, "id");
		version = getAttribute(unitNode, "version");

		// Parse properties
		properties = getProperties(unitNode, "properties/property");

		// Parse provides
		provides = getProvides(unitNode);

		// Parse requires
		requires = getRequires(unitNode);

		// Parse touchpoint data
		touchpointData = getTouchpointData(unitNode);
	}

	/**
	 * Get all properties from the unit
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Get all provided capabilities
	 */
	private List<Provided> getProvides(Node unitNode) throws Exception {
		List<Provided> result = new ArrayList<>();
		NodeList providesNodes = getNodes(unitNode, "provides/provided");
		for (int i = 0; i < providesNodes.getLength(); i++) {
			Node item = providesNodes.item(i);
			Provided provided = new Provided();
			provided.namespace = getAttribute(item, "namespace");
			provided.name = getAttribute(item, "name");
			provided.version = getAttribute(item, "version");
			result.add(provided);
		}
		return result;
	}

	/**
	 * Get all required capabilities
	 */
	private List<Required> getRequires(Node unitNode) throws Exception {
		List<Required> result = new ArrayList<>();
		NodeList requiresNodes = getNodes(unitNode, "requires/required");
		for (int i = 0; i < requiresNodes.getLength(); i++) {
			Node item = requiresNodes.item(i);
			Required req = new Required();
			req.namespace = getAttribute(item, "namespace");
			req.name = getAttribute(item, "name");
			req.range = getAttribute(item, "range");
			
			// Check for optional attribute
			String optional = getAttribute(item, "optional");
			req.optional = "true".equals(optional);
			
			// Check for greedy attribute (default is true)
			String greedy = getAttribute(item, "greedy");
			if (greedy != null) {
				req.greedy = !"false".equals(greedy);
			}
			
			// Get filter child element if present
			NodeList filterNodes = getNodes(item, "filter");
			if (filterNodes.getLength() > 0) {
				req.filter = filterNodes.item(0).getTextContent();
			}
			
			result.add(req);
		}
		return result;
	}

	/**
	 * Get touchpoint data
	 */
	private Map<String, String> getTouchpointData(Node unitNode) throws Exception {
		Map<String, String> result = new HashMap<>();
		NodeList instructionNodes = getNodes(unitNode, "touchpointData/instructions/instruction");
		for (int i = 0; i < instructionNodes.getLength(); i++) {
			Node item = instructionNodes.item(i);
			String key = getAttribute(item, "key");
			String value = item.getTextContent();
			if (key != null && value != null) {
				result.put(key, value);
			}
		}
		return result;
	}

	/**
	 * Create an OSGi Resource representation of this product with capabilities
	 * and requirements
	 */
	public Resource toResource() throws Exception {
		ResourceBuilder rb = new ResourceBuilder();

		// Create identity capability with
		// type=org.eclipse.equinox.p2.type.product
		CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);
		identity.addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, id);
		identity.addAttribute(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, "org.eclipse.equinox.p2.type.product");
		
		if (version != null) {
			try {
				Version v = Version.parseVersion(version);
				identity.addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, v);
			} catch (IllegalArgumentException e) {
				// If version parsing fails, store as string
				identity.addAttribute("version.string", version);
			}
		}

		// Add all product properties as attributes in the identity capability
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			
			// Handle special properties
			if (key.equals("org.eclipse.equinox.p2.name")) {
				identity.addAttribute("name", value);
			} else if (key.equals("org.eclipse.equinox.p2.description")) {
				identity.addAttribute("description", value);
			} else if (key.equals("org.eclipse.equinox.p2.provider")) {
				identity.addAttribute("provider", value);
			} else if (key.equals("org.eclipse.equinox.p2.type.group")) {
				identity.addAttribute("type.group", value);
			} else if (key.equals("org.eclipse.equinox.p2.type.product")) {
				identity.addAttribute("type.product", value);
			} else {
				// Add other properties with their full key
				identity.addAttribute(key, value);
			}
		}

		rb.addCapability(identity);

		// Create requirements for all required capabilities
		for (Required requirement : requires) {
			// Skip tooling requirements (they start with "tooling")
			if (requirement.name != null && requirement.name.startsWith("tooling")) {
				continue;
			}
			
			CapReqBuilder req = new CapReqBuilder(requirement.namespace);
			
			// Build filter based on namespace
			StringBuilder filterBuilder = new StringBuilder();
			
			if (requirement.namespace.equals("org.eclipse.equinox.p2.iu")) {
				// For p2 IU namespace, filter on the IU name
				filterBuilder.append("(");
				filterBuilder.append(requirement.namespace);
				filterBuilder.append("=");
				filterBuilder.append(requirement.name);
				filterBuilder.append(")");
				
				// Add version range if present
				if (requirement.range != null && !requirement.range.equals("0.0.0")) {
					// P2 uses ranges like [1.0.0,1.0.0] for exact versions
					filterBuilder.insert(0, "(&");
					filterBuilder.append("(version");
					filterBuilder.append(requirement.range);
					filterBuilder.append("))");
				}
			} else if (requirement.namespace.equals("osgi.ee")) {
				// For execution environment, use a simple filter
				filterBuilder.append("(");
				filterBuilder.append(requirement.namespace);
				filterBuilder.append("=");
				filterBuilder.append(requirement.name);
				filterBuilder.append(")");
			} else {
				// For other namespaces, use the name as filter
				filterBuilder.append("(");
				filterBuilder.append(requirement.namespace);
				filterBuilder.append("=");
				filterBuilder.append(requirement.name);
				filterBuilder.append(")");
			}
			
			// Add the filter from the requirement if present
			if (requirement.filter != null && !requirement.filter.isEmpty()) {
				String combinedFilter = "(&" + filterBuilder.toString() + requirement.filter + ")";
				req.addDirective("filter", combinedFilter);
			} else {
				req.addDirective("filter", filterBuilder.toString());
			}
			
			// Set resolution directive for optional requirements
			if (requirement.optional) {
				req.addDirective("resolution", "optional");
			}
			
			rb.addRequirement(req);
		}

		return rb.build();
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}

	public Map<String, String> getPropertiesMap() {
		return properties;
	}

	public List<Required> getRequires() {
		return requires;
	}

	public List<Provided> getProvides() {
		return provides;
	}

	@Override
	public String toString() {
		return "Product [id=" + id + ", version=" + version + ", properties=" + properties.size() + ", requires="
			+ requires.size() + ", provides=" + provides.size() + "]";
	}
}
