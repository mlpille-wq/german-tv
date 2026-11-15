package au.ludger.germantv;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/** Looks up this app's public IP address and its location (ipinfo.io, no account needed). */
final class GeoInfo {
    String ip = "";
    String city = "";
    String region = "";
    String country = "";
    String org = "";

    /** Blocking network call: never call on the main thread. */
    static GeoInfo fetch() throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL("https://ipinfo.io/json").openConnection();
        con.setConnectTimeout(8000);
        con.setReadTimeout(8000);
        con.setRequestProperty("Accept", "application/json");
        try {
            InputStream in = con.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            JSONObject j = new JSONObject(out.toString("UTF-8"));
            GeoInfo g = new GeoInfo();
            g.ip = j.optString("ip", "");
            g.city = j.optString("city", "");
            g.region = j.optString("region", "");
            g.country = j.optString("country", "");
            g.org = j.optString("org", "");
            return g;
        } finally {
            con.disconnect();
        }
    }

    boolean isGermany() {
        return "DE".equalsIgnoreCase(country);
    }

    String countryName() {
        if (country.isEmpty()) return "";
        return new Locale("", country).getDisplayCountry(Locale.ENGLISH);
    }

    String place() {
        StringBuilder sb = new StringBuilder();
        if (!city.isEmpty()) sb.append(city);
        if (!region.isEmpty() && !region.equals(city)) sb.append(sb.length() > 0 ? ", " : "").append(region);
        String cn = countryName();
        if (!cn.isEmpty()) sb.append(sb.length() > 0 ? ", " : "").append(cn);
        return sb.toString();
    }
}
