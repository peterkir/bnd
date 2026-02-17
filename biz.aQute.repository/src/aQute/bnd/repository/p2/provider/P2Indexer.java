package aQute.bnd.repository.p2.provider;

import static aQute.bnd.osgi.repository.ResourcesRepository.toResourcesRepository;
import static aQute.bnd.osgi.resource.ResourceUtils.toVersion;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.repository.BridgeRepository;
import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceGenerator;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.service.RepositoryPlugin.DownloadListener;
import aQute.bnd.service.resource.SupportingResource;
import aQute.bnd.service.url.State;
import aQute.bnd.service.url.TaggedData;
import aQute.bnd.util.repository.DownloadListenerPromise;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.libg.cryptography.MD5;
import aQute.p2.api.Artifact;
import aQute.p2.api.ArtifactProvider;
import aQute.p2.packed.Unpack200;
import aQute.p2.provider.Feature;
import aQute.p2.provider.P2Impl;
import aQute.p2.provider.Product;
import aQute.p2.provider.TargetImpl;
import aQute.service.reporter.Reporter;

/**
 * This class maintains an OBR index but gets its sources from a P2 or
 * TargetPlatform.
 */
class P2Indexer implements Closeable {
	private final static Logger				logger		= LoggerFactory.getLogger(P2Indexer.class);
	private static final long				MAX_STALE	= TimeUnit.DAYS.toMillis(100);
	private final Reporter					reporter;
	private final Unpack200					processor;
	final File								location;
	final URI								url;
	final String							name;
	final String							urlHash;
	final File								indexFile;
	private final HttpClient				client;
	private final PromiseFactory			promiseFactory;
	private volatile BridgeRepository		bridge;
	private static final SupportingResource	RECOVERY	= new ResourceBuilder().build();

	P2Indexer(Unpack200 processor, Reporter reporter, File location, HttpClient client, URI url, String name)
		throws Exception {
		this.processor = processor;
		this.reporter = reporter;
		this.location = location;
		this.indexFile = new File(location, "index.xml.gz");
		this.client = client;
		this.promiseFactory = client.promiseFactory();
		this.url = url;
		this.name = name;
		this.urlHash = client.toName(url);
		IO.mkdirs(this.location);

		validate();
		init();
	}

	private void init() throws Exception {
		bridge = new BridgeRepository(readRepository(indexFile));
	}

	private void validate() {
		if (!this.location.isDirectory())
			throw new IllegalArgumentException("%s cannot be made a directory" + this.location);
	}

	File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		Resource resource = getBridge().get(bsn, version);
		if (resource == null)
			return null;

		ContentCapability contentCapability = ResourceUtils.getContentCapability(resource);

		if (contentCapability == null)
			return null;

		URI url = contentCapability.url();

		final File source = client.getCacheFileFor(url);
		final File link = new File(location, bsn + "-" + version + ".jar");

		IO.createSymbolicLinkOrCopy(link, source);

		Promise<File> go = client.build()
			.useCache(MAX_STALE)
			.asTag()
			.async(url.toURL())
			.map(tag -> processor.unpackAndLinkIfNeeded(tag, link));

		if (listeners.length == 0)
			return go.getValue();

