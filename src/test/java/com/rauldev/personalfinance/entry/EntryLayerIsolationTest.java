package com.rauldev.personalfinance.entry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

class EntryLayerIsolationTest {

    private static final Path BASE = Path.of("src/main/java/com/rauldev/personalfinance");

    @Test
    void domainDoesNotDependOnSpring() throws IOException {
        assertNoSpringReferences("domain");
    }

    @Test
    void applicationDoesNotDependOnSpring() throws IOException {
        assertNoSpringReferences("application");
    }

    @Test
    void infrastructureDoesNotDependOnSpring() throws IOException {
        assertNoSpringReferences("infrastructure");
    }

    private void assertNoSpringReferences(String layer) throws IOException {
        Path root = BASE.resolve(layer);
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertFalse(files.isEmpty(), "No sources found under " + root);

        for (Path file : files) {
            assertFalse(Files.readString(file).contains("org.springframework"),
                file + " must not reference org.springframework");
        }
    }

    @Test
    void entryWebDoesNotReferenceInfrastructure() throws IOException {
        Path root = BASE.resolve("entry/web");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(root)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertFalse(files.isEmpty(), "No sources found under " + root);

        for (Path file : files) {
            assertFalse(Files.readString(file).contains(".infrastructure."),
                file + " must not reference the infrastructure layer");
        }
    }

    @Test
    void applicationClassLivesInEntryPackageSoScanningIsLimitedToEntryLayer() {
        assertEquals("com.rauldev.personalfinance.entry", PersonalFinanceApplication.class.getPackageName());
    }
}
