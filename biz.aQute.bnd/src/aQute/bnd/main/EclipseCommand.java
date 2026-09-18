package aQute.bnd.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.eclipse.EclipseLifecyclePlugin;
import aQute.bnd.eclipse.LibPde;
import aQute.bnd.header.Attrs;
import aQute.bnd.main.bnd.projectOptions;
import aQute.bnd.main.bnd.workspaceOptions;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Processor;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;

public class EclipseCommand extends Processor {
	private static final Logger				logger	= LoggerFactory.getLogger(EclipseCommand.class);
	private static final DocumentBuilderFactory	dbf	= DocumentBuilderFactory.newInstance();

	@Description("Show info about the current directory's eclipse project")
	interface eclipseOptions extends workspaceOptions {
		@Description("Path to the project")
		String dir();
	}

	private final bnd bnd;

	public EclipseCommand(bnd bnd) {
		super(bnd);
		this.bnd = bnd;
	}

	// -----------------------------------------------------------------------
	// eclipse sync
	// -----------------------------------------------------------------------

	@Arguments(arg = {})
	@Description("Synchronized the ./cnf/.settings directory to all the projects")
	interface SyncSettings extends Options {

	}

	@Description("Synchronized the ./cnf/.settings directory to all the projects")
	public void _sync(SyncSettings sync) {
		Workspace workspace = bnd.getWorkspace();
		if (workspace == null) {
			error("Need to be in workspace");
			return;
		}

		File sourceDir = workspace.getFile(Workspace.CNFDIR + "/.settings");
		if (!sourceDir.isDirectory()) {
			error("The Eclipse  .settings directory is not a directory: %s", sourceDir);
			return;
		}

		List<File> toCopy = IO.listFiles(sourceDir);

		for (Project p : workspace.getAllProjects()) {
			if (p.getName()
				.equals(Project.BNDCNF))
				continue;

			File targetDir = p.getFile(".settings");
			targetDir.mkdirs();

			if (!targetDir.isDirectory()) {
				error("Cannot create .settings directory in %s", p);
				continue;
			}

			for (File sourceFile : toCopy) {
				bnd.trace("Copying to %s to %s", sourceFile, targetDir);
				try {
					File targetFile = new File(targetDir, sourceFile.getName());
					IO.copy(sourceFile, targetFile);
				} catch (IOException e) {
					exception(e, "Failed to copy %s to %s because %s", sourceFile, targetDir, e.getMessage());
				}
			}
		}
	}

	// -----------------------------------------------------------------------
	// eclipse pde – import Eclipse PDE projects into a bnd workspace
	// -----------------------------------------------------------------------

	@Description("Eclipse PDE to bnd project conversion")
	@Arguments(arg = {
		"[pdeProjectDir]", "..."
	})
	interface ToBndOptions extends workspaceOptions {

		@Description("Delete the existing bnd project directory before writing")
		boolean clean();

		@Description("Eclipse Working Set name for imported projects")
		String[] set();

		@Description("Path to a bnd instructions file that controls which projects are imported")
		String instructions();

		@Description("If a directory is not a PDE project (no build.properties), recurse into subdirectories")
		boolean recurse();

		@Description("Use the PDE manifest as-is (-manifest: META-INF/MANIFEST.MF) instead of letting bnd derive it")
		boolean manifestfirst();
	}

	@Description("Eclipse PDE to bnd project conversion")
	public void _pde(ToBndOptions options) throws Exception {
		if (options._arguments()
			.isEmpty()) {
			bnd.out.println(options._help());
			return;
		}
		Workspace workspace = bnd.getWorkspace(options.workspace());
		if (workspace == null) {
			error("No workspace");
			return;
		}
		List<String> arguments = options._arguments();
		Set<Project> projects = new HashSet<>();

		Instructions selection = new Instructions("*");
		UTF8Properties properties = new UTF8Properties();

		if (options.instructions() != null) {
			File f = getFile(options.instructions());
			if (!f.isFile()) {
				error("Cannot find instructions file %s", f);
				return;
			}
			properties.load(f, this);
			selection = new Instructions(properties.getProperty("pde.selection"));
			logger.debug("pde.selection {}", selection);
		}

		String optWorkingset = options.set() == null ? null : Strings.join(options.set());
		Set<Instruction> had = new HashSet<>();

		for (String path : arguments) {
			logger.debug("look in path {}", path);
			File f = getFile(path);
			processDirectory(options, workspace, projects, selection, optWorkingset, had, f);
		}

		Set<Instruction> missingProjects = new HashSet<>(selection.keySet());
		missingProjects.removeAll(had);

		if (!missingProjects.isEmpty()) {
			error("Not all selections in the instruction file were used: %s", missingProjects);
		}

		Set<String> missing = new TreeSet<>();
		for (Project p : projects) {
			getInfo(p, p.getName() + ": ");
		}
		if (!missing.isEmpty()) {
			getParent().error("Missing packages %s", Strings.join("\n", missing));
		}

		getInfo(workspace, "ws: ");
	}

