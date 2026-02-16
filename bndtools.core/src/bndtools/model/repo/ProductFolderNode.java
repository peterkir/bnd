package bndtools.model.repo;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;

import aQute.p2.provider.Product;

/**
 * Virtual folder node for grouping product children (included features and
 * included bundles). This provides a hierarchical structure under each
 * RepositoryProduct.
 */
public class ProductFolderNode {

	private static final ILogger logger = Logger.getLogger(ProductFolderNode.class);

	public enum FolderType {
		INCLUDED_FEATURES("Included Features"),
		INCLUDED_BUNDLES("Included Bundles");

		private final String label;

		FolderType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	private final RepositoryProduct	parent;
	private final FolderType		type;
	private final List<Object>		children;

	public ProductFolderNode(RepositoryProduct parent, FolderType type) {
		this.parent = parent;
		this.type = type;
		this.children = new ArrayList<>();

		// Populate children based on type
		Product product = parent.getProduct();
		try {
			switch (type) {
				case INCLUDED_FEATURES :
					// Extract features from product requirements
					for (Product.Required req : product.getRequires()) {
						// Features are in org.eclipse.equinox.p2.iu namespace with type.group
						if ("org.eclipse.equinox.p2.iu".equals(req.namespace)) {
							// Check if this is a feature (ends with .feature.group)
							if (req.name != null && req.name.endsWith(".feature.group")) {
								children.add(new ProductRequiredItem(this, req, true));
							}
						}
					}
					break;
				case INCLUDED_BUNDLES :
					// Extract bundles from product requirements
					for (Product.Required req : product.getRequires()) {
						// Bundles are in osgi.bundle namespace or p2 IU namespace without .feature.group suffix
						if ("osgi.bundle".equals(req.namespace) ||
							("org.eclipse.equinox.p2.iu".equals(req.namespace) && req.name != null
								&& !req.name.endsWith(".feature.group"))) {
							children.add(new ProductRequiredItem(this, req, false));
						}
					}
					break;
			}
		} catch (Exception e) {
			// Log parsing errors for debugging
			logger.logError("Failed to parse product " + product.getId() + " for folder type " + type, e);
		}
	}

	public RepositoryProduct getParent() {
		return parent;
	}

	public FolderType getType() {
		return type;
	}

	public String getLabel() {
		return type.getLabel();
	}

	public List<Object> getChildren() {
		return children;
	}

	public boolean hasChildren() {
		return !children.isEmpty();
	}

	@Override
	public String toString() {
		return "ProductFolderNode [type=" + type + ", children=" + children.size() + "]";
	}
}
