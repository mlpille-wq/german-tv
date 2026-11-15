# German TV: ARD and ZDF with automatic VPN

An app for the Sony Bravia KD-55A8G (Android TV 9). Press **ARD** or **ZDF** and the app:

1. connects the VPN to Germany through **OpenVPN for Android** using your ExpressVPN account,
2. tries the backup German server if the first one doesn't connect within 30 seconds,
3. opens the channel app,
4. switches the VPN off again a few minutes after you leave the German apps, or when the TV goes to standby.

The home screen also has tiles for any other German channel apps you choose (arte, 3sat, DW, …), a **VPN on/off** button, a **Connection info** screen (server, connection time, public IP and location) and **Settings**. In Settings you can switch on "Keep the VPN on all the time" instead of auto-off.

Setup has four parts and takes about 30–40 minutes the first time. You only do it once.

---

## Part A: let GitHub build the app (on your PC, about 10 minutes)

GitHub compiles the code in its cloud for free and gives you a download link for the TV.

1. Go to **github.com** and sign up for a free account, or sign in.
2. Click **+** (top right) → **New repository**.
   - Repository name: `german-tv`
   - Choose **Public**, so the TV can download the app without signing in. The project contains no passwords or personal data.
   - Leave everything else unticked and click **Create repository**.
3. On the new, empty repository page, click the link **uploading an existing file**.
4. Unzip `GermanTV.zip` on your PC and open the `GermanTV` folder. Select **everything inside it**, including the `.github` folder, and drag it into the browser window. Then click **Commit changes**.
   - Check: the repository's main page should now list `.github`, `app`, `build.gradle`, `settings.gradle`, `README.md` and so on at the top level. If you see a single `GermanTV` folder instead, you dragged the folder rather than its contents: delete it and repeat step 4.
5. Click the **Actions** tab. A build called "Build German TV app" starts automatically and takes about 3–5 minutes. A green tick means it's finished. If GitHub asks you to enable workflows, click the green button to enable them.
6. Your download link is now:

   `https://github.com/YOUR-GITHUB-NAME/german-tv/releases/latest/download/GermanTV.apk`

   Write it down. You'll type it on the TV in Part C.

## Part B: get the German server files from ExpressVPN (on your PC, about 5 minutes)

1. Sign in at expressvpn.com → **Set up apps** → **Advanced setup** → **Manual configuration** → **OpenVPN**.
2. Note the **username** and **password** shown there. These are not your normal ExpressVPN login.
3. Click two different **Germany** locations to download their `.ovpn` files, for example Germany – Frankfurt and Germany – Nuremberg.
4. Copy both `.ovpn` files onto a USB stick.

## Part C: set up the TV (about 15 minutes)

### C1. Install the two helper apps from the Play Store
- **OpenVPN for Android** by Arne Schwabe. This is the free, open-source one. Don't install "OpenVPN Connect", which is a different app.
- **Downloader** by AFTVnews. You'll use it to install the German TV app.

### C2. Create the two German profiles in OpenVPN for Android
1. Plug the USB stick into the TV and open **OpenVPN for Android**.
2. Choose **+** → **Import** and pick the first `.ovpn` file from the USB stick.
3. Rename the profile to exactly **`Germany 1`**, with the capital G and a space before the 1.
4. In the profile's **Basic** settings, enter the ExpressVPN manual-configuration **username** and **password**, and choose to save the password.
5. Repeat for the second file and name it exactly **`Germany 2`**.
6. Select **Germany 1** once to test it. Android asks to allow a VPN connection: choose **OK**. When it says connected, disconnect again.

(You'll come back to these profiles in C5 to set *Allowed apps*.)

### C3. Allow installing the app
1. **Settings → Device Preferences → About**, scroll to **Build** and press it **7 times** until it says you're a developer.
2. **Settings → Device Preferences → Security & restrictions → Unknown sources**: switch **Downloader** on.

(Menu names on Sony TVs can differ slightly. Look for the nearest match.)

### C4. Install German TV
1. Open **Downloader**, type your link from Part A step 6, and press **Go**.
2. When the download finishes, choose **Install**, then **Done**. You can delete the downloaded file when Downloader offers.
3. **German TV** now appears in your apps. You can add it to the home screen favourites with a long press.

### C5. Choose which apps use the VPN (important)
In **OpenVPN for Android**, edit **Germany 1** → **Allowed apps**:
- choose the option to use the VPN **only for the selected apps**,
- tick **ARD Mediathek**, **ZDF**, any other German channel apps, and **German TV**.

Do the same for **Germany 2**. This keeps Netflix, YouTube and everything else on your normal Australian connection even while the VPN is on. Ticking German TV lets the app detect the VPN and show the German location on its info screen.

### C6. Allow auto-off (recommended)
**Settings → Apps → Special app access → Usage access → German TV → On.**

This lets the app see when you've left ARD or ZDF. Without it, the VPN still switches off when the TV goes to standby, and you can always use the **VPN off** button.

If your TV has no Usage access page, it can be granted once from a computer with this command (ask Claude to walk you through it):
`adb shell appops set au.ludger.germantv GET_USAGE_STATS allow`

## Part D: first use
1. Open **German TV**. ARD and ZDF should already have tiles. If not, go to **Settings**, tick them and press **Save**.
2. Press **ARD**. The first time, OpenVPN for Android asks whether German TV may control the VPN: allow it, and tick "remember" if it's offered. After that it's one button press.
3. Check that it works with **Connection info**: it should show *VPN on* and a location in Germany.

---

## Everyday use
- **ARD / ZDF / other tiles:** connect to Germany (if not already connected), then open the app.
- **VPN on / VPN off:** switch the VPN manually.
- **Connection info:** server, connection time, public IP and location. *Switch to Germany 2* changes server if ARD or ZDF says a programme isn't available in your region.
- **Settings:** profile names, auto-off minutes (default 5, 0 = never), *Keep the VPN on all the time*, and which apps get a tile.

## If something doesn't work
| Problem | What to do |
|---|---|
| "Could not connect to Germany" | Check that the profile names in Settings exactly match the names in OpenVPN for Android, and that the password is saved in both profiles. Try connecting the profile directly in OpenVPN for Android to see its error. |
| Nothing happens when pressing a tile the first time | Open OpenVPN for Android and look for a prompt or setting allowing other apps (remote control / external apps), and allow German TV. |
| ARD/ZDF says "not available in your country" | Connection info → *Switch to Germany 2*. If both are blocked, download a different Germany `.ovpn` file and import it under the same name. |
| Netflix or other apps behave oddly while the VPN is on | Check *Allowed apps* in both OpenVPN profiles (step C5). |
| VPN doesn't switch off by itself | Grant Usage access (step C6) and check auto-off minutes in Settings is not 0. |

## Updating the app later
Change files in the GitHub repository (or upload new ones). GitHub rebuilds automatically, and the same Downloader link then installs the new version over the old one, keeping your settings.

## How it works (technical)
- The app controls OpenVPN for Android through its documented remote-control activities (`de.blinkt.openvpn.api.ConnectVPN` / `DisconnectVPN`, with the profile name as `de.blinkt.openvpn.api.profileName`).
- VPN state is detected from Android's network list (a network with the VPN transport).
- Auto-off reads the foreground app from Android's usage statistics every 15 seconds.
- The public location comes from ipinfo.io.
- No passwords are stored in this app. They stay in OpenVPN for Android on the TV.
