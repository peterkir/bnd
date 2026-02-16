package aQute.bnd.service;

import java.util.List;

/**
 * An interface for repository plugins that can provide Eclipse P2 products.
 * This allows repositories to expose products alongside bundles and features.
 * The actual product objects are implementation-specific (e.g.,
 * aQute.p2.provider.Product for P2 repositories).
 */
public interface ProductProvider {

	/**
	 * Get all products available in this repository.
	 *
	 * @return a list of products, or empty list if none available
	 * @throws Exception if an error occurs while fetching products
	 */
	List<?> getProducts() throws Exception;

	/**
	 * Get a specific product by ID and version.
	 *
	 * @param id the product ID
	 * @param version the product version
	 * @return the product, or null if not found
	 * @throws Exception if an error occurs while fetching the product
	 */
	Object getProduct(String id, String version) throws Exception;
}
