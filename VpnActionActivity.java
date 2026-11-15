package au.ludger.germantv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Invisible helper screen. OpenVPN for Android's remote-control commands are screens
 * (activities), so background parts of this app (auto-off, keep-on) start this
 * transparent activity, which passes the command on and closes itself.
 */
public class VpnActionActivity extends Activity {
    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_PROFILE = "profile";
    private static final String ACTION_CONNECT = "connect";
    private static final String ACTION_DISCONNECT = "disconnect";
    private static final int REQ = 7;

    private final Handler handler = new Handler(Looper.getMainLooper());

    static void disconnect(Context c) {
        new Prefs(c).clearConnection();
        start(c, ACTION_DISCONNECT, null);
    }

    static void connect(Context c, String profile) {
        start(c, ACTION_CONNECT, profile);
    }

    private static void start(Context c, String action, String profile) {
        if (!Vpn.isOpenVpnInstalled(c)) return;
        Intent i = new Intent(c, VpnActionActivity.class);
        i.putExtra(EXTRA_ACTION, action);
        if (profile != null) i.putExtra(EXTRA_PROFILE, profile);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        c.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            finish();
            return;
        }
        String action = getIntent().getStringExtra(EXTRA_ACTION);
        Intent command;
        if (ACTION_CONNECT.equals(action)) {
            String profile = getIntent().getStringExtra(EXTRA_PROFILE);
            if (profile == null || profile.isEmpty()) {
                finish();
                return;
            }
            command = Vpn.connectIntent(profile);
        } else {
            command = Vpn.disconnectIntent();
            WatchService.stop(this);
        }
        try {
            // startActivityForResult lets OpenVPN see which app is asking (needed for its permission check).
            startActivityForResult(command, REQ);
        } catch (Exception e) {
            finish();
            return;
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) finish();
            }
        }, 15000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
