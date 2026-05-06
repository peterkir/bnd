package aQute.bnd.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;

/**
 * Parses an Eclipse PDE {@code META-INF/MANIFEST.MF} and generates the
 * corresponding {@code bnd.bnd} file content.
 */
public class EclipseManifest {
	public static final String	HEADER_FORMAT	= "%-40s: %s\n";

	/**
	 * Headers that are removed (and not placed) in the generated bnd file
	 * because bnd derives them automatically.
	 */
	public static String[]		REMOVE_HEADERS	= {
		"Built-By", "Created-By", "Bundle-RequiredExecutionEnvironment", "Build-Jdk", "Bundle-ManifestVersion",
		"ManifestVersion", "Archiver-Version", Constants.BUNDLE_CLASSPATH, Constants.EXPORT_PACKAGE,
		"Manifest-Version", Constants.BUNDLE_SYMBOLICNAME, Constants.SERVICE_COMPONENT, Constants.IMPORT_PACKAGE,
		"Originally-Created-By", Constants.PRIVATE_PACKAGE, Constants.IGNORE_PACKAGE, Constants.REQUIRE_BUNDLE,
		// version is set via packageinfo / Package-Info.java
		Constants.BUNDLE_VERSION
	};

	/**
	 * Headers that should be commented out in the generated bnd file.
	 */
	public static String[]		COMMENT_HEADERS	= {
		"Built-By", "Created-By", "Bundle-RequiredExecutionEnvironment", "Build-Jdk", "Bundle-ManifestVersion",
		"ManifestVersion", "Archiver-Version", Constants.BUNDLE_CLASSPATH, Constants.EXPORT_PACKAGE,
		"Manifest-Version", Constants.BUNDLE_SYMBOLICNAME, Constants.SERVICE_COMPONENT, Constants.IMPORT_PACKAGE,
		"Originally-Created-By", Constants.PRIVATE_PACKAGE, Constants.IGNORE_PACKAGE
	};

	/** Headers that take OSGi parameter syntax. */
	static String[]				PARAMETER_HEADERS	= {
		Constants.BUNDLE_ACTIVATIONPOLICY, Constants.BUNDLE_ACTIVATOR, Constants.BUNDLE_CATEGORY,
		Constants.BUNDLE_DEVELOPERS, Constants.BUNDLE_LICENSE, Constants.BUNDLE_LOCALIZATION,
		Constants.BUNDLE_NATIVECODE, Constants.EXPORT_SERVICE, Constants.FRAGMENT_HOST, Constants.IMPORT_SERVICE,
		Constants.PROVIDE_CAPABILITY, Constants.EXPORT_CONTENTS, Constants.EXPORT_PACKAGE
	};

	private final Processor		properties;
	private Domain				manifest;
	private String				bsn;

	EclipseManifest(Processor properties, String manifestPath) throws IOException {
		this.properties = properties;
		File file = properties.getFile(manifestPath);
		if (!file.isFile())
			this.properties.error("Manifest not found %s", file);

		this.manifest = Domain.domain(file);

		Map.Entry<String, Attrs> bsnEntry = this.manifest.getBundleSymbolicName();
		bsn = bsnEntry != null ? bsnEntry.getKey() : null;
		if (bsn == null)
			bsn = properties.getBase().getName();
	}

	/**
	 * Generate the bnd.bnd file content from the PDE manifest.
	 *
	 * @param sourcePackages packages found in the main source directories
	 * @param mainResources resource path mapper
	 * @param workingset optional Eclipse working set name
	 * @return the bnd.bnd file content as a string
	 */
	public String toBndFile(Set<String> sourcePackages, BndConversionPaths mainResources, String workingset)
		throws IOException {
		try (Formatter model = new Formatter()) {

			Parameters bcpin = manifest.getBundleClasspath();
			if (bcpin != null && !bcpin.isEmpty()) {
				boolean hasOnlyDefault = bcpin.size() == 1 && bcpin.keySet()
					.iterator()
					.next()
					.equals(".");
				if (!hasOnlyDefault) {
					model.format(HEADER_FORMAT, Constants.BUNDLE_CLASSPATH, format(bcpin));
				}
			}

			Set<String> headers = new HashSet<>();
			for (String key : manifest)
				headers.add(key);

			for (String header : REMOVE_HEADERS) {
				headers.remove(header);
			}

			for (String name : PARAMETER_HEADERS) {
				headers.remove(name);
				String value = manifest.get(name);
				if (value != null) {
					if (!value.equals(properties.getProperty(name))) {
						Parameters parameters = new Parameters(value);
						model.format(HEADER_FORMAT, name, format(parameters));
					}
				}
			}

			Parameters requireBundle = manifest.getRequireBundle();
			if (requireBundle != null && !requireBundle.isEmpty()) {
				model.format(HEADER_FORMAT, "# Require-Bundle", requireBundle);
				headers.remove(Constants.REQUIRE_BUNDLE);
			}

			Parameters exports = manifest.getExportContents();
			exports.putAll(manifest.getExportPackage());
			Parameters privates = new Parameters(String.join(",", sourcePackages));
			privates.keySet()
				.removeAll(exports.keySet());
			privates.keySet()
				.removeIf(pname -> !Verifier.PACKAGEPATTERN.matcher(pname)
					.matches());

			if (!privates.isEmpty())
				model.format(HEADER_FORMAT, Constants.PRIVATE_PACKAGE, format(privates));

			for (String header : headers) {
				String value = manifest.get(header)
					.trim();
				if (!value.equals(properties.getProperty(header))) {
					model.format(HEADER_FORMAT, header, value);
				}
			}

			if (workingset != null) {
				model.format(HEADER_FORMAT, "-workingset", workingset);
			}

			return model.toString();
		}
	}

	static String format(Parameters parameters) throws IOException {
		if (parameters.isEmpty())
			return "";

		if (parameters.size() == 1) {
			return parameters.toString();
		}

		StringBuilder sb = new StringBuilder();
		String del = "\\\n    ";
		for (Map.Entry<String, Attrs> e : parameters.entrySet()) {
			sb.append(del)
				.append(e.getKey());
			Attrs value = e.getValue();
			for (Entry<String, String> a : value.entrySet()) {
				sb.append("; \\\n        ");
				sb.append(a.getKey()
					.trim());
				Type type = value.getType(a.getKey());
				if (type != null && !type.equals(Type.STRING)) {
					sb.append(":")
						.append(type);
				}
				sb.append("=");
				OSGiHeader.quote(sb, a.getValue());
			}
			del = ", \\\n    ";
		}
		sb.append("\n");
		return sb.toString();
	}

	public String getBsn() {
		return bsn;
	}
}
