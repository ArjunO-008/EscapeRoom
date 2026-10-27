package core;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

/**
 * Single place that opens game assets (images, maps, music).
 *
 * Paths are written the way the project always wrote them, e.g.
 * "src/map/resources/Key.png". They work in both setups:
 *   - packaged build: the assets live inside the jar (classpath root is src/)
 *   - development:    `java -cp bin core.App` from the project root (plain files)
 */
public final class Assets {

    private Assets() {
    }

    /** "src/map/x.png" -> "map/x.png" (the classpath name). */
    private static String classpathName(String path) {
        String p = path.replace('\\', '/');
        return p.startsWith("src/") ? p.substring(4) : p;
    }

    /** Opens an asset, or throws IOException if it exists nowhere. */
    public static InputStream open(String path) throws IOException {
        InputStream in = Assets.class.getClassLoader()
                .getResourceAsStream(classpathName(path));
        if (in != null) {
            return new BufferedInputStream(in);
        }
        File f = new File(path);
        if (f.isFile()) {
            return new BufferedInputStream(new FileInputStream(f));
        }
        throw new IOException("Asset not found: " + path);
    }

    public static boolean exists(String path) {
        if (Assets.class.getClassLoader().getResource(classpathName(path)) != null) {
            return true;
        }
        return new File(path).isFile();
    }

    public static BufferedImage image(String path) throws IOException {
        try (InputStream in = open(path)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                throw new IOException("Could not decode image: " + path);
            }
            return img;
        }
    }

    /** Resolves a path written inside a file (e.g. a tileset "source") against that file's folder. */
    public static String sibling(String basePath, String relative) {
        String base = basePath.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        String dir = slash >= 0 ? base.substring(0, slash) : "";
        Path p = Paths.get(dir.isEmpty() ? relative : dir + "/" + relative).normalize();
        return p.toString().replace('\\', '/');
    }

    /** Per-user folder for save data (works when the app folder is read-only). */
    public static File userDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", ".");
        File dir;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            dir = new File(appData != null ? appData : home, "EscapeRoom");
        } else if (os.contains("mac")) {
            dir = new File(home, "Library/Application Support/EscapeRoom");
        } else {
            String xdg = System.getenv("XDG_DATA_HOME");
            dir = (xdg != null && !xdg.isEmpty())
                    ? new File(xdg, "EscapeRoom")
                    : new File(home, ".local/share/EscapeRoom");
        }
        return dir;
    }
}
