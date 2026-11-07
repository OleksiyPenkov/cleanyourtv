package com.gathin.tvhome;

import java.util.List;
import java.util.regex.Pattern;

/** The files ThemeProvider may write or delete under Config.dir, by the URI's path segments. */
final class ThemeFiles {
    static final String WALLPAPERS = "wallpapers";
    static final String THEMES = "themes";
    /** The names tools/theme.sh accepts for a collection image, and the types WallpaperRotation reads. */
    private static final Pattern IMAGE = Pattern.compile("[A-Za-z0-9_.-]+\\.(jpg|jpeg|png|webp)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FOLDER = Pattern.compile("[A-Za-z0-9_-]+");

    private ThemeFiles() {}

    /** The path relative to the theme directory, e.g. "config.json" or "wallpapers/dusk.jpg", or null if refused. */
    static String path(List<String> segments) {
        if (segments == null) return null;
        if (segments.size() >= 3 && THEMES.equals(segments.get(0)) && themeName(segments.get(1))) {
            String sub = singleThemePath(segments.subList(2, segments.size()));
            return sub == null ? null : THEMES + "/" + segments.get(1) + "/" + sub;
        }
        return singleThemePath(segments);
    }

    private static String singleThemePath(List<String> segments) {
        if (segments.size() == 1) {
            String name = segments.get(0);
            return Config.FILE_NAME.equals(name) || "wallpaper.jpg".equals(name) || "theme_revision".equals(name)
                    ? name : null;
        }
        if (segments.size() == 2 && WALLPAPERS.equals(segments.get(0))
                && (collectionImage(segments.get(1)) || WallpaperCaptions.FILE_NAME.equals(segments.get(1)))) {
            return WALLPAPERS + "/" + segments.get(1);
        }
        return null;
    }

    /** True for "wallpapers" or "themes/<name>/wallpapers": a delete of it clears the collection's images. */
    static boolean isCollection(List<String> segments) {
        if (segments == null) return false;
        if (segments.size() == 1) return WALLPAPERS.equals(segments.get(0));
        return segments.size() == 3 && THEMES.equals(segments.get(0))
                && themeName(segments.get(1)) && WALLPAPERS.equals(segments.get(2));
    }

    static boolean themeName(String name) {
        return name != null && FOLDER.matcher(name).matches();
    }

    static boolean collectionImage(String name) {
        return name != null && !name.startsWith(".") && IMAGE.matcher(name).matches();
    }
}