		new DownloadListenerPromise(reporter, name + ": get " + bsn + ";" + version + " " + url, go, listeners);
		return link;
	}

	List<String> list(String pattern) throws Exception {
		return getBridge().list(pattern);
	}

	SortedSet<Version> versions(String bsn) throws Exception {
		return getBridge().versions(bsn);
	}

	private ResourcesRepository readRepository(File index) throws Exception {
		if (index.isFile()) {
			try (XMLResourceParser xp = new XMLResourceParser(index.toURI())) {
				List<Resource> resources = xp.parse();
				if (urlHash.equals(xp.name())) {
					return new ResourcesRepository(resources);
				}
			}
		}
		return save(readRepository());
	}

	private ResourcesRepository readRepository() throws Exception {
		ArtifactProvider p2;
		if (isTargetPlatform(this.url))
			p2 = new TargetImpl(processor, client, this.url, promiseFactory);
		else
			p2 = new P2Impl(processor, client, this.url, promiseFactory);

		List<Artifact> artifacts = p2.getAllArtifacts();
		Set<ArtifactID> visitedArtifacts = new HashSet<>(artifacts.size());
		Set<URI> visitedURIs = new HashSet<>(artifacts.size());

		Promise<List<Resource>> all = artifacts.stream()
			.map(a -> {
				// Products don't have URIs, they're metadata-only
				if (a.classifier == aQute.p2.api.Classifier.PRODUCT) {
					// Process product directly without fetching
					try {
						return promiseFactory.resolved(processArtifact(a, null));
					} catch (Exception e) {
						logger.info("{}: Failed to create resource for product {}", name, a.id, e);
						return promiseFactory.resolved(RECOVERY);
					}
				}
				
				if (!visitedURIs.add(a.uri))
					return null;
				if (a.md5 != null) {
					ArtifactID id = new ArtifactID(a.id, toVersion(a.version), a.md5);
					if (!visitedArtifacts.add(id))
						return null;
				}
				Promise<SupportingResource> fetched = fetch(a, 2, 1000L)
					.map(tag -> processor.unpackAndLinkIfNeeded(tag, null))
					.map(file -> processArtifact(a, file))
					.recover(failed -> {
						logger.info("{}: Failed to create resource for {}", name, a.uri, failed.getFailure());
						return RECOVERY;
					});
				return fetched;
			})
			.map(a -> a)
			.filter(Objects::nonNull)
			.collect(promiseFactory.toPromise());

		return all.map(resources -> resources.stream()
			.filter(resource -> resource != RECOVERY)
			.collect(toResourcesRepository()))
			.getValue();
	}

	static boolean isTargetPlatform(URI repositoryUri) {
		String path = repositoryUri.getPath();
		if (path == null) {
			path = repositoryUri.getSchemeSpecificPart();
		}
		if (path == null) {
			return false;
		}
		int queryIndex = path.indexOf('?');
		if (queryIndex >= 0) {
			path = path.substring(0, queryIndex);
		}
		int fragmentIndex = path.indexOf('#');
		if (fragmentIndex >= 0) {
			path = path.substring(0, fragmentIndex);
		}
		return path.endsWith(".target");
	}

	private Promise<TaggedData> fetch(Artifact a, int retries, long delay) {
		return client.build()
			.useCache(MAX_STALE)
			.asTag()
			.async(a.uri)
			.then(success -> success.thenAccept(tag -> checkDownload(a, tag))
				.recoverWith(failed -> {
					if (retries < 1) {
						return null; // no recovery
					}
					logger.info("Retrying invalid download: {}. delay={}, retries={}", failed.getFailure()
						.getMessage(), delay, retries);
					@SuppressWarnings("unchecked")
					Promise<TaggedData> delayed = (Promise<TaggedData>) failed.delay(delay);
					return delayed
						.recoverWith(f -> fetch(a, retries - 1, Math.min(delay * 2L, TimeUnit.MINUTES.toMillis(10))));
				}));
	}

	private void checkDownload(Artifact a, TaggedData tag) throws Exception {
		if (tag.getState() != State.UPDATED) {
			return;
		}
		File file = tag.getFile();
		String remoteDigest = a.md5;
		if (remoteDigest != null) {
			String fileDigest = MD5.digest(file)
				.asHex();
			int start = 0;
			while (start < remoteDigest.length() && Character.isWhitespace(remoteDigest.charAt(start))) {
				start++;
			}
			for (int i = 0; i < fileDigest.length(); i++) {
				if (start + i < remoteDigest.length()) {
					char us = fileDigest.charAt(i);
					char them = remoteDigest.charAt(start + i);
					if (us == them || Character.toLowerCase(us) == Character.toLowerCase(them)) {
						continue;
					}
				}
				IO.delete(file);
				throw new IOException(
					String.format("Invalid content checksum %s for %s; expected %s", fileDigest, a.uri, remoteDigest));
			}
		} else if (a.download_size != -1L) {
			long download_size = file.length();
			if (download_size != a.download_size) {
				IO.delete(file);
				throw new IOException(String.format("Invalid content size %s for %s; expected %s", download_size, a.uri,
					a.download_size));
			}
		}
	}

	/**
	 * Process an artifact (bundle, feature, or product) and convert it to an
	 * OSGi Resource
	 */
	private SupportingResource processArtifact(Artifact artifact, File file) throws Exception {
		ResourceBuilder rb = new ResourceBuilder();

		if (artifact.classifier == aQute.p2.api.Classifier.FEATURE) {
			// Process feature: parse it and add capabilities/requirements
			try (java.io.InputStream in = IO.stream(file)) {
				aQute.p2.provider.Feature feature = new aQute.p2.provider.Feature(in);
				feature.parse();
				Resource featureResource = feature.toResource();

				// Copy all capabilities and requirements from the feature resource
				for (Capability cap : featureResource.getCapabilities(null)) {
					aQute.bnd.osgi.resource.CapReqBuilder cb = new aQute.bnd.osgi.resource.CapReqBuilder(
						cap.getNamespace());
					cap.getAttributes()
						.forEach(cb::addAttribute);
					cap.getDirectives()
						.forEach(cb::addDirective);
					rb.addCapability(cb);
				}

				for (Requirement req : featureResource.getRequirements(null)) {
					aQute.bnd.osgi.resource.CapReqBuilder crb = new aQute.bnd.osgi.resource.CapReqBuilder(
						req.getNamespace());
					req.getAttributes()
						.forEach(crb::addAttribute);
					req.getDirectives()
						.forEach(crb::addDirective);
					rb.addRequirement(crb);
				}
			}
		} else if (artifact.classifier == aQute.p2.api.Classifier.PRODUCT) {
			// Products are metadata-only, reconstruct from artifact properties
			// Create identity capability
			aQute.bnd.osgi.resource.CapReqBuilder identity = new aQute.bnd.osgi.resource.CapReqBuilder(
				org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE);
			identity.addAttribute(org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE, artifact.id);
			identity.addAttribute(org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
				"org.eclipse.equinox.p2.type.product");
			identity.addAttribute(org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE,
				artifact.version);
			
			// Add product properties as attributes
			Map<String, String> artifactProps = artifact.getProperties();
			for (Map.Entry<String, String> entry : artifactProps.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				
				// Skip internal property
				if (key.startsWith("_product.")) {
					continue;
				}
				
				// Map P2 properties to simpler attribute names
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
					// Add other properties
					identity.addAttribute(key, value);
				}
			}
			
			rb.addCapability(identity);
			
			// Parse and add requirements
			String reqsString = artifactProps.get("_product.requires");
			if (reqsString != null && !reqsString.isEmpty()) {
				String[] reqs = reqsString.split(";");
				for (String reqStr : reqs) {
					String[] parts = reqStr.split("\\|", -1);
					if (parts.length >= 5) {
						String namespace = parts[0];
						String name = parts[1];
						String range = parts[2];
						boolean optional = "true".equals(parts[3]);
						String filter = parts[4];
						
						// Skip tooling requirements
						if (name.startsWith("tooling")) {
							continue;
						}
						
						// Create requirement
						aQute.bnd.osgi.resource.CapReqBuilder req = new aQute.bnd.osgi.resource.CapReqBuilder(namespace);
						req.addDirective("filter", Product.buildRequirementFilter(namespace, name, range, filter));
						
						if (optional) {
							req.addDirective("resolution", "optional");
						}
						
						rb.addRequirement(req);
					}
				}
			}
			
			// No file content for products
			return rb.build();
		}

		addP2IuCapability(rb, artifact);

		// Add content capability for the artifact (bundle or feature JAR)
		if (file != null) {
			rb.addFile(file, artifact.uri);
		}

		return rb.build();
	}

	private static void addP2IuCapability(ResourceBuilder rb, Artifact artifact) {
		aQute.bnd.osgi.resource.CapReqBuilder iu = new aQute.bnd.osgi.resource.CapReqBuilder(
			"org.eclipse.equinox.p2.iu");
		iu.addAttribute("org.eclipse.equinox.p2.iu", artifact.id);
		iu.addAttribute("version", artifact.version);
		rb.addCapability(iu);
	}

	private ResourcesRepository save(ResourcesRepository repository) throws IOException, Exception {
		XMLResourceGenerator xrg = new XMLResourceGenerator();
		xrg.repository(repository)
			.name(urlHash)
			.save(indexFile);
		return repository;
	}

	Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return getBridge().getRepository()
			.findProviders(requirements);
	}

	public void refresh() throws Exception {
		init();
	}

	@Override
	public void close() throws IOException {}

	BridgeRepository getBridge() {
		return bridge;
	}

	void reread() throws Exception {
		indexFile.delete();
		init();
	}

	/**
	 * Get all features available in this P2 repository.
	 * 
	 * @return a list of features, or empty list if none available
	 * @throws Exception if an error occurs while fetching features
	 */
	public List<Feature> getFeatures() throws Exception {
		List<Feature> features = new ArrayList<>();
		
		// Get all resources from the repository
		org.osgi.service.repository.Repository repository = getBridge().getRepository();
		
		// Create a wildcard requirement to find all identity capabilities
		aQute.bnd.osgi.resource.RequirementBuilder rb = new aQute.bnd.osgi.resource.RequirementBuilder(
			IdentityNamespace.IDENTITY_NAMESPACE);
		Requirement req = rb.buildSyntheticRequirement();
		
		// Find all providers
		Map<Requirement, Collection<Capability>> providers = repository.findProviders(
			java.util.Collections.singleton(req));
		Collection<Capability> allCaps = providers.get(req);
		
		if (allCaps == null || allCaps.isEmpty()) {
			return features;
		}
		
		// Get unique resources
		Set<Resource> allResources = aQute.bnd.osgi.resource.ResourceUtils.getResources(allCaps);
		
		// Filter resources with type=org.eclipse.update.feature in identity capability
		for (Resource resource : allResources) {
			List<Capability> identities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			for (Capability identity : identities) {
				Object type = identity.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
				if ("org.eclipse.update.feature".equals(type)) {
					// Extract the feature from the resource
					Feature feature = extractFeatureFromResource(resource);
					if (feature != null) {
						features.add(feature);
					}
					break;
				}
			}
		}
		
		return features;
	}

	/**
	 * Get a specific feature by ID and version.
	 * 
	 * @param id the feature ID
	 * @param version the feature version
	 * @return the feature, or null if not found
	 * @throws Exception if an error occurs while fetching the feature
	 */
	public Feature getFeature(String id, String version) throws Exception {
		// Get all resources from the repository
		org.osgi.service.repository.Repository repository = getBridge().getRepository();
		
		// Create a wildcard requirement to find all identity capabilities
		aQute.bnd.osgi.resource.RequirementBuilder rb = new aQute.bnd.osgi.resource.RequirementBuilder(
			IdentityNamespace.IDENTITY_NAMESPACE);
		Requirement req = rb.buildSyntheticRequirement();
		
		// Find all providers
		Map<Requirement, Collection<Capability>> providers = repository.findProviders(
			java.util.Collections.singleton(req));
		Collection<Capability> allCaps = providers.get(req);
		
		if (allCaps == null || allCaps.isEmpty()) {
			return null;
		}
		
		// Get unique resources
		Set<Resource> allResources = aQute.bnd.osgi.resource.ResourceUtils.getResources(allCaps);
		
		org.osgi.framework.Version requestedVersion = org.osgi.framework.Version.parseVersion(version);
		
		// Find the matching feature resource
		for (Resource resource : allResources) {
			List<Capability> identities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			for (Capability identity : identities) {
				Object type = identity.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
				Object idAttr = identity.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
				Object versionAttr = identity.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				
				if ("org.eclipse.update.feature".equals(type) && 
					id.equals(idAttr) && 
					requestedVersion.equals(versionAttr)) {
					return extractFeatureFromResource(resource);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Get all products available in this repository.
	 *
	 * @return a list of products, or empty list if none available
	 * @throws Exception if an error occurs while fetching products
	 */
	public List<Product> getProducts() throws Exception {
		List<Product> products = new ArrayList<>();
		
		// Get all resources from the repository
		org.osgi.service.repository.Repository repository = getBridge().getRepository();
		
		// Create a wildcard requirement to find all identity capabilities
		aQute.bnd.osgi.resource.RequirementBuilder rb = new aQute.bnd.osgi.resource.RequirementBuilder(
			IdentityNamespace.IDENTITY_NAMESPACE);
		Requirement req = rb.buildSyntheticRequirement();
		
		// Find all providers
		Map<Requirement, Collection<Capability>> providers = repository.findProviders(
			java.util.Collections.singleton(req));
		Collection<Capability> allCaps = providers.get(req);
		
		if (allCaps == null || allCaps.isEmpty()) {
			return products;
		}
		
		// Get unique resources
		Set<Resource> allResources = aQute.bnd.osgi.resource.ResourceUtils.getResources(allCaps);
		
		// Filter resources with type=org.eclipse.equinox.p2.type.product in identity capability
		for (Resource resource : allResources) {
			List<Capability> identities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			for (Capability identity : identities) {
				Object type = identity.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
				if ("org.eclipse.equinox.p2.type.product".equals(type)) {
					// Extract the product from the resource
					Product product = extractProductFromResource(resource);
					if (product != null) {
						products.add(product);
					}
					break;
				}
			}
		}
		
		return products;
	}

	/**
	 * Get a specific product by ID and version.
	 * 
	 * @param id the product ID
	 * @param version the product version
	 * @return the product, or null if not found
	 * @throws Exception if an error occurs while fetching the product
	 */
	public Product getProduct(String id, String version) throws Exception {
		// Get all resources from the repository
		org.osgi.service.repository.Repository repository = getBridge().getRepository();
		
		// Create a wildcard requirement to find all identity capabilities
		aQute.bnd.osgi.resource.RequirementBuilder rb = new aQute.bnd.osgi.resource.RequirementBuilder(
			IdentityNamespace.IDENTITY_NAMESPACE);
		Requirement req = rb.buildSyntheticRequirement();
		
		// Find all providers
		Map<Requirement, Collection<Capability>> providers = repository.findProviders(
			java.util.Collections.singleton(req));
		Collection<Capability> allCaps = providers.get(req);
		
		if (allCaps == null || allCaps.isEmpty()) {
			return null;
		}
		
		// Get unique resources
		Set<Resource> allResources = aQute.bnd.osgi.resource.ResourceUtils.getResources(allCaps);
		
		org.osgi.framework.Version requestedVersion = org.osgi.framework.Version.parseVersion(version);
		
		// Find the matching product resource
		for (Resource resource : allResources) {
			List<Capability> identities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			for (Capability identity : identities) {
				Object type = identity.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
				Object idAttr = identity.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
				Object versionAttr = identity.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
				
				if ("org.eclipse.equinox.p2.type.product".equals(type) && 
					id.equals(idAttr) && 
					requestedVersion.equals(versionAttr)) {
					return extractProductFromResource(resource);
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Extract a Product object from a Resource.
	 * 
	 * @param resource the resource representing the product
	 * @return the Product, or null if extraction fails
	 */
	private Product extractProductFromResource(Resource resource) {
		try {
			// Products in P2 are stored as capabilities in the resource itself
			// Extract the identity capability to get product information
			List<Capability> identities = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
			for (Capability identity : identities) {
				Object type = identity.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
				if ("org.eclipse.equinox.p2.type.product".equals(type)) {
					// Create a Product from the resource's capabilities
					return createProductFromResource(resource, identity);
				}
			}
			return null;
		} catch (Exception e) {
			logger.debug("Failed to extract product from resource: {}", e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Create a Product object from a resource's identity capability.
	 * 
	 * @param resource the resource
	 * @param identity the identity capability
	 * @return the Product
	 */
	private Product createProductFromResource(Resource resource, Capability identity) throws Exception {
		// Get product ID and version from identity capability
		String id = (String) identity.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
		Object versionObj = identity.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
		String version = versionObj != null ? versionObj.toString() : "0.0.0";
		
		// Create a minimal Product instance
		// Products don't have XML files like features, they're defined inline in content.xml
		Product product = new Product((org.w3c.dom.Document) null);
		product.id = id;
		product.version = version;
		
		// Extract requirements from the resource and convert to Product.Required
		// Use FilterParser to properly parse the requirement filters
		aQute.bnd.osgi.resource.FilterParser filterParser = new aQute.bnd.osgi.resource.FilterParser();
		List<org.osgi.resource.Requirement> resourceRequirements = resource.getRequirements(null);
		
		for (org.osgi.resource.Requirement req : resourceRequirements) {
			Product.Required productReq = new Product.Required();
			productReq.namespace = req.getNamespace();
			
			// Parse the requirement filter to extract name and version range
			try {
				aQute.bnd.osgi.resource.FilterParser.Expression expression = filterParser.parse(req);
				
				// Check for identity expression which gives us the name directly
				if (expression instanceof aQute.bnd.osgi.resource.FilterParser.IdentityExpression) {
					aQute.bnd.osgi.resource.FilterParser.IdentityExpression idExpr = 
						(aQute.bnd.osgi.resource.FilterParser.IdentityExpression) expression;
					productReq.name = idExpr.getSymbolicName();
					
					// Get version range if present
					aQute.bnd.osgi.resource.FilterParser.RangeExpression rangeExpr = idExpr.getRangeExpression();
					if (rangeExpr != null) {
						productReq.range = rangeExpr.getRangeString();
					}
				}
				// Check for bundle expression
				else if (expression instanceof aQute.bnd.osgi.resource.FilterParser.BundleExpression) {
					aQute.bnd.osgi.resource.FilterParser.BundleExpression bundleExpr = 
						(aQute.bnd.osgi.resource.FilterParser.BundleExpression) expression;
					productReq.name = bundleExpr.printExcludingRange();
					
					// Get version range if present
					aQute.bnd.osgi.resource.FilterParser.RangeExpression rangeExpr = bundleExpr.getRangeExpression();
					if (rangeExpr != null) {
						productReq.range = rangeExpr.getRangeString();
					}
				}
				// Check for package expression
				else if (expression instanceof aQute.bnd.osgi.resource.FilterParser.PackageExpression) {
					aQute.bnd.osgi.resource.FilterParser.PackageExpression pkgExpr = 
						(aQute.bnd.osgi.resource.FilterParser.PackageExpression) expression;
					productReq.name = pkgExpr.getPackageName();
					
					// Get version range if present
					aQute.bnd.osgi.resource.FilterParser.RangeExpression rangeExpr = pkgExpr.getRangeExpression();
					if (rangeExpr != null) {
						productReq.range = rangeExpr.getRangeString();
					}
				}
				// For simple expressions, extract key/value
				else if (expression instanceof aQute.bnd.osgi.resource.FilterParser.SimpleExpression) {
					aQute.bnd.osgi.resource.FilterParser.SimpleExpression simpleExpr = 
						(aQute.bnd.osgi.resource.FilterParser.SimpleExpression) expression;
					productReq.name = simpleExpr.getValue();
				}
			} catch (Exception e) {
				logger.debug("Failed to parse requirement filter: {}", e.getMessage(), e);
				// Fallback: Try to extract from raw filter string
				Map<String, String> directives = req.getDirectives();
				String filter = directives.get("filter");
				if (filter != null) {
					// Simple extraction for osgi.identity filters like "(osgi.identity=bundle.name)"
					int nameStart = filter.indexOf(req.getNamespace() + "=");
					if (nameStart == -1) {
						nameStart = filter.indexOf("osgi.identity=");
					}
					if (nameStart != -1) {
						int equals = filter.indexOf('=', nameStart);
						int closeParen = filter.indexOf(')', equals);
						if (equals != -1 && closeParen != -1) {
							productReq.name = filter.substring(equals + 1, closeParen).trim();
						}
					}
				}
			}
			
			// Check optional directive
			String resolution = req.getDirectives().get("resolution");
			productReq.optional = "optional".equals(resolution);
			
			// Only add if we successfully parsed a name
			if (productReq.name != null && !productReq.name.isEmpty()) {
				product.getRequires().add(productReq);
			}
		}
		
		return product;
	}
	
	/**
	 * Extract a Feature object from a Resource by downloading and parsing the
	 * feature JAR.
	 * 
	 * @param resource the resource representing the feature
	 * @return the parsed Feature, or null if extraction fails
	 */
	private Feature extractFeatureFromResource(Resource resource) {
		try {
			// Get the content capability to find the JAR location
			ContentCapability contentCapability = ResourceUtils.getContentCapability(resource);
			if (contentCapability == null) {
				logger.debug("Feature resource has no content capability, skipping");
				return null;
			}
			
			URI uri = contentCapability.url();
			logger.debug("Extracting feature from URI: {}", uri);
			
			// Download and get TaggedData
			TaggedData tag = client.build()
				.useCache(MAX_STALE)
				.get()
				.asTag()
				.go(uri);
			
			logger.debug("Downloaded feature JAR, state: {}, file: {}", tag.getState(), tag.getFile());
			
			// Try to unpack if it's a pack.gz or similar format
			File featureFile = processor.unpackAndLinkIfNeeded(tag, null);
			logger.debug("Unpacked feature file: {}", featureFile);
			
			// Parse the feature.xml from the JAR
			Feature feature = parseFeatureFromJar(featureFile);
			if (feature != null) {
				logger.debug("Successfully extracted feature: {} version {}", feature.getId(), feature.getVersion());
			} else {
				logger.debug("Failed to parse feature from file: {}", featureFile);
			}
			return feature;
			
		} catch (Exception e) {
			logger.debug("Failed to extract feature from resource: {}", e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Parse a Feature from a feature JAR file.
	 * 
	 * @param jarFile the feature JAR file (may be .jar, .zip, or .content from cache)
	 * @return the parsed Feature, or null if parsing fails
	 */
	private Feature parseFeatureFromJar(File jarFile) {
		// Check if file exists and is readable
		if (!jarFile.exists() || !jarFile.canRead()) {
			logger.debug("Feature file not accessible: {}", jarFile);
			return null;
		}
		
		try (InputStream in = IO.stream(jarFile)) {
			Feature feature = new Feature(in);
			feature.parse();
			return feature;
		} catch (Exception e) {
			logger.debug("Failed to parse feature from {}: {}", jarFile, e.getMessage());
			return null;
		}
	}
}
