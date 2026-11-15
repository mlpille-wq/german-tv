package au.ludger.germantv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;

/**
 * Talks to "OpenVPN for Android" (de.blinkt.openvpn), which offers documented
 * activities that other apps may start to connect or disconnect a named profile.
 */
final class Vpn {
    static final String OPENVPN_PKG = "de.blinkt.openvpn";
    private static final String CONNECT_CLASS = "de.blinkt.openvpn.api.ConnectVPN";
    private static final String DISCONNECT_CLASS = "de.blinkt.openvpn.api.DisconnectVPN";
    private static final String EXTRA_PROFILE = "de.blinkt.openvpn.api.profileName";

    private Vpn() {
    }

    static boolean isOpenVpnInstalled(Context c) {
        try {
            c.getPackageManager().getPackageInfo(OPENVPN_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static Intent connectIntent(String profile) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setClassName(OPENVPN_PKG, CONNECT_CLASS);
        i.putExtra(EXTRA_PROFILE, profile);
        return i;
    }

    static Intent disconnectIntent() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setClassName(OPENVPN_PKG, DISCONNECT_CLASS);
        return i;
    }

    /** True when any VPN network is up on the TV. */
    static boolean isVpnActive(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network active = cm.getActiveNetwork();
        if (hasVpn(cm, active)) return true;
        Network[] all = cm.getAllNetworks();
        if (all != null) {
            for (Network n : all) {
                if (hasVpn(cm, n)) return true;
            }
        }
        return false;
    }

    private static boolean hasVpn(ConnectivityManager cm, Network n) {
        if (n == null) return false;
        NetworkCapabilities nc = cm.getNetworkCapabilities(n);
        return nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
    }

    /** True when the TV has a normal internet connection (Wi-Fi or Ethernet). */
    @SuppressWarnings("deprecation")
    static boolean hasNetwork(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }
}
