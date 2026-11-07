package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ThemeFilesTest {
    @Test public void themeFilesAreAllowed() {
        assertEquals("config.json", ThemeFiles.path(Collections.singletonList("config.json")));
        assertEquals("wallpaper.jpg", ThemeFiles.path(Collections.singletonList("wallpaper.jpg")));
        assertEquals("theme_revision", ThemeFiles.path(Collections.singletonList("theme_revision")));
    }

    @Test public void collectionImagesAreAllowed() {
        assertEquals("wallpapers/dusk.jpg", ThemeFiles.path(Arrays.asList("wallpapers", "dusk.jpg")));
        assertEquals("wallpapers/Night_2.WEBP", ThemeFiles.path(Arrays.asList("wallpapers", "Night_2.WEBP")));
    }

    @Test public void theCollectionsCaptionsAreAllowed() {
        assertEquals("wallpapers/captions.txt", ThemeFiles.path(Arrays.asList("wallpapers", "captions.txt")));
        assertNull(ThemeFiles.path(Collections.singletonList("captions.txt")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "notes.txt")));
        assertFalse(ThemeFiles.collectionImage("captions.txt"));
    }

    @Test public void anythingElseIsRefused() {
        assertNull(ThemeFiles.path(null));
        assertNull(ThemeFiles.path(Collections.emptyList()));
        assertNull(ThemeFiles.path(Collections.singletonList("cards.json")));
        assertNull(ThemeFiles.path(Collections.singletonList("..")));
        assertNull(ThemeFiles.path(Arrays.asList("..", "config.json")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "..")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "run.sh")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "a b.jpg")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "../config.jpg")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", ".jpg")));
        assertNull(ThemeFiles.path(Arrays.asList("other", "dusk.jpg")));
        assertNull(ThemeFiles.path(Arrays.asList("wallpapers", "sub", "dusk.jpg")));
    }

    @Test public void onlyTheBareFolderIsTheCollection() {
        assertTrue(ThemeFiles.isCollection(Collections.singletonList("wallpapers")));
        assertFalse(ThemeFiles.isCollection(Arrays.asList("wallpapers", "dusk.jpg")));
        assertFalse(ThemeFiles.isCollection(Collections.singletonList("config.json")));
    }

    @Test public void namedThemeFilesAreAllowed() {
        assertEquals("themes/aurora/config.json",
                ThemeFiles.path(Arrays.asList("themes", "aurora", "config.json")));
        assertEquals("themes/aurora/wallpaper.jpg",
                ThemeFiles.path(Arrays.asList("themes", "aurora", "wallpaper.jpg")));
        assertEquals("themes/stockholm/wallpapers/01-gryning.jpg",
                ThemeFiles.path(Arrays.asList("themes", "stockholm", "wallpapers", "01-gryning.jpg")));
        assertEquals("themes/stockholm/wallpapers/captions.txt",
                ThemeFiles.path(Arrays.asList("themes", "stockholm", "wallpapers", "captions.txt")));
        assertTrue(ThemeFiles.isCollection(Arrays.asList("themes", "stockholm", "wallpapers")));
        assertNull(ThemeFiles.path(Arrays.asList("themes", "..", "config.json")));
        assertNull(ThemeFiles.path(Arrays.asList("themes", ".hidden", "config.json")));
        assertNull(ThemeFiles.path(Arrays.asList("themes", "a b", "config.json")));
    }

    @Test public void themeFoldersListsValidSubfoldersSortedByName() throws Exception {
        java.io.File root = java.nio.file.Files.createTempDirectory("themes-test").toFile();
        java.io.File themes = new java.io.File(root, "themes");
        new java.io.File(themes, "stockholm").mkdirs();
        new java.io.File(themes, "aurora").mkdirs();
        new java.io.File(themes, ".hidden").mkdirs();
        new java.io.File(themes, "empty").mkdirs();
        java.nio.file.Files.write(new java.io.File(themes, "stockholm/config.json").toPath(), "{}".getBytes());
        java.nio.file.Files.write(new java.io.File(themes, "aurora/config.json").toPath(), "{}".getBytes());
        assertEquals(Arrays.asList("aurora", "stockholm"), Config.themeNames(root));
        assertEquals(new java.io.File(themes, "aurora"), Config.themeDir(root, "aurora"));
        assertEquals(root, Config.themeDir(root, "missing"));
        assertEquals(root, Config.themeDir(root, ""));
        assertEquals(root, Config.themeDir(root, null));
    }
}
