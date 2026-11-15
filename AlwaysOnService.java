package au.ludger.germantv;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * "Keep VPN on" mode: connects the German profile when the TV starts or wakes from
 * standby, if it isn't connected already. Only the apps allowed in the OpenVPN profile
 * use the VPN, so everything else on the TV is unaffected.
 */
public class AlwaysOnService extends Service {
    private static final int NOTIFICATION_ID = 2;
    private static final long FIRST_CHECK_MS = 15000;
    private static final int MAX_TRIES = 3;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private int tries;
    private boolean receiverRegistered;

    private final Runnable check = new Runnable() {
        @Override
        public void run() {
            ensureConnected();
        }
    };

    private final BroadcastReceiver screenOn = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tries = 0;
            handler.removeCallbacks(check);
            handler.postDelayed(check, FIRST_CHECK_MS);
        }
    };

    /** Starts or stops this service to match the "keep VPN on" setting. */
    static void sync(Context c) {
        Intent i = new Intent(c, AlwaysOnService.class);
        if (new Prefs(c).alwaysOn()) {
            c.startForegroundService(i);
        } else {
            c.stopService(i);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new Prefs(this);
        registerReceiver(screenOn, new IntentFilter(Intent.ACTION_SCREEN_ON));
        receiverRegistered = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, Notifs.status(this, "Keeping the VPN on for the German TV apps."));
        if (!prefs.alwaysOn()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        tries = 0;
        handler.removeCallbacks(check);
        handler.postDelayed(check, FIRST_CHECK_MS);
        return START_STICKY;
    }

    private void ensureConnected() {
        if (!prefs.alwaysOn()) {
            stopSelf();
            return;
        }
        if (Vpn.isVpnActive(this)) {
            if (prefs.connectedSince() == 0) prefs.setConnected(prefs.activeProfile(), System.currentTimeMillis());
            return;
        }
        if (!Vpn.hasNetwork(this) || tries >= MAX_TRIES) {
            if (tries < MAX_TRIES) handler.postDelayed(check, 30000);
            return;
        }
        // First two tries use the main profile, the last one the backup (if configured).
        String profile = prefs.profile1();
        if (tries == MAX_TRIES - 1 && !prefs.profile2().isEmpty()) profile = prefs.profile2();
        tries++;
        prefs.setConnected(profile, 0);
        VpnActionActivity.connect(this, profile);
        handler.postDelayed(check, 45000);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (receiverRegistered) {
            unregisterReceiver(screenOn);
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
