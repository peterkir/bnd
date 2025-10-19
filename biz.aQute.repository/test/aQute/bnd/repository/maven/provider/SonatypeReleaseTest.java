package aQute.bnd.repository.maven.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import aQute.bnd.build.Project;
import aQute.bnd.build.Project.ReleaseParameter;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;
import aQute.lib.io.IO;

public class SonatypeReleaseTest {

	@InjectTemporaryDirectory
	static File wsDir;

	@BeforeAll
	public static void prepare() throws IOException {
		IO.delete(wsDir);
		assertFalse("directory could not be deleted " + wsDir, wsDir.exists());
		IO.copy(IO.getFile("testresources/sonatype"), wsDir);
		System.err.println("--" + wsDir);
	}

	@AfterAll
	public static void tearDown() throws Exception {}

	@Test
	public void testReleaseDeployment() throws Exception {

		LinkedList<File> releasedFiles = new LinkedList<File>();
		try (Workspace ws = Workspace.findWorkspace(wsDir)) {
			ws.setPedantic(true);
			List<RepositoryPlugin> repos = ws.getRepositories();
			assertEquals(repos.size(), 5);
			File releasedVersionFile = new File(wsDir, "cnf/ext/gav_30_sonatype.mvn");
			assertFalse(releasedVersionFile.exists());
			Set<Project> projects = new LinkedHashSet<>();
			projects.addAll(ws.getAllProjects());
			for (Iterator<Project> iterator = projects.iterator(); iterator.hasNext();) {
				Project p = iterator.next();
				releasedFiles.addAll(Arrays.asList(p.build()));
				if (iterator.hasNext()) {
					p.release();
				} else {
					p.release(new ReleaseParameter(null, false, true));
				}
			}
			assertTrue(releasedVersionFile.exists());
			byte[] bytes = java.nio.file.Files.readAllBytes(releasedVersionFile.toPath());
			String content = new String(bytes);
			// Check that all bundles are in released content
			for (File file : releasedFiles) {
				String filename = file.getName();
				String nameWithoutExtension = filename.substring(0, filename.lastIndexOf('.'));
				boolean found = content
					.matches("(?s).*\\b" + java.util.regex.Pattern.quote(nameWithoutExtension) + "\\b.*");
				assertTrue("Filename without extension '" + nameWithoutExtension + "' not found as word in content",
					found);
			}
			File deploymentIDFile = new File(wsDir,
				MavenBndRepository.SONATYPE_RELEASE_DIR + "/" + MavenBndRepository.SONATYPE_DEPLOYMENTID_FILE);
			assertTrue("Deployment ID file not found: " + deploymentIDFile, deploymentIDFile.exists());
		}
	}

}
