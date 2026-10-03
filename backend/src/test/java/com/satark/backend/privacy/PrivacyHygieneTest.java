package com.satark.backend.privacy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Phase 16 verification: privacy hygiene guardrail.
 *
 * <p>Scans production sources for patterns that could leak untrusted user
 * content (analysis text) into logs, stdout, or stack traces. Fails loudly
 * with file:line locations so leaks are caught in CI, not in production.
 * Runs anywhere (plain file I/O, no Spring context).
 */
class PrivacyHygieneTest {

    private static final List<Pattern> FORBIDDEN = List.of(
            Pattern.compile("System\\.(out|err)\\.print"),
            Pattern.compile("\\.printStackTrace\\s*\\("),
            // Logging anything derived from request/model text getters.
            Pattern.compile("\\blog\\.(trace|debug|info|warn|error)\\s*\\([^;]*("
                    + "request\\.text\\(\\)|getInputText\\(\\)|getClaimText\\(\\)|getExplanation\\(\\)"
                    + "|analysis\\.get|savedAnalysis\\.get)\\)"));

    @Test
    void productionSourcesContainNoLeakPatterns() throws Exception {
        Path base = Paths.get(System.getProperty("user.dir"), "src", "main", "java");
        if (!Files.isDirectory(base)) {
            base = Paths.get(System.getProperty("user.dir"), "backend", "src", "main", "java");
        }
        assertTrue(Files.isDirectory(base), "cannot locate src/main/java from " + System.getProperty("user.dir"));
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(base)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(p);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    for (Pattern pattern : FORBIDDEN) {
                        if (pattern.matcher(line).find()) {
                            violations.add(base.relativize(p) + ":" + (i + 1) + ": " + line.trim());
                        }
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), "privacy violations:\n" + String.join("\n", violations));
    }
}
