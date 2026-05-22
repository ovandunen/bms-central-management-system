package com.solarcsms.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JUnit condition helpers for Docker-dependent tests.
 */
public final class DockerConditions {

    private DockerConditions() {
    }

    /**
     * @return {@code true} when the Docker socket is available (PostGIS devservices can start)
     */
    public static boolean isDockerAvailable() {
        return Files.exists(Path.of("/var/run/docker.sock"))
                || System.getenv("DOCKER_HOST") != null
                || commandExists("docker");
    }

    private static boolean commandExists(String command) {
        try {
            Process process = new ProcessBuilder(command, "info")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
