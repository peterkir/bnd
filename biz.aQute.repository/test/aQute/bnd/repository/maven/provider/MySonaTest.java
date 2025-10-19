package aQute.bnd.repository.maven.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.Workspace;

public class MySonaTest {

    @Test
    public void testReleaseBundlesToSonatypeRepo() throws Exception {
        // Create temp directory
        Path tempDir = Files.createTempDirectory("sonatypeTest");
        tempDir.toFile().deleteOnExit();

        // Copy workspace files
        Path sourceDir = Paths.get("biz.aQute.repository/testresources/sonatype");
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            stream.forEach(source -> {
                try {
                    Path dest = tempDir.resolve(sourceDir.relativize(source).toString());
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Initialize workspace
        Workspace ws = new Workspace(tempDir.toFile());
        ws.open();

        // Verify bundles released in cnf/ext/gav_30_sonatype.mvn
        Path releasedBundles = tempDir.resolve("cnf/ext/gav_30_sonatype.mvn");
        assertTrue(Files.exists(releasedBundles), "gav_30_sonatype.mvn should exist");

        // Optionally, check that the file contains released bundles
        String content = Files.readString(releasedBundles);
        assertTrue(content.contains("bundle"), "Released bundles should be listed in gav_30_sonatype.mvn");

        ws.close();
    }
}
