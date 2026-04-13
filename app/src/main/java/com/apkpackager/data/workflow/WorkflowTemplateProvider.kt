package com.apkpackager.data.workflow

enum class AppFramework { REACT_NATIVE, FLUTTER, ANDROID, GODOT }

object WorkflowTemplateProvider {

    fun getTemplate(framework: AppFramework): String = when (framework) {
        AppFramework.REACT_NATIVE -> reactNativeTemplate()
        AppFramework.FLUTTER -> flutterTemplate()
        AppFramework.ANDROID -> androidTemplate()
        AppFramework.GODOT -> godotTemplate()
    }

    private fun reactNativeTemplate() = """
name: Build APK
on:
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-node@v6
        with:
          node-version: '20'
          cache: 'npm'
      - name: Install dependencies
        run: npm ci
      - uses: actions/setup-java@v5
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Setup Android SDK
        uses: android-actions/setup-android@v4
      - name: Build debug APK
        run: |
          cd android
          ./gradlew assembleDebug --no-daemon
        env:
          GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx4g"
      - uses: actions/upload-artifact@v7
        with:
          name: app-debug
          path: android/app/build/outputs/apk/debug/*.apk
          retention-days: 7
""".trimIndent()

    private fun flutterTemplate() = """
name: Build APK
on:
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - uses: subosito/flutter-action@v2
        with:
          flutter-version: 'stable'
          cache: true
      - name: Get dependencies
        run: flutter pub get
      - name: Build debug APK
        run: flutter build apk --debug
      - uses: actions/upload-artifact@v7
        with:
          name: app-debug
          path: build/app/outputs/flutter-apk/app-debug.apk
          retention-days: 7
""".trimIndent()

    private fun godotTemplate() = """
name: Build APK
on:
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - name: Detect Godot version from project.godot
        run: |
          GODOT_VERSION=${'$'}(sed -n 's/.*config\/features=PackedStringArray("\([0-9][0-9]*\.[0-9][0-9]*\).*/\1/p' project.godot | head -n1)
          if [ -z "${'$'}GODOT_VERSION" ]; then
            echo "Could not parse Godot version from project.godot" >&2
            exit 1
          fi
          echo "Detected Godot version: ${'$'}GODOT_VERSION"
          echo "GODOT_VERSION=${'$'}GODOT_VERSION" >> "${'$'}GITHUB_ENV"
          echo "GODOT_MAJOR=${'$'}(echo ${'$'}GODOT_VERSION | cut -d. -f1)" >> "${'$'}GITHUB_ENV"
      - uses: actions/setup-java@v5
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Setup Android SDK
        uses: android-actions/setup-android@v4
      - name: Install Godot and export templates
        run: |
          wget -q "https://github.com/godotengine/godot/releases/download/${'$'}{GODOT_VERSION}-stable/Godot_v${'$'}{GODOT_VERSION}-stable_linux.x86_64.zip"
          unzip -q "Godot_v${'$'}{GODOT_VERSION}-stable_linux.x86_64.zip"
          sudo mv "Godot_v${'$'}{GODOT_VERSION}-stable_linux.x86_64" /usr/local/bin/godot
          sudo chmod +x /usr/local/bin/godot
          wget -q "https://github.com/godotengine/godot/releases/download/${'$'}{GODOT_VERSION}-stable/Godot_v${'$'}{GODOT_VERSION}-stable_export_templates.tpz"
          mkdir -p "${'$'}HOME/.local/share/godot/export_templates/${'$'}{GODOT_VERSION}.stable"
          unzip -q "Godot_v${'$'}{GODOT_VERSION}-stable_export_templates.tpz"
          mv templates/* "${'$'}HOME/.local/share/godot/export_templates/${'$'}{GODOT_VERSION}.stable/"
      - name: Generate debug keystore
        run: |
          keytool -keyalg RSA -genkeypair -alias androiddebugkey -keypass android \
            -keystore "${'$'}HOME/debug.keystore" -storepass android \
            -dname "CN=Android Debug,O=Android,C=US" -validity 9999 -deststoretype pkcs12
      - name: Configure Godot editor settings
        run: |
          mkdir -p "${'$'}HOME/.config/godot"
          cat > "${'$'}HOME/.config/godot/editor_settings-${'$'}{GODOT_MAJOR}.tres" <<EOF
[gd_resource type="EditorSettings" format=3]
[resource]
export/android/android_sdk_path = "${'$'}{ANDROID_HOME}"
export/android/debug_keystore = "${'$'}{HOME}/debug.keystore"
export/android/debug_keystore_user = "androiddebugkey"
export/android/debug_keystore_pass = "android"
EOF
      - name: Ensure export_presets.cfg has an Android preset
        run: |
          if [ ! -f export_presets.cfg ] || ! grep -q 'platform="Android"' export_presets.cfg; then
            echo "No Android preset found — generating a default one."
            cat > export_presets.cfg <<'PRESET'
[preset.0]

name="Android"
platform="Android"
runnable=true
dedicated_server=false
custom_features=""
export_filter="all_resources"
include_filter=""
exclude_filter=""
export_path=""
encrypt_pck=false
encrypt_directory=false
script_export_mode=2

[preset.0.options]

custom_template/debug=""
custom_template/release=""
gradle_build/use_gradle_build=false
gradle_build/export_format=0
architectures/armeabi-v7a=false
architectures/arm64-v8a=true
architectures/x86=false
architectures/x86_64=false
version/code=1
version/name="1.0"
package/unique_name="com.godot.game"
package/name=""
package/signed=true
package/classify_as_game=true
package/retain_data_on_uninstall=false
package/exclude_from_recents=false
package/app_category=0
launcher_icons/main_192x192=""
launcher_icons/adaptive_foreground_432x432=""
launcher_icons/adaptive_background_432x432=""
graphics/opengl_debug=false
xr_features/xr_mode=0
screen/immersive_mode=true
screen/support_small=true
screen/support_normal=true
screen/support_large=true
screen/support_xlarge=true
permissions/custom_permissions=PackedStringArray()
permissions/internet=true
PRESET
          fi
      - name: Import project
        run: godot --headless --import || true
      - name: Export debug APK
        run: |
          mkdir -p build
          godot --headless --export-debug "Android" build/app-debug.apk
      - uses: actions/upload-artifact@v7
        with:
          name: app-debug
          path: build/app-debug.apk
          retention-days: 7
""".trimIndent()

    private fun androidTemplate() = """
name: Build APK
on:
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - uses: actions/setup-java@v5
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Setup Android SDK
        uses: android-actions/setup-android@v4
      - name: Build debug APK
        run: ./gradlew assembleDebug --no-daemon
        env:
          GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx4g"
      - uses: actions/upload-artifact@v7
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 7
""".trimIndent()
}
