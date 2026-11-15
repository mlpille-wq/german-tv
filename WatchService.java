package au.ludger.germantv;

import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.Set;

/**
 * Auto-off: runs while the VPN is on (unless "keep VPN on" is set). Every 15 seconds it
 * checks which app is on screen; once none of the German TV apps has been on screen for
 * the configured number of minutes, or the TV goes into standby, it switches the VPN off.
 */
public class WatchService extends Service {
    private static final String EXTRA_TARGET = "target";
    private static final int NOTIFICATION_ID = 1;
    private static final long POLL_MS = 15000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private String currentForeground;
    private long lastQuery;
    private long lastWatching;
    private boolean receiverRegistered;

    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) switchOff();
        }
    };

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            check();
        }
    };

    /** Starts auto-off monitoring if it applies to the current settings. */
    static void startIfNeeded(Context c, String targetPackage) {
        Prefs p = new Prefs(c);
        if (p.alwaysOn() || p.autoOffMinutes() <= 0) return;
        Intent i = new Intent(c, WatchService.class);
        if (targetPackage != null) i.putExtra(EXTRA_TARGET, targetPackage);
        c.startForegroundService(i);
    }

    static void stop(Context c) {
        c.stopService(new Intent(c, WatchService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new Prefs(this);
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        receiverRegistered = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int minutes = prefs.autoOffMinutes();
        startForeground(NOTIFICATION_ID, Notifs.status(this,
                "VPN on. Switches off " + minutes + " min after you leave the German TV apps."));
        long now = System.currentTimeMillis();
        String target = intent == null ? null : intent.getStringExtra(EXTRA_TARGET);
        if (target != null) currentForeground = target;
        lastWatching = now;
        if (lastQuery == 0) lastQuery = now - 5000;
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, POLL_MS);
        return START_STICKY;
    }

    private void check() {
        if (prefs.alwaysOn() || prefs.autoOffMinutes() <= 0 || !Vpn.isVpnActive(this)) {
            stopSelf();
            return;
        }
        long now = System.currentTimeMillis();
        if (UsageAccess.granted(this)) {
            updateForeground(now);
            if (isWatched(currentForeground)) lastWatching = now;
        } else {
            // Without usage access the app cannot see what is on screen: only standby switches off.
            lastWatching = now;
        }
        if (now - lastWatching >= prefs.autoOffMinutes() * 60000L) {
            switchOff();
            return;
        }
        handler.postDelayed(tick, POLL_MS);
    }

    @SuppressWarnings("deprecation")
    private void updateForeground(long now) {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return;
        UsageEvents events = usm.queryEvents(lastQuery, now);
        UsageEvents.Event e = new UsageEvents.Event();
        while (events != null && events.hasNextEvent()) {
            events.getNextEvent(e);
            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentForeground = e.getPackageName();
            }
        }
        lastQuery = now;
    }

    private boolean isWatched(String pkg) {
        if (pkg == null) return false;
        if (pkg.equals(getPackageName()) || pkg.equals(Vpn.OPENVPN_PKG)) return true;
        Set<String> apps = prefs.apps();
        return apps.contains(pkg);
    }

    private void switchOff() {
        handler.removeCallbacks(tick);
        if (Vpn.isVpnActive(this)) VpnActionActivity.disconnect(this);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (receiverRegistered) {
            unregisterReceiver(screenOff);
            receiverRegistered = false;
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
