package com.gathin.tvhome;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LauncherSettingsTest {
    @Test public void stepIndexWrapsBothWays() {
        assertEquals(1, Config.stepIndex(0, 4, 1));
        assertEquals(3, Config.stepIndex(0, 4, -1));
        assertEquals(0, Config.stepIndex(3, 4, 1));
    }

    @Test public void chosenIntUsesValidChoiceOrFallsBackToTheme() {
        assertEquals(50, Config.chosenInt(50, 30, 0, 90));
        assertEquals(0, Config.chosenInt(0, 30, 0, 90));
        assertEquals(30, Config.chosenInt(-1, 30, 0, 90));
        assertEquals(30, Config.chosenInt(95, 30, 0, 90));
    }

    @Test public void dockStyleRoundTripsBandAndGlass() {
        // 0 = Solid (band on, glass off), 1 = Glass (band on, glass on), 2 = Floating (band off)
        assertEquals(0, Config.dockStyle(true, false));
        assertEquals(1, Config.dockStyle(true, true));
        assertEquals(2, Config.dockStyle(false, false));
        assertEquals(2, Config.dockStyle(false, true));

        assertTrue(Config.dockBandForStyle(0));
        assertFalse(Config.dockGlassForStyle(0));
        assertTrue(Config.dockBandForStyle(1));
        assertTrue(Config.dockGlassForStyle(1));
        assertFalse(Config.dockBandForStyle(2));
        assertFalse(Config.dockGlassForStyle(2));
    }

    @Test public void glowModeRoundTripsAmbientAndAppGlow() {
        // 0 = Off, 1 = Dock only, 2 = All tiles
        assertEquals(0, Config.glowMode(false, false));
        assertEquals(1, Config.glowMode(true, false));
        assertEquals(2, Config.glowMode(true, true));
        assertEquals(2, Config.glowMode(false, true));

        assertFalse(Config.ambientGlowForMode(0));
        assertFalse(Config.appGlowForMode(0));
        assertTrue(Config.ambientGlowForMode(1));
        assertFalse(Config.appGlowForMode(1));
        assertTrue(Config.ambientGlowForMode(2));
        assertTrue(Config.appGlowForMode(2));
    }

    @Test public void stepWallpaperCyclesDefaultImagesAndNone() {
        List<File> images = Arrays.asList(new File("01-dusk.jpg"), new File("02-coast.jpg"));
        // null (Theme default) -> 01-dusk.jpg -> 02-coast.jpg -> "none" -> null
        assertEquals("01-dusk.jpg", LauncherSettingsActivity.stepWallpaper(null, images, 1));
        assertEquals("02-coast.jpg", LauncherSettingsActivity.stepWallpaper("01-dusk.jpg", images, 1));
        assertEquals("none", LauncherSettingsActivity.stepWallpaper("02-coast.jpg", images, 1));
        assertNull(LauncherSettingsActivity.stepWallpaper("none", images, 1));
        assertEquals("none", LauncherSettingsActivity.stepWallpaper(null, images, -1));
        assertNull(LauncherSettingsActivity.stepWallpaper("01-dusk.jpg", images, -1));
    }

    @Test public void stepWallpaperWithNoImagesTogglesDefaultAndNone() {
        List<File> empty = Collections.emptyList();
        assertEquals("none", LauncherSettingsActivity.stepWallpaper(null, empty, 1));
        assertNull(LauncherSettingsActivity.stepWallpaper("none", empty, 1));
    }
}
