package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Translucent glass drawer over Home: Up/Down moves between rows, Left/Right steps values live while
 * Home updates behind the drawer, and OK cycles or opens a picker.
 */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    static final String EXTRA_EDIT_DOCK = "com.gathin.tvhome.extra.EDIT_DOCK_FROM_SETTINGS";

    static final int[] COLUMNS = {4, 5, 6, 7};
    static final int[] ROWS_BELOW_DOCK = {0, 1, 2, 3};
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private static final int[] WALLPAPER_DIMS = {0, 15, 30, 50, 70};
    private static final int[] WALLPAPER_ROTATIONS = {0, 15, 30, 60};
    private static final int[] IDLE_CLOCK_MINUTES = {0, 5, 10, 15, 30};

    private static final int STEP_COLUMNS = 0;
    private static final int STEP_ROWS_BELOW = 1;
    private static final int STEP_DOCK_STYLE = 2;
    private static final int STEP_DOCK_OPACITY = 3;
    private static final int STEP_CINEMATIC = 4;
    private static final int STEP_ALWAYS_NAMES = 5;
    private static final int STEP_THEME = 6;
    private static final int STEP_WALLPAPER = 7;
    private static final int STEP_WALLPAPER_DIM = 8;
    private static final int STEP_WALLPAPER_ROT = 9;
    private static final int STEP_FOCUS_STYLE = 10;
    private static final int STEP_APP_GLOW = 11;
    private static final int STEP_ANIMATIONS = 12;
    private static final int STEP_ADVANCED = 13;
    private static final int STEP_CLOCK_FMT = 14;
    private static final int STEP_IDLE_CLOCK = 15;
    private static final int STEP_BACK_CLOCK = 16;
    private static final int STEP_SHOW_STATS = 17;
    private static final int STEP_OPEN_ON_START = 18;
    private static final int STEP_COUNT = 19;

    private SharedPreferences choices;
    private Config cfg;
    private boolean rtlGrid;
    private boolean ru;
    private List<String> themes = Collections.emptyList();
    private final TextView[] stepRows = new TextView[STEP_COUNT];
    private final String[] baseLabels = new String[STEP_COUNT];
    private TextView followThemeChoice;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        rtlGrid = getResources().getBoolean(R.bool.rtl_app_grid);
        ru = "ru".equals(Locale.getDefault().getLanguage());
        ViewGroup content = findViewById(android.R.id.content);
        ViewGroup scrim = (ViewGroup) content.getChildAt(0);
        ViewGroup panel = (ViewGroup) scrim.getChildAt(0);
        LinearLayout rows = (LinearLayout) panel.getChildAt(0);
        if (getResources().getBoolean(R.bool.rtl_menu_bar)) {
            content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) panel.getLayoutParams();
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            int inset = lp.getMarginEnd();
            lp.setMarginEnd(0);
            lp.setMargins(inset, lp.topMargin, 0, lp.bottomMargin);
            panel.setLayoutParams(lp);
        }
        scrim.setOnClickListener(v -> finish());

        cfg = Config.load(this);
        themes = Config.themeNames(Config.dir(this));

        View reorderChoice = rows.getChildAt(1);
        reorderChoice.setVisibility(cfg.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });

        View editDockChoice = rows.getChildAt(2);
        editDockChoice.setVisibility(cfg.dockRow ? View.VISIBLE : View.GONE);
        editDockChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_EDIT_DOCK, true));
            finish();
        });

        int[] indices = {3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 19, 20, 21, 22};
        for (int i = 0; i < STEP_COUNT; i++) {
            final int key = i;
            TextView row = (TextView) rows.getChildAt(indices[i]);
            stepRows[i] = row;
            baseLabels[i] = row.getText().toString();
            row.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN || !v.isEnabled()) return false;
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    boolean left = keyCode == KeyEvent.KEYCODE_DPAD_LEFT;
                    v.playSoundEffect(left ? SoundEffectConstants.NAVIGATION_LEFT
                            : SoundEffectConstants.NAVIGATION_RIGHT);
                    step(key, (left ^ rtlGrid) ? -1 : 1);
                    return true;
                }
                return false;
            });
            row.setOnClickListener(v -> step(key, 1));
        }
        baseLabels[STEP_ANIMATIONS] = getString(R.string.animations_state, "").replace(":", "").trim();
        if (ru) {
            String[] ruLabels = ("Стиль дока|Кино-полка|Названия плиток|Тема|Затемнение обоев|Смена обоев|"
                    + "Стиль фокуса|Свечение плиток|Часы при простое|«Назад» открывает часы|Статистика системы").split("\\|");
            int[] ruIdx = {2, 4, 5, 6, 8, 9, 10, 11, 15, 16, 17};
            for (int i = 0; i < ruIdx.length; i++) baseLabels[ruIdx[i]] = ruLabels[i];
        }

        if (!themes.isEmpty()) stepRows[STEP_THEME].setVisibility(View.VISIBLE);

        if (cfg.openOnStartChoice) {
            stepRows[STEP_OPEN_ON_START].setVisibility(View.VISIBLE);
            stepRows[STEP_OPEN_ON_START].setOnClickListener(v -> showOpenOnStartPicker());
        }

        followThemeChoice = (TextView) rows.getChildAt(23);
        followThemeChoice.setOnClickListener(v -> {
            if (!Config.hasOnTvChoices(choices)) return;
            save(Config.clearOnTvChoices(choices.edit()).remove(OpenOnStart.KEY));
        });

        rows.getChildAt(25).setOnClickListener(v ->
                open(new Intent(this, HomeSettingsActivity.class)));
        rows.getChildAt(27).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));

        refresh();
        if (cfg.launcherSettingsButton) reorderChoice.requestFocus();
        else if (cfg.dockRow) editDockChoice.requestFocus();
        else stepRows[STEP_COLUMNS].requestFocus();
    }

    private void step(int key, int delta) {
        if (key == STEP_ADVANCED) {
            toggleAdvanced();
            return;
        }
        SharedPreferences.Editor ed = choices.edit();
        switch (key) {
            case STEP_COLUMNS:
                ed.putInt("columns", 4 + Config.stepIndex(cfg.columns - 4, 4, delta));
                break;
            case STEP_ROWS_BELOW:
                ed.putInt(Config.ROWS_BELOW_DOCK_KEY, Config.stepIndex(cfg.rowsBelowDock, 4, delta));
                break;
            case STEP_DOCK_STYLE:
                ed.putInt(Config.DOCK_STYLE_KEY, Config.stepIndex(
                        Config.dockStyle(cfg.dockBand, cfg.dockGlass), 3, delta));
                break;
            case STEP_DOCK_OPACITY:
                ed.putInt(Config.DOCK_OPACITY_KEY, stepInt(DOCK_OPACITIES, cfg.dockOpacity, delta));
                break;
            case STEP_CINEMATIC:
                ed.putBoolean(Config.CINEMATIC_HOME_KEY, !cfg.cinematicHome);
                break;
            case STEP_ALWAYS_NAMES:
                ed.putBoolean(Config.ALWAYS_SHOW_NAMES_KEY, !cfg.alwaysShowNames);
                break;
            case STEP_THEME:
                String cur = choices.getString(Config.THEME_CHOICE_KEY, null);
                int next = Config.stepIndex(cur == null ? 0 : themes.indexOf(cur) + 1,
                        themes.size() + 1, delta);
                Config.clearOnTvChoices(ed);
                if (next > 0) ed.putString(Config.THEME_CHOICE_KEY, themes.get(next - 1));
                break;
            case STEP_WALLPAPER:
                String nextWall = stepWallpaper(choices.getString(Config.WALLPAPER_CHOICE_KEY, null),
                        wallpaperImages(), delta);
                if (nextWall == null) ed.remove(Config.WALLPAPER_CHOICE_KEY);
                else ed.putString(Config.WALLPAPER_CHOICE_KEY, nextWall);
                break;
            case STEP_WALLPAPER_DIM:
                ed.putInt(Config.WALLPAPER_DIM_KEY, stepInt(WALLPAPER_DIMS, cfg.wallpaperDim, delta));
                break;
            case STEP_WALLPAPER_ROT:
                ed.putInt(Config.WALLPAPER_ROTATION_KEY,
                        stepInt(WALLPAPER_ROTATIONS, cfg.wallpaperRotationMinutes, delta));
                break;
            case STEP_FOCUS_STYLE:
                int fs = Config.GLASS_RIM.equals(cfg.glassStyle) ? 1
                        : Config.GLASS_HALO.equals(cfg.glassStyle) ? 2 : 0;
                int nextFs = Config.stepIndex(fs, 3, delta);
                ed.putString(Config.GLASS_STYLE_KEY, nextFs == 1 ? Config.GLASS_RIM
                        : nextFs == 2 ? Config.GLASS_HALO : Config.GLASS_DEFAULT);
                break;
            case STEP_APP_GLOW:
                ed.putInt(Config.GLOW_MODE_KEY, Config.stepIndex(
                        Config.glowMode(cfg.ambientGlow, cfg.appGlow), 3, delta));
                break;
            case STEP_ANIMATIONS:
                boolean anim = !cfg.animations;
                ed.putBoolean("animations", anim).putBoolean("advanced_effects", anim && cfg.advancedEffects);
                break;
            case STEP_CLOCK_FMT:
                int cf = "12".equals(cfg.clockFormat) ? 1 : "24".equals(cfg.clockFormat) ? 2 : 0;
                int nextCf = Config.stepIndex(cf, 3, delta);
                ed.putString(Config.CLOCK_FORMAT_KEY, nextCf == 1 ? "12" : nextCf == 2 ? "24" : "system");
                break;
            case STEP_IDLE_CLOCK:
                ed.putInt(Config.IDLE_CLOCK_KEY, stepInt(IDLE_CLOCK_MINUTES, cfg.homeIdleClockMinutes, delta));
                break;
            case STEP_BACK_CLOCK:
                ed.putBoolean(Config.BACK_OPENS_CLOCK_KEY, !cfg.backOpensClock);
                break;
            case STEP_SHOW_STATS:
                ed.putBoolean(Config.SHOW_STATS_KEY, !cfg.showStats);
                break;
        }
        save(ed);
    }

    private static int stepInt(int[] options, int current, int delta) {
        int idx = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == current) {
                idx = i;
                break;
            }
        }
        return options[Config.stepIndex(idx, options.length, delta)];
    }

    /** Steps through [Theme default (null), image_0 .. image_N-1, "none"]. */
    static String stepWallpaper(String current, List<File> images, int delta) {
        int total = images.size() + 2;
        int idx = "none".equals(current) ? images.size() + 1 : indexOf(images, current) + 1;
        int next = Config.stepIndex(idx, total, delta);
        if (next == 0) return null;
        if (next == images.size() + 1) return "none";
        return images.get(next - 1).getName();
    }

    private List<File> wallpaperImages() {
        File dir = Config.activeDir(this);
        List<File> images = new ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        return images;
    }

    private static String prettyWallpaperName(String name) {
        if (name == null || Config.WALLPAPER_NAME.equals(name)) return name == null ? "" : name;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        base = base.replaceFirst("^\\d+[-_]", "").replace('-', ' ').replace('_', ' ').trim();
        if (base.isEmpty()) return name;
        return base.substring(0, 1).toUpperCase(Locale.ROOT) + base.substring(1);
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    private void save(SharedPreferences.Editor ed) {
        setResult(RESULT_OK);
        ed.putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).commit();
        refresh();
    }

    private void toggleAdvanced() {
        if (cfg.animations && cfg.advancedEffects) {
            save(choices.edit().putBoolean("advanced_effects", false));
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.advanced_effects)
                    .setMessage(R.string.advanced_effects_warning)
                    .setPositiveButton(R.string.enable, (d, w) ->
                            save(choices.edit().putBoolean("advanced_effects", true)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private void setStep(int index, String value) {
        String label = baseLabels[index];
        SpannableString span = new SpannableString(label + "   ‹ " + value + " ›");
        span.setSpan(new ForegroundColorSpan(0xFF9AD4FF), label.length(), span.length(), 0);
        stepRows[index].setText(span);
    }

    private void refresh() {
        cfg = Config.load(this);
        String on = getString(R.string.on), off = getString(R.string.off);
        setStep(STEP_COLUMNS, String.valueOf(cfg.columns));
        setStep(STEP_ROWS_BELOW, String.valueOf(cfg.rowsBelowDock));
        int ds = Config.dockStyle(cfg.dockBand, cfg.dockGlass);
        setStep(STEP_DOCK_STYLE, ds == 2 ? (ru ? "Парящий" : "Floating")
                : ds == 1 ? (ru ? "Стекло" : "Glass") : (ru ? "Сплошной" : "Solid"));
        setStep(STEP_DOCK_OPACITY, cfg.dockOpacity + "%");
        setStep(STEP_CINEMATIC, cfg.cinematicHome ? on : off);
        setStep(STEP_ALWAYS_NAMES, cfg.alwaysShowNames ? on : off);

        String themeCurrent = choices.getString(Config.THEME_CHOICE_KEY, null);
        setStep(STEP_THEME, themeCurrent != null && themes.contains(themeCurrent) ? themeCurrent
                : getString(R.string.wallpaper_theme_default));

        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        setStep(STEP_WALLPAPER, "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                : wallpaperCurrent != null ? prettyWallpaperName(wallpaperCurrent)
                : getString(R.string.wallpaper_theme_default));
        setStep(STEP_WALLPAPER_DIM, cfg.wallpaperDim + "%");
        setStep(STEP_WALLPAPER_ROT, cfg.wallpaperRotationMinutes <= 0 ? off
                : getString(R.string.race_countdown_minutes, cfg.wallpaperRotationMinutes));

        setStep(STEP_FOCUS_STYLE, Config.GLASS_RIM.equals(cfg.glassStyle) ? (ru ? "Контур" : "Rim")
                : Config.GLASS_HALO.equals(cfg.glassStyle) ? (ru ? "Ореол" : "Halo")
                : (ru ? "Стекло" : "Glass"));
        int gm = Config.glowMode(cfg.ambientGlow, cfg.appGlow);
        setStep(STEP_APP_GLOW, gm == 2 ? (ru ? "Все" : "All") : gm == 1 ? (ru ? "Док" : "Dock") : off);

        setStep(STEP_ANIMATIONS, cfg.animations ? on : off);
        setStep(STEP_ADVANCED, cfg.animations && cfg.advancedEffects ? on : off);
        stepRows[STEP_ADVANCED].setEnabled(cfg.animations);
        stepRows[STEP_ADVANCED].setAlpha(cfg.animations ? 1f : 0.45f);

        setStep(STEP_CLOCK_FMT, getString("12".equals(cfg.clockFormat) ? R.string.clock_format_12
                : "24".equals(cfg.clockFormat) ? R.string.clock_format_24
                : R.string.clock_format_system));
        setStep(STEP_IDLE_CLOCK, cfg.homeIdleClockMinutes <= 0 ? off
                : getString(R.string.race_countdown_minutes, cfg.homeIdleClockMinutes));
        setStep(STEP_BACK_CLOCK, cfg.backOpensClock ? on : off);
        setStep(STEP_SHOW_STATS, cfg.showStats ? on : off);

        setStep(STEP_OPEN_ON_START, openOnStartLabel());
        followThemeChoice.setSelected(!Config.hasOnTvChoices(choices));
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