	// -----------------------------------------------------------------------
	// eclipse classpath – recalculate .classpath from bnd settings
	// -----------------------------------------------------------------------

	@Description("Calculate the Eclipse .classpath file from bnd.bnd settings. "
		+ "By default the old and the calculated classpath are shown. "
		+ "Specify -u to update the .classpath file.")
	@Arguments(arg = {})
	interface ClasspathOptions extends projectOptions {
		@Description("Update the .classpath file")
		boolean update();
	}

	@Description("Calculate the Eclipse .classpath file from bnd.bnd settings")
	public void _classpath(ClasspathOptions options) throws Exception {
		Project project = bnd.getProject(options.project());
		if (project == null) {
			error("Not in a project or -p set to an invalid directory");
			return;
		}

		File f = project.getFile(".classpath");
		if (!f.isFile()) {
			error("No .classpath file %s", f);
			return;
		}
		String oldClasspath = IO.collect(f);

		DocumentBuilder db = dbf.newDocumentBuilder();
		try (InputStream in = new FileInputStream(f)) {
			Document doc = db.parse(in);
			String classpath = EclipseLifecyclePlugin.toClasspath(project, doc);
			if (options.update())
				IO.store(classpath, f);
			else {
				bnd.out.println("Current version");
				bnd.out.println(oldClasspath);
				bnd.out.println("Proposed version (use -u to copy this to " + f + ")");
				bnd.out.println(classpath);
			}
		}
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

	private void processDirectory(ToBndOptions options, Workspace workspace, Set<Project> projects,
		Instructions selection, String optWorkingset, Set<Instruction> had, File f) throws IOException, Exception {
		if (f.isDirectory()) {
			logger.debug("visiting dir {}", f);
			if (getFile(f, "build.properties").isFile()) {
				logger.debug("found PDE project {}", f);

				Instruction matcher = selection.matcher(f.getName());
				if (matcher != null) {
					logger.debug("matched PDE project {}", f);
					Attrs attrs = selection.get(matcher);
					logger.info("Matched {} {}", f.getName(), attrs);
					String workingset = attrs.get("-workingset", optWorkingset);

					Project p = processFile(workingset, options.clean(), workspace, f);
					if (p != null) {
						for (Map.Entry<String, String> entry : attrs.entrySet()) {
							p.setProperty(entry.getKey(), entry.getValue());
							logger.info("Set attr {} {}", p, entry);
						}
						projects.add(p);
						had.add(matcher);
					}
				} else {
					logger.debug("skipped PDE project {}", f);
				}
			} else {
				if (options.recurse()) {
					logger.debug("recursing {}", f);
					for (File sub : f.listFiles()) {
						processDirectory(options, workspace, projects, selection, optWorkingset, had, sub);
					}
				}
			}
		}
	}

	private Project processFile(String workingsets, boolean clean, Workspace workspace, File f)
		throws IOException, Exception {
		logger.info("Process {}", f);
		LibPde pde = new LibPde(workspace, f);

		if (workingsets != null)
			pde.setWorkingset(workingsets);

		if (clean)
			pde.clean();

		pde.verify();
		if (!pde.isOk()) {
			getParent().getInfo(pde, f.getName());
		} else {
			Project p = pde.write();
			getParent().getInfo(p);
			getParent().getLogger()
				.trace("converted {}", p);
			return p;
		}
		return null;
	}
}

