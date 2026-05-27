package org.eclipse.slm.aas.testcontainers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

final class ExternalUrlSupport {

    private ExternalUrlSupport() {
        // Utility class
    }

    static int reserveFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to reserve free host port for external URL", e);
        }
    }

    static String normalizeHost(String externalUrlHost) {
        if (externalUrlHost == null || externalUrlHost.isBlank()) {
            return "localhost";
        }

        return externalUrlHost;
    }

    static void validatePort(int externalUrlPort) {
        if (externalUrlPort <= 0 || externalUrlPort > 65535) {
            throw new IllegalArgumentException("externalUrlPort must be between 1 and 65535");
        }
    }

    static String buildUrl(String externalUrlHost, int externalUrlPort) {
        return "http://" + externalUrlHost + ":" + externalUrlPort;
    }
}

