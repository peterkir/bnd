package aQute.bnd.eclipse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.component.TagResource;
import aQute.bnd.eclipse.EclipseBuildProperties.Library;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.plugin.git.GitPlugin;
import aQute.bnd.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

/**
 * Converts an Eclipse PDE project into a bnd workspace project.
 * <p>
 * The conversion reads the PDE {@code build.properties} file plus the bundle's
 * {@code META-INF/MANIFEST.MF} and produces a {@code bnd.bnd} file together
 * with a Maven-layout source tree ({@code src/main/java},
 * {@code src/main/resources}, etc.).
 */
public class LibPde extends Processor {
	private static final String				META_INF_MANIFEST_MF	= "META-INF/MANIFEST.MF";
	private static DocumentBuilderFactory	dbf						= DocumentBuilderFactory.newInstance();
	private static DocumentBuilder			db;

	static {
		try {
			db = dbf.newDocumentBuilder();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private static final String	TODO	= "\n# TODO ";

	final BndConversionPaths	mainSources;
	final BndConversionPaths	mainResources;
	final BndConversionPaths	testSources;
	final BndConversionPaths	testResources;
	final Workspace				workspace;
	String						workingset;
	boolean						clean;
	boolean						useManifest;

	/**
	 * Create a LibPde converter for the given PDE project directory.
	 *
	 * @param ws the bnd workspace to import into
	 * @param pdeProject the root directory of the Eclipse PDE project
	 */
	public LibPde(Workspace ws, File pdeProject) throws IOException {
		super(ws);
		File file = getFile(pdeProject, "build.properties");
		setProperties(file);
		ws.addBasicPlugin(new GitPlugin());

		this.workspace = ws;

		mainSources = new BndConversionPaths(ws, Constants.DEFAULT_PROP_SRC_DIR, "src/main/java", "src=src/main/java");
		mainResources = new BndConversionPaths(ws, Constants.DEFAULT_PROP_RESOURCES_DIR, "src/main/resources", null);

		//
		// Fixup: remove any source path that contains "resources" and move it
		// to the mainResources list. A bit of a hack but the convention is
		// common enough to be worth handling here.
		//
		for (String path : new HashSet<>(mainSources.directories)) {
			if (path.contains("resources")) {
				mainSources.directories.remove(path);
				if (!mainResources.directories.contains(path))
					mainResources.directories.add(path);
			}
		}

		testSources = new BndConversionPaths(ws, Constants.DEFAULT_PROP_TESTSRC_DIR, "src/test/java",
			"test=src/test/java");
		testResources = new BndConversionPaths(ws, Constants.DEFAULT_PROP_TESTRESOURCES_DIR, "src/test/resources",
			null);
		useManifest = isTrue(ws.getProperty("pde.useManifest", "true"));
	}

	/**
	 * Convert the PDE project content (already loaded into a {@link Jar}) into
	 * a bnd project layout, and return the bundle symbolic name.
	 *
	 * @param content the jar to reorganise in place
	 * @return the Bundle-SymbolicName
	 */
	public String convert(Jar content) throws Exception {

		EclipseBuildProperties buildProperties = new EclipseBuildProperties(this);

		Library lib = buildProperties.getLibraries()
			.iterator()
			.next();
		EclipseManifest manifest = lib.getManifest();

		lib.move(content, mainSources, mainResources, testSources, testResources);

		Set<String> sourcePackages = mainSources.getRelative(content.getResources()
			.keySet())
			.stream()
			.map(s -> {
				int n = s.lastIndexOf("/");
				if (n >= 0) {
					return s.substring(0, n)
						.replace('/', '.');
				} else
					return null;
			})
			.filter(s -> s != null)
			.distinct()
			.collect(Collectors.toSet());

		Set<String> remove = content.getResources()
			.keySet()
			.stream()
			.filter(s -> s.endsWith(".DS_store"))
			.collect(Collectors.toSet());
		remove.add("pom.xml");

		String bnd = manifest.toBndFile(sourcePackages, mainResources, workingset);

		if (useManifest) {
			//
			// Although we just created a bnd file based on the manifest we're
			// going to ignore most of it. We use the original manifest to ensure
			// we have identical manifests in our bundles. This is very useful if
			// you want to ensure that a move to bndtools generates exactly the
			// same bundles.
			//
			String path = mainResources.find(content, META_INF_MANIFEST_MF);
			if (path == null) {
				error("No manifest file found");
			} else {
				content.move(path, META_INF_MANIFEST_MF);
			}

			bnd = "# Remove next line and META-INF directory when you are going to modify this bundle\n"
				+ "# and fix any issues. This line mimics the PDE manifest first behavior\n"
				+ "# and voids many functions of bndtools\n" //
				+ "-manifest: " + META_INF_MANIFEST_MF + "\n\n" + bnd;
		}

		// We clearly do not want the manifest in our resources ever
		mainResources.remove(content, META_INF_MANIFEST_MF);

		content.putResource("bnd.bnd", new EmbeddedResource(bnd.getBytes(StandardCharsets.UTF_8), 0));

		lib.removeOutputs(content);

		content.getResources()
			.keySet()
			.removeAll(remove);

		return manifest.getBsn();
	}

	/**
	 * Write the converted bnd workspace project to disk.
	 *
	 * @return the newly created {@link Project}
	 */
	public Project write() throws Exception {

		try (Jar content = new Jar(getBase())) {
			content.setReporter(this);
			content.setDoNotTouchManifest();

			String bsn = convert(content);
			if (clean) {
				File projectDir = workspace.getFile(bsn);
				IO.delete(projectDir);
			}

			Project p = workspace.createProject(bsn);

			content.putResource(".classpath", new TagResource(
				EclipseLifecyclePlugin.toClasspathTag(p, toDoc(content.getResource(".classpath")))));
			content.putResource(".project", new TagResource(
				EclipseLifecyclePlugin.toProjectTag(p, toDoc(content.getResource(".project")))));

			content.writeFolder(p.getBase());
			p.forceRefresh();
			EclipseLifecyclePlugin.updateSettingsJDT(p);

			if (!p.getErrors()
				.isEmpty()
				|| !p.getWarnings()
					.isEmpty()) {
				File bndFile = p.getFile("bnd.bnd");
				String bnd = IO.collect(bndFile);
				bnd = TODO + //
					Strings.join(TODO, p.getErrors()) + //
					Strings.join(TODO, p.getWarnings()) + //
					"\n####\n\n" + bnd;
				IO.store(bnd, bndFile);
				p.forceRefresh();
			}
			p.getInfo(this, bsn + ": ");
			return p;
		}
	}

	private Document toDoc(Resource resource) throws Exception {
		if (resource == null)
			return null;

		return db.parse(resource.openInputStream());
	}

	/** Mark this converter to verify without writing (currently a no-op). */
	public void verify() {}

	/** Mark the target bnd project directory to be deleted before writing. */
	public void clean() {
		this.clean = true;
	}

	/**
	 * Set the Eclipse working set name to embed in the generated
	 * {@code bnd.bnd}.
	 */
	public void setWorkingset(String workingset) {
		this.workingset = workingset;
	}
}
