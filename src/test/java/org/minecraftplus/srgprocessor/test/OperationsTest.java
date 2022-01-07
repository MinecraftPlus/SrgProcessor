package org.minecraftplus.srgprocessor.test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.*;
import org.minecraftplus.srgprocessor.SrgCleanMain;
import org.minecraftplus.srgprocessor.api.DeducerBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public class OperationsTest
{
    final Path root = getRoot().resolve("operations/");
    static final FileSystem imfs = Jimfs.newFileSystem(Configuration.unix());
    Path input, inferred, dictionary, deduced;

    private Path getRoot() {
        URL url = this.getClass().getResource("/test.marker");
        Assertions.assertNotNull(url, "Could not find test.marker");

        try {
            return new File(url.toURI()).getParentFile().toPath();
        } catch (URISyntaxException e) {
            return new File(url.getPath()).getParentFile().toPath();
        }
    }

    @BeforeEach
    public void init() throws IOException {
        input = root.resolve("input.txt");
        inferred = root.resolve("infer/inferred.txt");
        dictionary = root.resolve("deduce/dictionary.txt");
        deduced = root.resolve("deduce/deduced.txt");
    }

    @Test
    @Order(1)
    public void testInferring() throws IOException {
        Path pattern = root.resolve("infer/pattern.txt");

        String[] args = new String[] {
                "--in", input.toString(),
                "--out", inferred.toString(),
                "--inferMethodParameters",
        };

        SrgCleanMain.main(args); //TODO Make builder for clean task?
        test(inferred, pattern);
    }

    @Test
    @Order(2)
    public void testDeducing() throws IOException {
        Path pattern = root.resolve("deduce/pattern.txt");

        DeducerBuilder builder = new DeducerBuilder()
                .collectStatistics()
                .input(root.resolve("infer/pattern.txt")) // use inferred SRG
                .dictionary(dictionary).output(deduced);

        builder.build().run();
        test(deduced, pattern);
    }

    @AfterAll
    public static void exit() throws IOException {
        imfs.close();
    }

    private void test(Path dest, Path pattern) throws IOException {
        String pcontent = getFileContents(pattern).replaceAll("\r", ""); // MappingFile writes LF endings
        Assertions.assertEquals(pcontent, getFileContents(dest), "Output content differ from pattern");
    }

    private String getFileContents(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + file.toAbsolutePath(), e);
        }
    }
}
