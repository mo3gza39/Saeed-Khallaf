# 🚀 How to Export and Build Your Premium Glass Keyboard APK

Since you are working directly from your mobile phone, we have configured a completely automated **GitHub Actions Workflow** in your project. This allows you to build and download your installable `.apk` file without needing a computer!

---

## Method 1: The Automated Mobile-Only Way (GitHub Actions) 📱

This is the easiest option for mobile-only users.

### Step 1: Push Project to GitHub
1. Look at your **Google AI Studio** interface (usually in the sidebar, project menu, or settings).
2. Click the **"Push to GitHub"** button (or "Export to GitHub").
3. Connect your GitHub account and create a new repository for your project (e.g. `premium-glass-keyboard`).

### Step 2: Download Your Built APK Automatically
Once the project is on GitHub:
1. Open your repository on your phone's browser or the GitHub mobile app.
2. Go to the **Actions** tab at the top of your repository page.
3. You will see a running workflow named **"Build Android APK"**.
4. Wait about 1.5 to 2 minutes for it to complete (it will show a green checkmark ✅).
5. Click on the completed run (e.g., *"Build Debug APK"* or your commit message).
6. Scroll down to the **Artifacts** section at the bottom.
7. Click on **`glass-keyboard-debug-apk`** to download the ZIP file.
8. Extract the ZIP file on your phone and tap the **`app-debug.apk`** to install your custom keyboard!

---

## Method 2: Download Source Code ZIP 📦

If you want to save the raw source files directly to your phone:
1. In **Google AI Studio**, open the main menu or settings panel.
2. Select **"Export as ZIP"** (or "Download ZIP") to get all files.
3. You can store this ZIP as a backup of your entire project codebase.

---

## Method 3: Opening and Building in Android Studio (PC/Mac) 💻

If you ever get access to a laptop or computer, you can build it manually:

### Step 1: Prepare the Project
1. Extract your downloaded ZIP or clone the GitHub repository.
2. Open **Android Studio**.
3. Select **"Open an Existing Project"** and choose the extracted/cloned folder.
4. Let Gradle sync and download dependencies automatically.

### Step 2: Build the Installable APK
1. In the top toolbar of Android Studio, click **Build**.
2. Select **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
3. Once the build finishes, a popup bubble will appear in the bottom-right corner. Click **"locate"** to find your installable `app-debug.apk` file!
