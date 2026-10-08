package org.gradle.wrapper;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class GradleWrapperMain {
    private static final String EXPECTED_JAVA = "17";

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--wrapper-self-test".equals(args[0])) {
            System.out.println("LyceumMobile Gradle wrapper launcher: OK");
            System.out.println("Java: " + System.getProperty("java.version"));
            return;
        }

        String javaSpec = System.getProperty("java.specification.version", "");
        if (!EXPECTED_JAVA.equals(javaSpec)) {
            System.err.println("ERROR: LyceumMobile FINAL requires JDK 17.");
            System.err.println("Current Java specification version: " + javaSpec);
            System.exit(3);
        }

        Path project = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path propertiesPath = project.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (!Files.isRegularFile(propertiesPath)) {
            throw new FileNotFoundException("Missing " + propertiesPath);
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath)) {
            props.load(in);
        }

        String distributionUrl = props.getProperty("distributionUrl");
        if (distributionUrl == null || distributionUrl.isBlank()) {
            throw new IllegalStateException("distributionUrl is missing");
        }

        String gradleVersion = "8.10.2";
        Path home = Paths.get(System.getProperty("user.home"), ".gradle", "wrapper", "dists",
                "lyceummobile-final", "gradle-" + gradleVersion);
        Path installation = home.resolve("gradle-" + gradleVersion);
        Path bin = installation.resolve("bin")
                .resolve(isWindows() ? "gradle.bat" : "gradle");

        if (!Files.isRegularFile(bin)) {
            Files.createDirectories(home);
            Path zip = home.resolve("gradle-" + gradleVersion + "-bin.zip");
            if (!Files.isRegularFile(zip) || Files.size(zip) < 1_000_000L) {
                System.out.println("Downloading Gradle " + gradleVersion + "...");
                download(URI.create(distributionUrl), zip);
            }
            System.out.println("Extracting Gradle " + gradleVersion + "...");
            unzip(zip, home);
        }

        if (!Files.isRegularFile(bin)) {
            throw new FileNotFoundException("Gradle executable not found after extraction: " + bin);
        }

        List<String> command = new ArrayList<>();
        if (isWindows()) {
            command.add("cmd.exe");
            command.add("/d");
            command.add("/c");
            command.add(bin.toString());
        } else {
            command.add("/bin/sh");
            command.add(bin.toString());
        }
        command.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(project.toFile());
        pb.inheritIO();
        int exit = pb.start().waitFor();
        System.exit(exit);
    }

    private static void download(URI uri, Path target) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();

        Path temp = target.resolveSibling(target.getFileName() + ".part");
        Files.deleteIfExists(temp);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(java.time.Duration.ofMinutes(5))
                .header("User-Agent", "LyceumMobile-GradleWrapper/1.0")
                .GET()
                .build();

        HttpResponse<Path> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofFile(temp)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(temp);
            throw new IOException("Gradle download failed: HTTP " + response.statusCode());
        }

        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void unzip(Path zip, Path destination) throws Exception {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                Path output = destination.resolve(entry.getName()).normalize();
                if (!output.startsWith(destination.normalize())) {
                    throw new IOException("Unsafe ZIP entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    try (OutputStream out = Files.newOutputStream(output,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        in.transferTo(out);
                    }
                }
                in.closeEntry();
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
