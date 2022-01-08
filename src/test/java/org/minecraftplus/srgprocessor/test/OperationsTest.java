package org.minecraftplus.srgprocessor.test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.*;
import org.minecraftplus.srgprocessor.api.CleanerBuilder;
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
    Path input, input_inferred, dictionary, inferred, filtered, cleaned, deduced;

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
        // Inputs
        input = root.resolve("input.txt");
        input_inferred = root.resolve("input_inferred.txt");
        dictionary = root.resolve("deduce/dictionary.txt");

        // Results
        inferred = root.resolve("clean/inferred.txt");
        filtered = root.resolve("clean/filtered.txt");
        cleaned = root.resolve("clean/cleaned.txt");
        deduced = root.resolve("deduce/deduced.txt");
    }

    @Test
    public void testOnlyInferring() throws IOException {
        Path pattern = root.resolve("clean/infer/pattern.txt");

        CleanerBuilder builder = new CleanerBuilder()
                .input(input).output(inferred)
                .infer(true);

        builder.build().run();
        test(inferred, pattern);
    }

    @Test
    public void testOnlyFiltering() throws IOException {
        Path pattern = root.resolve("clean/filter/pattern.txt");

        CleanerBuilder builder = new CleanerBuilder()
                .input(input).output(filtered)
                .filter(true);

        builder.build().run();
        test(filtered, pattern);
    }

    @Test
    public void testCleaning() throws IOException {
        Path pattern = root.resolve("clean/pattern.txt");

        CleanerBuilder builder = new CleanerBuilder()
                .input(input).output(cleaned)
                .infer(true).filter(true);

        builder.build().run();
        test(cleaned, pattern);
    }

    @Test
    public void testDeducing() throws IOException {
        Path pattern = root.resolve("deduce/pattern.txt");

        DeducerBuilder builder = new DeducerBuilder()
                .input(input_inferred).output(deduced)
                .dictionary(dictionary).collectStatistics();

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
