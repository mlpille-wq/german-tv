package au.ludger.germantv;

import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Date;

/** Connection info: VPN state, server profile, public IP and location, auto-off status. */
public class InfoActivity extends Activity {
    private LinearLayout rows;
    private LinearLayout buttons;
    private TextView geoLine;
    private TextView geoHint;
    private volatile boolean alive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = Ui.dp(this, 56);
        int padV = Ui.dp(this, 36);
        root.setPadding(padH, padV, padH, padV);
        scroll.addView(root);

        root.addView(Ui.heading(this, "Connection info", 32), Ui.matchWidth(this, 20));

        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setBackground(Ui.panel(this));
        int p = Ui.dp(this, 24);
        rows.setPadding(p, p, p, p);
        root.addView(rows, Ui.matchWidth(this, 24));

        buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(buttons, Ui.matchWidth(this, 0));

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        alive = true;
        render();
    }

    @Override
    protected void onPause() {
        alive = false;
        super.onPause();
    }

    private void render() {
        final Prefs prefs = new Prefs(this);
        final boolean active = Vpn.isVpnActive(this);
        rows.removeAllViews();

        addRow("VPN", active ? "On" : "Off", active ? Ui.OK : Ui.MUTED);
        if (active) {
            String profile = prefs.activeProfile();
            addRow("Server profile", profile.isEmpty() ? "(started outside this app)" : profile, Ui.TEXT);
            long since = prefs.connectedSince();
            if (since > 0) {
                long mins = (System.currentTimeMillis() - since) / 60000L;
                String dur = mins < 60 ? mins + " min" : (mins / 60) + " h " + (mins % 60) + " min";
                addRow("Connected since", DateFormat.getTimeFormat(this).format(new Date(since)) + "  (" + dur + ")", Ui.TEXT);
            }
        }
        geoLine = addRow("Public location", "Checking…", Ui.MUTED);
        geoHint = Ui.text(this, "", 16, Ui.WARN);
        geoHint.setVisibility(View.GONE);
        rows.addView(geoHint, Ui.matchWidth(this, 12));

        String autoOff;
        if (prefs.alwaysOn()) {
            autoOff = "Off: VPN is kept on (Settings)";
        } else if (prefs.autoOffMinutes() <= 0) {
            autoOff = "Off: switch the VPN off yourself";
        } else if (UsageAccess.granted(this)) {
            autoOff = prefs.autoOffMinutes() + " min after you leave the German TV apps, or at standby";
        } else {
            autoOff = "Only at standby: grant Usage access for the timer";
        }
        addRow("Auto-off", autoOff, Ui.TEXT);
        addRow("Main / backup profile", prefs.profile1() + "  /  "
                + (prefs.profile2().isEmpty() ? "none" : prefs.profile2()), Ui.TEXT);
        addRow("OpenVPN for Android", Vpn.isOpenVpnInstalled(this) ? "Installed" : "Not installed",
                Vpn.isOpenVpnInstalled(this) ? Ui.TEXT : Ui.WARN);

        buildButtons(prefs, active);
        lookUpLocation(active);
    }

    private TextView addRow(String label, String value, int valueColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView l = Ui.text(this, label, 20, Ui.MUTED);
        row.addView(l, new LinearLayout.LayoutParams(Ui.dp(this, 260), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView v = Ui.text(this, value, 20, valueColor);
        row.addView(v, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        rows.addView(row, Ui.matchWidth(this, 12));
        return v;
    }

    private void buildButtons(final Prefs prefs, boolean active) {
        buttons.removeAllViews();
        Button refresh = Ui.button(this, "Refresh");
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                render();
            }
        });
        buttons.addView(refresh, Ui.wrap(this, 16, 0));
        refresh.requestFocus();

        final String other = prefs.otherProfile(prefs.activeProfile());
        if (active && !other.isEmpty()) {
            Button sw = Ui.button(this, "Switch to " + other);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConnectActivity.switchTo(InfoActivity.this, other);
                }
            });
            buttons.addView(sw, Ui.wrap(this, 16, 0));
        }

        Button vpn = Ui.button(this, active ? "VPN off" : "VPN on");
        vpn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Vpn.isVpnActive(InfoActivity.this)) {
                    VpnActionActivity.disconnect(InfoActivity.this);
                    finish();
                } else {
                    ConnectActivity.open(InfoActivity.this, null);
                }
            }
        });
        buttons.addView(vpn, Ui.wrap(this, 16, 0));

        if (!UsageAccess.granted(this) && !prefs.alwaysOn() && prefs.autoOffMinutes() > 0) {
            Button grant = Ui.button(this, "Grant usage access");
            grant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UsageAccess.openSettings(InfoActivity.this);
                }
            });
            buttons.addView(grant, Ui.wrap(this, 0, 0));
        }
    }

    private void lookUpLocation(final boolean vpnActive) {
        final TextView line = geoLine;
        final TextView hint = geoHint;
        new Thread(new Runnable() {
            @Override
            public void run() {
                GeoInfo g = null;
                String err = null;
                try {
                    g = GeoInfo.fetch();
                } catch (Exception e) {
                    err = e.getClass().getSimpleName();
                }
                final GeoInfo geo = g;
                final String error = err;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!alive || line != geoLine) return;
                        if (geo == null) {
                            line.setText("Could not check (" + error + ")");
                            line.setTextColor(Ui.MUTED);
                            return;
                        }
                        String text = geo.place();
                        if (!geo.ip.isEmpty()) text += "   ·   IP " + geo.ip;
                        if (!geo.org.isEmpty()) text += "\n" + geo.org;
                        line.setText(text);
                        line.setTextColor(geo.isGermany() ? Ui.OK : Ui.TEXT);
                        if (vpnActive && !geo.isGermany()) {
                            hint.setText("The VPN is on, but this app is not using it, so it sees your normal "
                                    + "location. That is fine if \"German TV\" is not in the profile's Allowed apps; "
                                    + "add it there if you want this screen to show the German location.");
                            hint.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }).start();
    }
}
