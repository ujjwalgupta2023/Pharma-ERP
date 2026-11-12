package com.pharma.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

/**
 * Opens the user's default browser at the app URL once the server is ready.
 * This makes the packaged desktop app behave like a normal application: the
 * user clicks the icon and the UI appears, without touching a browser manually.
 *
 * Disabled by setting the environment variable PHARMA_NO_BROWSER=1 (useful when
 * running headless or on a server).
 */
@Component
public class BrowserLauncher {

    private final Environment env;

    public BrowserLauncher(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        if ("1".equals(System.getenv("PHARMA_NO_BROWSER"))) {
            return;
        }
        String port = env.getProperty("server.port", "8081");
        String url = "http://localhost:" + port;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {
            // Fall through to OS-specific command below.
        }
        // Fallback for environments where java.awt.Desktop is unavailable.
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", url);
            } else {
                pb = new ProcessBuilder("xdg-open", url);
            }
            pb.start();
        } catch (Exception ignored) {
            System.out.println("Open your browser at " + url);
        }
    }
}
