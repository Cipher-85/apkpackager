package com.apkpackager.data.workflow

enum class AppFramework { REACT_NATIVE, FLUTTER, ANDROID, UNKNOWN }

object WorkflowTemplateProvider {

    fun getTemplate(framework: AppFramework): String = when (framework) {
        AppFramework.REACT_NATIVE -> reactNativeTemplate()
        AppFramework.FLUTTER -> flutterTemplate()
        AppFramework.ANDROID -> androidTemplate()
        AppFramework.UNKNOWN -> error("No template for unknown framework")
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
