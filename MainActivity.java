package au.ludger.germantv;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/** Home screen: a tile for each German TV app, the VPN status, and the VPN / info / settings buttons. */
public class MainActivity extends Activity {
    private static final int TILES_PER_ROW = 4;
    private static final long STATUS_REFRESH_MS = 2000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private TextView status;
    private LinearLayout tiles;
    private Button vpnButton;
    private boolean lastVpnState;

    private final Runnable refresher = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            handler.postDelayed(this, STATUS_REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = Ui.dp(this, 56);
        int padV = Ui.dp(this, 36);
        root.setPadding(padH, padV, padH, padV);
        scroll.addView(root);

        TextView title = Ui.heading(this, "German TV", 34);
        root.addView(title, Ui.matchWidth(this, 4));

        status = Ui.text(this, "", 20, Ui.MUTED);
        root.addView(status, Ui.matchWidth(this, 28));

        tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.VERTICAL);
        root.addView(tiles, Ui.matchWidth(this, 28));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(bar, Ui.matchWidth(this, 0));

        vpnButton = Ui.button(this, "VPN on");
        vpnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVpn();
            }
        });
        bar.addView(vpnButton, Ui.wrap(this, 16, 0));

        Button info = Ui.button(this, "Connection info");
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new android.content.Intent(MainActivity.this, InfoActivity.class));
            }
        });
        bar.addView(info, Ui.wrap(this, 16, 0));

        Button settings = Ui.button(this, "Settings");
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new android.content.Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        bar.addView(settings, Ui.wrap(this, 0, 0));

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildTiles();
        updateStatus();
        AlwaysOnService.sync(this);
        handler.postDelayed(refresher, STATUS_REFRESH_MS);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(refresher);
        super.onPause();
    }

    private void buildTiles() {
        List<GermanApps.AppEntry> all = GermanApps.launchableApps(this);
        if (!prefs.appsInitialised()) prefs.saveApps(GermanApps.defaultSelection(all));
        Set<String> chosen = prefs.apps();
        List<GermanApps.AppEntry> shown = new ArrayList<>();
        for (GermanApps.AppEntry a : all) {
            if (chosen.contains(a.pkg)) shown.add(a);
        }

        tiles.removeAllViews();
        if (shown.isEmpty()) {
            TextView hint = Ui.text(this,
                    "No German TV apps selected yet. Open Settings and tick ARD, ZDF and any others.",
                    20, Ui.WARN);
            tiles.addView(hint, Ui.matchWidth(this, 0));
            return;
        }
        LinearLayout row = null;
        View first = null;
        for (int i = 0; i < shown.size(); i++) {
            if (i % TILES_PER_ROW == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                HorizontalScrollView hs = new HorizontalScrollView(this);
                hs.setClipChildren(false);
                hs.setClipToPadding(false);
                int p = Ui.dp(this, 8);
                hs.setPadding(p, p, p, p);
                hs.addView(row);
                tiles.addView(hs, Ui.matchWidth(this, 8));
            }
            View tile = makeTile(shown.get(i));
            row.addView(tile, Ui.wrap(this, 24, 0));
            if (first == null) first = tile;
        }
        if (first != null) first.requestFocus();
    }

    private View makeTile(final GermanApps.AppEntry app) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setBackground(Ui.focusBackground(this, Ui.TILE));
        int p = Ui.dp(this, 14);
        tile.setPadding(p, p, p, p);

        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams ilp;
        if (app.banner != null) {
            img.setImageDrawable(app.banner);
            ilp = new LinearLayout.LayoutParams(Ui.dp(this, 224), Ui.dp(this, 126));
        } else {
            img.setImageDrawable(app.icon);
            ilp = new LinearLayout.LayoutParams(Ui.dp(this, 224), Ui.dp(this, 126));
            int ip = Ui.dp(this, 20);
            img.setPadding(ip, ip, ip, ip);
        }
        ilp.setMargins(0, 0, 0, Ui.dp(this, 10));
        tile.addView(img, ilp);

        TextView label = Ui.text(this, app.label, 18, Ui.TEXT);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        tile.addView(label, new LinearLayout.LayoutParams(Ui.dp(this, 224), LinearLayout.LayoutParams.WRAP_CONTENT));

        Ui.zoomOnFocus(tile);
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectActivity.open(MainActivity.this, app.pkg);
            }
        });
        return tile;
    }

    private void toggleVpn() {
        if (Vpn.isVpnActive(this)) {
            VpnActionActivity.disconnect(this);
            status.setText("○  Switching the VPN off…");
        } else {
            ConnectActivity.open(this, null);
        }
    }

    private void updateStatus() {
        boolean active = Vpn.isVpnActive(this);
        if (active && prefs.connectedSince() == 0) prefs.setConnected(prefs.activeProfile(), System.currentTimeMillis());
        if (!active && lastVpnState) {
            prefs.clearConnection();
            WatchService.stop(this);
        }
        lastVpnState = active;

        if (!Vpn.isOpenVpnInstalled(this)) {
            status.setText("⚠  OpenVPN for Android is not installed. See the setup guide.");
            status.setTextColor(Ui.WARN);
        } else if (active) {
            StringBuilder sb = new StringBuilder("●  VPN on");
            String profile = prefs.activeProfile();
            if (!profile.isEmpty()) sb.append("  ·  ").append(profile);
            long since = prefs.connectedSince();
            if (since > 0) sb.append("  ·  since ").append(DateFormat.getTimeFormat(this).format(new Date(since)));
            if (prefs.alwaysOn()) {
                sb.append("  ·  kept on");
            } else if (prefs.autoOffMinutes() > 0) {
                sb.append("  ·  auto-off ").append(prefs.autoOffMinutes()).append(" min after leaving");
            }
            status.setText(sb.toString());
            status.setTextColor(Ui.OK);
        } else {
            status.setText("○  VPN off  ·  choose a channel to connect to Germany automatically");
            status.setTextColor(Ui.MUTED);
        }
        vpnButton.setText(active ? "VPN off" : "VPN on");
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
