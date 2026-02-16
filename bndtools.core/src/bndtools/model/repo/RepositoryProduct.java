package bndtools.model.repo;

import java.util.Map;

import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.p2.provider.Product;

/**
 * Represents an Eclipse P2 Product in the repository view. This is a synthetic
 * entry that wraps a {@link Product} and provides hierarchical display with
 * included features and included bundles as children.
 */
public class RepositoryProduct extends RepositoryEntry implements Actionable {

	private final Product product;

	public RepositoryProduct(final RepositoryPlugin repo, final Product product) {
		super(repo, product.getId(), new VersionFinder(product.getVersion(), Strategy.EXACT) {
			@Override
			Version findVersion() throws Exception {
				if (product.getVersion() != null) {
					try {
						return Version.parseVersion(product.getVersion());
					} catch (IllegalArgumentException e) {
						return null;
					}
				}
				return null;
			}
		});
		this.product = product;
	}

	public Product getProduct() {
		return product;
	}

	@Override
	public String toString() {
		return "RepositoryProduct [repo=" + getRepo() + ", id=" + product.getId() + ", version="
			+ product.getVersion() + "]";
	}

	@Override
	public String title(Object... target) throws Exception {
		try {
			if (getRepo() instanceof Actionable) {
				String s = ((Actionable) getRepo()).title(product.getId());
				if (s != null)
					return s;
			}
		} catch (Exception e) {
			// just default
		}
		return product.getId();
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		if (getRepo() instanceof Actionable) {
			try {
				String s = ((Actionable) getRepo()).tooltip(product.getId());
				if (s != null)
					return s;
			} catch (Exception e) {
				// fall through to default
			}
		}
		// Build default tooltip from product metadata
		StringBuilder sb = new StringBuilder();
		sb.append(product.getId());
		if (product.getVersion() != null) {
			sb.append(" ")
				.append(product.getVersion());
		}
		Map<String, String> props = product.getPropertiesMap();
		if (props != null) {
			String provider = props.get("org.eclipse.equinox.p2.provider");
			if (provider != null) {
				sb.append("\nProvider: ")
					.append(provider);
			}
			String description = props.get("org.eclipse.equinox.p2.description");
			if (description != null) {
				sb.append("\n")
					.append(description);
			}
		}
		return sb.toString();
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		Map<String, Runnable> map = null;
		try {
			if (getRepo() instanceof Actionable) {
				map = ((Actionable) getRepo()).actions(product.getId());
			}
		} catch (Exception e) {
			// just default
		}
		return map;
	}

	public String getText() {
		try {
			return title();
		} catch (Exception e) {
			return product.getId();
		}
	}
}
