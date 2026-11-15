package au.ludger.germantv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * "Connecting to Germany…" screen. Makes sure the VPN is up (main profile first, then the
 * backup profile), then opens the chosen app (ARD, ZDF, ...) and starts auto-off monitoring.
 */
public class ConnectActivity extends Activity {
    private static final String EXTRA_TARGET = "target";
    private static final String EXTRA_PROFILE = "profile";
    private static final int REQ_CONNECT = 1;
    private static final long TIMEOUT_MS = 30000;
    private static final long POLL_MS = 500;
    private static final long SWITCH_SETTLE_MS = 8000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private String target;
    private String forcedProfile;
    private final List<String> queue = new ArrayList<>();
    private int attempt;
    private long attemptStart;
    private boolean activeAtStart;
    private boolean sawDown;
    private boolean finished;

    private TextView title;
    private TextView detail;
    private ProgressBar progress;
    private LinearLayout buttons;

    /** Connect if needed, then open {@code targetPackage} (or just connect if it is null). */
    static void open(Context c, String targetPackage) {
        Intent i = new Intent(c, ConnectActivity.class);
        if (targetPackage != null) i.putExtra(EXTRA_TARGET, targetPackage);
        c.startActivity(i);
    }

    /** Switch to a specific profile (used by "Switch to backup server"). */
    static void switchTo(Context c, String profile) {
        Intent i = new Intent(c, ConnectActivity.class);
        i.putExtra(EXTRA_PROFILE, profile);
        c.startActivity(i);
    }

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            if (finished) return;
            boolean active = Vpn.isVpnActive(ConnectActivity.this);
            long elapsed = SystemClock.elapsedRealtime() - attemptStart;
            boolean connected;
            if (activeAtStart && !sawDown) {
                // Switching servers: wait for the old connection to drop and the new one to come up.
                if (!active) sawDown = true;
                connected = active && elapsed > SWITCH_SETTLE_MS;
            } else {
                connected = active;
            }
            if (connected) {
                onConnected(queue.get(attempt), true);
                return;
            }
            if (elapsed > TIMEOUT_MS) {
                if (attempt + 1 < queue.size()) {
                    startAttempt(attempt + 1);
                } else {
                    showError("Could not connect to Germany",
                            "No VPN connection after " + (TIMEOUT_MS / 1000) + " seconds.\n\n"
                                    + "Check that the profile names in Settings match the profiles in "
                                    + "OpenVPN for Android exactly, and that the ExpressVPN username and "
                                    + "password are saved in the profile.");
                }
                return;
            }
            handler.postDelayed(this, POLL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        target = getIntent().getStringExtra(EXTRA_TARGET);
        forcedProfile = getIntent().getStringExtra(EXTRA_PROFILE);
        buildUi();

        if (!Vpn.isOpenVpnInstalled(this)) {
            showError("OpenVPN for Android is not installed",
                    "Install \"OpenVPN for Android\" (by Arne Schwabe) from the Play Store and import your "
                            + "ExpressVPN Germany profile(s). The setup guide explains each step.");
            return;
        }
        if (forcedProfile == null && Vpn.isVpnActive(this)) {
            onConnected(prefs.activeProfile(), false);
            return;
        }
        if (forcedProfile != null) {
            queue.add(forcedProfile);
        } else {
            queue.add(prefs.profile1());
            String backup = prefs.profile2();
            if (!backup.isEmpty() && !backup.equals(prefs.profile1())) queue.add(backup);
        }
        startAttempt(0);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Ui.BG);
        int pad = Ui.dp(this, 64);
        root.setPadding(pad, pad, pad, pad);

        String appName = target == null ? null : GermanApps.labelOf(this, target);
        title = Ui.heading(this, "Connecting to Germany…", 32);
        title.setGravity(Gravity.CENTER);
        root.addView(title, Ui.matchWidth(this, 12));

        detail = Ui.text(this, appName == null ? "" : "Then opening " + appName, 20, Ui.MUTED);
        detail.setGravity(Gravity.CENTER);
        root.addView(detail, Ui.matchWidth(this, 32));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 72));
        plp.setMargins(0, 0, 0, Ui.dp(this, 32));
        root.addView(progress, plp);

        buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        root.addView(buttons, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        showButtons(false);

        setContentView(root);
    }

    private void startAttempt(int index) {
        attempt = index;
        finished = false;
        String profile = queue.get(index);
        title.setText(index == 0 ? "Connecting to Germany…" : "Trying the backup server…");
        String appName = target == null ? null : GermanApps.labelOf(this, target);
        detail.setText("Server profile: " + profile + (appName == null ? "" : "\nThen opening " + appName));
        progress.setVisibility(View.VISIBLE);
        showButtons(false);

        activeAtStart = Vpn.isVpnActive(this);
        sawDown = false;
        prefs.setConnected(profile, 0);
        try {
            // startActivityForResult lets OpenVPN see which app is asking (needed for its permission check).
            startActivityForResult(Vpn.connectIntent(profile), REQ_CONNECT);
        } catch (Exception e) {
            showError("Could not start OpenVPN for Android", String.valueOf(e.getMessage()));
            return;
        }
        attemptStart = SystemClock.elapsedRealtime();
        handler.removeCallbacks(poll);
        handler.postDelayed(poll, POLL_MS);
    }

    private void onConnected(String profile, boolean fresh) {
        finished = true;
        handler.removeCallbacksAndMessages(null);
        if (fresh || prefs.connectedSince() == 0) {
            prefs.setConnected(profile, System.currentTimeMillis());
        }
        WatchService.startIfNeeded(this, target);
        if (target != null) {
            launchTarget();
        } else {
            Toast.makeText(this, "VPN connected to Germany", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void launchTarget() {
        Intent i = GermanApps.launchIntent(this, target);
        if (i == null) {
            Toast.makeText(this, "That app is no longer installed", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(i);
    }

    private void showError(String heading, String message) {
        finished = true;
        handler.removeCallbacks(poll);
        title.setText(heading);
        title.setTextColor(Ui.ERROR);
        detail.setText(message);
        progress.setVisibility(View.GONE);
        showButtons(true);
    }

    private void showButtons(boolean error) {
        buttons.removeAllViews();
        if (error) {
            if (Vpn.isOpenVpnInstalled(this) && !queue.isEmpty()) {
                Button retry = Ui.button(this, "Try again");
                retry.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        title.setTextColor(Ui.TEXT);
                        startAttempt(0);
                    }
                });
                buttons.addView(retry, Ui.wrap(this, 16, 0));
                retry.requestFocus();
            }
            if (target != null) {
                Button anyway = Ui.button(this, "Open without VPN");
                anyway.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prefs.clearConnection();
                        launchTarget();
                        finish();
                    }
                });
                buttons.addView(anyway, Ui.wrap(this, 16, 0));
            }
        }
        Button cancel = Ui.button(this, error ? "Close" : "Cancel");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finished = true;
                finish();
            }
        });
        buttons.addView(cancel, Ui.wrap(this, 0, 0));
        if (!error || buttons.getChildCount() == 1) cancel.requestFocus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Nothing to do: the connection state is detected by polling.
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
