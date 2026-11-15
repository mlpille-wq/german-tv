package au.ludger.germantv;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Process;
import android.provider.Settings;
import android.widget.Toast;

/**
 * "Usage access" lets the app see which app is on screen, so it can tell when you
 * have left ARD/ZDF and switch the VPN off automatically.
 */
final class UsageAccess {
    private UsageAccess() {
    }

    static boolean granted(Context c) {
        AppOpsManager aom = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        if (aom == null) return false;
        int mode = aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName());
        if (mode == AppOpsManager.MODE_DEFAULT) {
            return c.checkCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS")
                    == PackageManager.PERMISSION_GRANTED;
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    static void openSettings(Activity a) {
        try {
            a.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a,
                    "This TV has no 'Usage access' settings page. See the setup guide for the one-line computer command that grants it.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
