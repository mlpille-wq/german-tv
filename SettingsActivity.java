package au.ludger.germantv;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Settings: OpenVPN profile names, auto-off, keep-on mode, and which apps appear as tiles. */
public class SettingsActivity extends Activity {
    private Prefs prefs;
    private EditText profile1;
    private EditText profile2;
    private EditText autoOff;
    private CheckBox alwaysOn;
    private final List<CheckBox> appBoxes = new ArrayList<>();
    private final List<String> appPackages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = Ui.dp(this, 56);
        int padV = Ui.dp(this, 36);
        root.setPadding(padH, padV, padH, padV);
        scroll.addView(root);

        root.addView(Ui.heading(this, "Settings", 32), Ui.matchWidth(this, 24));

        // --- VPN profiles
        root.addView(Ui.heading(this, "VPN server profiles", 22), Ui.matchWidth(this, 4));
        root.addView(Ui.text(this, "Type the names exactly as they appear in OpenVPN for Android.", 16, Ui.MUTED),
                Ui.matchWidth(this, 12));
        profile1 = field("Main profile", prefs.profile1(), InputType.TYPE_CLASS_TEXT, root);
        profile2 = field("Backup profile (leave empty for none)", prefs.profile2(), InputType.TYPE_CLASS_TEXT, root);

        // --- switching off
        root.addView(Ui.heading(this, "Switching the VPN off", 22), Ui.matchWidth(this, 12));
        autoOff = field("Minutes after leaving the German TV apps (0 = never)",
                String.valueOf(prefs.autoOffMinutes()), InputType.TYPE_CLASS_NUMBER, root);

        alwaysOn = new CheckBox(this);
        alwaysOn.setText("Keep the VPN on all the time (connects when the TV turns on; auto-off is not used)");
        alwaysOn.setTextColor(Ui.TEXT);
        alwaysOn.setTextSize(18);
        alwaysOn.setChecked(prefs.alwaysOn());
        alwaysOn.setBackground(Ui.focusBackground(this, Ui.BG));
        root.addView(alwaysOn, Ui.matchWidth(this, 12));

        boolean usage = UsageAccess.granted(this);
        LinearLayout usageRow = new LinearLayout(this);
        usageRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView usageText = Ui.text(this, usage
                ? "Usage access: granted (needed for the auto-off timer)"
                : "Usage access: NOT granted. Without it the VPN only switches off at standby.",
                16, usage ? Ui.OK : Ui.WARN);
        usageRow.addView(usageText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (!usage) {
            Button grant = Ui.button(this, "Grant usage access");
            grant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UsageAccess.openSettings(SettingsActivity.this);
                }
            });
            usageRow.addView(grant, Ui.wrap(this, 0, 0));
        }
        root.addView(usageRow, Ui.matchWidth(this, 28));

        // --- apps
        root.addView(Ui.heading(this, "Channel apps on the home screen", 22), Ui.matchWidth(this, 4));
        root.addView(Ui.text(this, "Ticked apps get a tile. Add the same apps to \"Allowed apps\" in the OpenVPN profile.",
                16, Ui.MUTED), Ui.matchWidth(this, 12));
        Set<String> chosen = prefs.apps();
        for (GermanApps.AppEntry a : GermanApps.launchableApps(this)) {
            CheckBox cb = new CheckBox(this);
            cb.setText(a.label);
            cb.setTextColor(Ui.TEXT);
            cb.setTextSize(18);
            cb.setChecked(chosen.contains(a.pkg));
            cb.setBackground(Ui.focusBackground(this, Ui.BG));
            root.addView(cb, Ui.matchWidth(this, 4));
            appBoxes.add(cb);
            appPackages.add(a.pkg);
        }

        // --- save / cancel
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams barLp = Ui.matchWidth(this, 0);
        barLp.topMargin = Ui.dp(this, 24);
        root.addView(bar, barLp);
        Button save = Ui.button(this, "Save");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        bar.addView(save, Ui.wrap(this, 16, 0));
        Button cancel = Ui.button(this, "Cancel");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        bar.addView(cancel, Ui.wrap(this, 0, 0));

        setContentView(scroll);
        profile1.requestFocus();
    }

    private EditText field(String label, String value, int inputType, LinearLayout parent) {
        parent.addView(Ui.text(this, label, 16, Ui.MUTED), Ui.matchWidth(this, 4));
        EditText e = new EditText(this);
        e.setText(value);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setTextColor(Ui.TEXT);
        e.setTextSize(20);
        e.setBackground(Ui.focusBackground(this, Ui.TILE));
        int p = Ui.dp(this, 14);
        e.setPadding(p, p, p, p);
        parent.addView(e, Ui.matchWidth(this, 16));
        return e;
    }

    private void save() {
        String p1 = profile1.getText().toString().trim();
        if (p1.isEmpty()) {
            Toast.makeText(this, "The main profile name can't be empty", Toast.LENGTH_LONG).show();
            profile1.requestFocus();
            return;
        }
        int minutes;
        try {
            minutes = Integer.parseInt(autoOff.getText().toString().trim());
        } catch (NumberFormatException e) {
            minutes = 5;
        }
        prefs.saveSettings(p1, profile2.getText().toString(), minutes, alwaysOn.isChecked());

        Set<String> apps = new HashSet<>();
        for (int i = 0; i < appBoxes.size(); i++) {
            if (appBoxes.get(i).isChecked()) apps.add(appPackages.get(i));
        }
        prefs.saveApps(apps);

        AlwaysOnService.sync(this);
        if (alwaysOn.isChecked() || minutes <= 0) WatchService.stop(this);
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
