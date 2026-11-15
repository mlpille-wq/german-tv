package au.ludger.germantv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** All settings, stored on the TV in the app's private preferences. */
final class Prefs {
    private static final String FILE = "settings";
    private static final String KEY_PROFILE1 = "profile1";
    private static final String KEY_PROFILE2 = "profile2";
    private static final String KEY_AUTO_OFF_MIN = "autoOffMinutes";
    private static final String KEY_ALWAYS_ON = "alwaysOn";
    private static final String KEY_APPS = "apps";
    private static final String KEY_APPS_INITIALISED = "appsInitialised";
    private static final String KEY_CONNECTED_SINCE = "connectedSince";
    private static final String KEY_ACTIVE_PROFILE = "activeProfile";

    static final String DEFAULT_PROFILE1 = "Germany 1";
    static final String DEFAULT_PROFILE2 = "Germany 2";

    private final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    String profile1() {
        return sp.getString(KEY_PROFILE1, DEFAULT_PROFILE1).trim();
    }

    /** Backup profile; empty string means "no backup". */
    String profile2() {
        return sp.getString(KEY_PROFILE2, DEFAULT_PROFILE2).trim();
    }

    /** Minutes after leaving the German apps before the VPN switches off; 0 = never. */
    int autoOffMinutes() {
        return sp.getInt(KEY_AUTO_OFF_MIN, 5);
    }

    boolean alwaysOn() {
        return sp.getBoolean(KEY_ALWAYS_ON, false);
    }

    Set<String> apps() {
        return new HashSet<>(sp.getStringSet(KEY_APPS, new HashSet<String>()));
    }

    boolean appsInitialised() {
        return sp.getBoolean(KEY_APPS_INITIALISED, false);
    }

    long connectedSince() {
        return sp.getLong(KEY_CONNECTED_SINCE, 0L);
    }

    String activeProfile() {
        return sp.getString(KEY_ACTIVE_PROFILE, "");
    }

    void saveSettings(String p1, String p2, int autoOffMinutes, boolean alwaysOn) {
        sp.edit()
                .putString(KEY_PROFILE1, p1.trim())
                .putString(KEY_PROFILE2, p2.trim())
                .putInt(KEY_AUTO_OFF_MIN, Math.max(0, autoOffMinutes))
                .putBoolean(KEY_ALWAYS_ON, alwaysOn)
                .apply();
    }

    void saveApps(Set<String> packages) {
        sp.edit()
                .putStringSet(KEY_APPS, new HashSet<>(packages))
                .putBoolean(KEY_APPS_INITIALISED, true)
                .apply();
    }

    void setConnected(String profile, long since) {
        sp.edit()
                .putString(KEY_ACTIVE_PROFILE, profile == null ? "" : profile)
                .putLong(KEY_CONNECTED_SINCE, since)
                .apply();
    }

    void clearConnection() {
        sp.edit().remove(KEY_ACTIVE_PROFILE).remove(KEY_CONNECTED_SINCE).apply();
    }

    /** The other configured profile, used for "switch server". Empty if there is none. */
    String otherProfile(String current) {
        String p1 = profile1();
        String p2 = profile2();
        if (p2.isEmpty()) return "";
        return p1.equals(current) ? p2 : p1;
    }
}
