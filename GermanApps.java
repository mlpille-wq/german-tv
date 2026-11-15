package au.ludger.germantv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Finds the apps installed on the TV and picks out the German broadcaster apps. */
final class GermanApps {

    static final class AppEntry {
        final String pkg;
        final String label;
        final Drawable icon;
        final Drawable banner;

        AppEntry(String pkg, String label, Drawable icon, Drawable banner) {
            this.pkg = pkg;
            this.label = label;
            this.icon = icon;
            this.banner = banner;
        }
    }

    /** Whole words in an app's name that mark it as a German TV app (used for the first-run selection). */
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "ard", "zdf", "zdfneo", "zdftivi", "arte", "3sat", "dw", "mediathek", "tagesschau",
            "phoenix", "kika", "br", "ndr", "wdr", "mdr", "swr", "hr", "rbb", "sr", "funk"));

    private GermanApps() {
    }

    /** All apps that can be opened from the TV home screen, sorted by name (this app excluded). */
    static List<AppEntry> launchableApps(Context c) {
        PackageManager pm = c.getPackageManager();
        Map<String, AppEntry> map = new LinkedHashMap<>();
        addFromCategory(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, map);
        addFromCategory(pm, Intent.CATEGORY_LAUNCHER, map);
        map.remove(c.getPackageName());
        List<AppEntry> list = new ArrayList<>(map.values());
        Collections.sort(list, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return list;
    }

    private static void addFromCategory(PackageManager pm, String category, Map<String, AppEntry> map) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(category);
        List<ResolveInfo> found = pm.queryIntentActivities(i, 0);
        if (found == null) return;
        for (ResolveInfo ri : found) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (map.containsKey(pkg)) continue;
            CharSequence label = ri.loadLabel(pm);
            Drawable icon = ri.loadIcon(pm);
            Drawable banner = null;
            try {
                banner = pm.getApplicationBanner(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {
                // no banner
            }
            map.put(pkg, new AppEntry(pkg, label == null ? pkg : label.toString(), icon, banner));
        }
    }

    /** Apps whose name looks like a German public broadcaster app (ARD, ZDF, arte, 3sat, DW, ...). */
    static Set<String> defaultSelection(List<AppEntry> apps) {
        Set<String> result = new HashSet<>();
        for (AppEntry a : apps) {
            String name = a.label.toLowerCase(Locale.ROOT);
            if (name.contains("deutsche welle")) {
                result.add(a.pkg);
                continue;
            }
            for (String word : name.split("[^a-z0-9]+")) {
                if (KEYWORDS.contains(word)) {
                    result.add(a.pkg);
                    break;
                }
            }
        }
        return result;
    }

    static Intent launchIntent(Context c, String pkg) {
        PackageManager pm = c.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i != null) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    static String labelOf(Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }
}
