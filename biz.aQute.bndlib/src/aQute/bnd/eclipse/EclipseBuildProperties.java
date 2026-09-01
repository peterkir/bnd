package aQute.bnd.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.lib.strings.Strings;

/**
 * Models the Eclipse {@code build.properties} file, which describes how a PDE
 * project is compiled and packaged.
 */
public class EclipseBuildProperties {

	final List<Library>	libraries	= new ArrayList<>();
	private Processor	properties;

	class Library {

		EclipseManifest	manifest;
		List<String>	includes	= new ArrayList<>();
		List<String>	excludes	= new ArrayList<>();
		List<String>	sources		= new ArrayList<>();
		List<String>	extra		= new ArrayList<>();
		String			output;

		Library(String libName, Processor props) throws IOException {
			String outputProperty = props.getProperty("output." + libName, "bin");
			output = ensureDir(outputProperty);

			if (props.getProperty("source." + libName) != null)
				for (String s : Strings.split(props.get("source." + libName)))
					this.sources.add(ensureDir(s));

			if (props.getProperty("bin.includes") != null)
				this.includes.addAll(Strings.split(props.get("bin.includes")));

			if (props.getProperty("bin.excludes") != null)
				this.excludes.addAll(Strings.split(props.get("bin.excludes")));

			if (props.getProperty("exclude." + libName) != null)
				this.excludes.addAll(Strings.split(props.get("exclude." + libName)));

			String manifestPath = props.getProperty("manifest." + libName, "META-INF/MANIFEST.MF");
			File file = props.getFile(manifestPath);
			if (!file.isFile()) {
				props.error("Cannot find manifest file %s for lib %s", file, libName);
			}
			this.manifest = new EclipseManifest(props, manifestPath);
		}

		private String ensureDir(String property) {
			return property.endsWith("/") ? property : property + "/";
		}

		public EclipseManifest getManifest() {
			return manifest;
		}

		public void move(Jar content, BndConversionPaths srcMainJava, BndConversionPaths srcMainResources,
			BndConversionPaths srcTestJava, BndConversionPaths srcTestResources) {

			for (String sourceDir : sources) {
				if (srcTestJava.has(sourceDir))
					srcTestJava.move(content, sourceDir);
				else if (srcMainJava.has(sourceDir)) {
					srcMainJava.move(content, sourceDir);
				} else {
					srcMainJava.addDirectory(sourceDir);
					srcMainJava.move(content, sourceDir);
				}
			}

			for (String resource : includes) {
				if (resource.equals("."))
					continue;

				String to = srcMainResources.directories.get(0) + resource;
				if (content.move(resource, to) == 0) {
					properties.warning("No resources were found %s", resource);
				}
			}
		}

		public void removeOutputs(Jar content) {
			content.removePrefix(output);
			content.remove("build.properties");
		}
	}

	public EclipseBuildProperties(Processor props) throws IOException {
		this.properties = props;
		List<String> jarsCompileOrder = Strings.split(props.getProperty("jars.compile.order", "."));
		if (jarsCompileOrder.isEmpty()) {
			props.error("the jars.compile.order=%s is empty", jarsCompileOrder);
			return;
		}

		for (String libName : jarsCompileOrder) {
			if (isDefault(libName) && jarsCompileOrder.size() > 1) {
				props.error(
					"the jars.compile.order=%s, when multiple libraries are built they should all be named and not be named '.'",
					jarsCompileOrder);
			}
			libraries.add(new Library(libName, props));
		}

		if (libraries.size() != 1)
			throw new UnsupportedOperationException("Only projects consisting of one plugin can be imported");
	}

	private boolean isDefault(String libName) {
		return ".".equals(libName);
	}

	public List<Library> getLibraries() {
		return libraries;
	}
}
