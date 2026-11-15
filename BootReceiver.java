package au.ludger.germantv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts "keep VPN on" mode after the TV restarts, if that setting is on. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        new Prefs(context).clearConnection();
        AlwaysOnService.sync(context);
    }
}
