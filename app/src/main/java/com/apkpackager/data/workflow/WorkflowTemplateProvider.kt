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
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - name: Install dependencies
        run: npm ci
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
      - name: Build debug APK
        run: |
          cd android
          ./gradlew assembleDebug --no-daemon
        env:
          GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx4g"
      - uses: actions/upload-artifact@v4
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
      - uses: actions/checkout@v4
      - uses: subosito/flutter-action@v2
        with:
          flutter-version: 'stable'
          cache: true
      - name: Get dependencies
        run: flutter pub get
      - name: Build debug APK
        run: flutter build apk --debug
      - uses: actions/upload-artifact@v4
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
      - uses: actions/checkout@v4
      - name: Detect Godot version from project.godot
        run: |
          if [ ! -f project.godot ]; then
            echo "project.godot not found at repo root" >&2
            exit 1
          fi
          GODOT_VERSION=${'$'}(sed -n 's/.*config\/features=PackedStringArray("\([0-9][0-9]*\.[0-9][0-9]*\).*/\1/p' project.godot | head -n1)
          if [ -z "${'$'}GODOT_VERSION" ]; then
            echo "Could not parse Godot version from project.godot (expected a config/features line like: config/features=PackedStringArray(\"4.3\", ...))" >&2
            exit 1
          fi
          echo "Detected Godot version: ${'$'}GODOT_VERSION"
          echo "GODOT_VERSION=${'$'}GODOT_VERSION" >> "${'$'}GITHUB_ENV"
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
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
      - name: Start ADB server
        run: adb start-server
      - name: Configure Godot editor settings
        run: |
          mkdir -p "${'$'}HOME/.config/godot"
          cat > "${'$'}HOME/.config/godot/editor_settings-4.tres" <<EOF
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
          advanced_options=false
          dedicated_server=false
          custom_features=""
          export_filter="all_resources"
          include_filter=""
          exclude_filter=""
          export_path=""
          encryption_include_filters=""
          encryption_exclude_filters=""
          seed=0
          encrypt_pck=false
          encrypt_directory=false
          script_export_mode=2

          [preset.0.options]

          custom_template/debug=""
          custom_template/release=""
          gradle_build/use_gradle_build=false
          gradle_build/gradle_build_directory=""
          gradle_build/android_source_template=""
          gradle_build/compress_native_libraries=false
          gradle_build/export_format=0
          gradle_build/min_sdk=""
          gradle_build/target_sdk=""
          architectures/armeabi-v7a=true
          architectures/arm64-v8a=true
          architectures/x86=false
          architectures/x86_64=false
          version/code=1
          version/name="1.0"
          package/unique_name="org.godotengine.${'$'}genname"
          package/name=""
          package/signed=true
          package/app_category=2
          package/retain_data_on_uninstall=false
          package/exclude_from_recents=false
          package/show_in_android_tv=false
          package/show_in_app_library=true
          package/show_as_launcher_app=false
          launcher_icons/main_192x192=""
          launcher_icons/adaptive_foreground_432x432=""
          launcher_icons/adaptive_background_432x432=""
          launcher_icons/adaptive_monochrome_432x432=""
          graphics/opengl_debug=false
          xr_features/xr_mode=0
          screen/immersive_mode=true
          screen/support_small=true
          screen/support_normal=true
          screen/support_large=true
          screen/support_xlarge=true
          user_data_backup/allow=false
          command_line/extra_args=""
          apk_expansion/enable=false
          apk_expansion/SALT=""
          apk_expansion/public_key=""
          permissions/custom_permissions=PackedStringArray()
          PRESET
          fi
      - name: Import project
        run: godot --headless --import || true
      - name: Export debug APK
        run: |
          mkdir -p build
          godot --headless --export-debug "Android" build/app-debug.apk
      - uses: actions/upload-artifact@v4
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
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
      - name: Build debug APK
        run: ./gradlew assembleDebug --no-daemon
        env:
          GRADLE_OPTS: "-Dorg.gradle.jvmargs=-Xmx4g"
      - uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 7
""".trimIndent()
}
