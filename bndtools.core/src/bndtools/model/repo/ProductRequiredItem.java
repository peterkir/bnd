package bndtools.model.repo;

import aQute.p2.provider.Product;

/**
 * Represents a required item (feature or bundle) in a product's folder view.
 * Wraps a {@link Product.Required} and displays its properties.
 */
public class ProductRequiredItem {

	private final ProductFolderNode	parent;
	private final Product.Required	required;
	private final boolean			isFeature;

	public ProductRequiredItem(ProductFolderNode parent, Product.Required required, boolean isFeature) {
		this.parent = parent;
		this.required = required;
		this.isFeature = isFeature;
	}

	public ProductFolderNode getParent() {
		return parent;
	}

	public Product.Required getRequired() {
		return required;
	}

	public boolean isFeature() {
		return isFeature;
	}

	public String getId() {
		return required.name;
	}

	public String getVersionRange() {
		return required.range;
	}

	public boolean isOptional() {
		return required.optional;
	}

	public String getFilter() {
		return required.filter;
	}

	public String getNamespace() {
		return required.namespace;
	}

	/**
	 * Get display text for this item
	 */
	public String getText() {
		StringBuilder sb = new StringBuilder();
		if (required.name != null) {
			sb.append(required.name);
		}
		if (required.range != null && !"0.0.0".equals(required.range)) {
			sb.append(" ")
				.append(required.range);
		}
		if (required.optional) {
			sb.append(" [optional]");
		}
		if (required.filter != null && !required.filter.isEmpty()) {
			// Extract platform info from filter if present
			String filter = required.filter;
			if (filter.contains("osgi.os=")) {
				sb.append(" (platform filtered)");
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "ProductRequiredItem [name=" + required.name + ", range=" + required.range + ", optional="
			+ required.optional + ", isFeature=" + isFeature + "]";
	}
}
