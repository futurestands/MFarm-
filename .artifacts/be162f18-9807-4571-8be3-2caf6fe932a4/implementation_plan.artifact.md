# Implementation Plan: Migrate to AndroidX

This plan outlines the steps required to migrate the "DailyFarm" project from the deprecated Android Support Library to AndroidX. This modernization is necessary to use current Android features and libraries.

## Proposed Changes

### Build System & Configuration

#### [MODIFY] [gradle-wrapper.properties](file:///C:/Users/MJ/Downloads/DailyFarm-master/gradle/wrapper/gradle-wrapper.properties)
- Upgrade Gradle version from `2.10` to `8.0` to support modern Android Gradle Plugins.

#### [MODIFY] [gradle.properties](file:///C:/Users/MJ/Downloads/DailyFarm-master/gradle.properties)
- Add `android.useAndroidX=true` and `android.enableJetifier=true`.

#### [MODIFY] [build.gradle (root)](file:///C:/Users/MJ/Downloads/DailyFarm-master/build.gradle)
- Update Android Gradle Plugin to `8.0.0`.
- Update repositories to use `google()` and `mavenCentral()`.

#### [MODIFY] [build.gradle (app)](file:///C:/Users/MJ/Downloads/DailyFarm-master/app/build.gradle)
- Update `compileSdkVersion` and `targetSdkVersion` to `33`.
- Replace `com.android.support` dependencies with `androidx` equivalents (e.g., `androidx.appcompat:appcompat`, `com.google.android.material:material`).

### Source Code Refactoring

#### [MODIFY] All Java Files
- Update imports from `android.support.*` to `androidx.*`. For example:
    - `android.support.v7.app.AppCompatActivity` -> `androidx.appcompat.app.AppCompatActivity`
    - `android.support.design.widget.NavigationView` -> `com.google.android.material.navigation.NavigationView`

#### [MODIFY] Layout XML Files
- Update tag names for support library components. For example:
    - `android.support.v4.widget.DrawerLayout` -> `androidx.drawerlayout.widget.DrawerLayout`
    - `android.support.design.widget.NavigationView` -> `com.google.android.material.navigation.NavigationView`

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify that the project builds successfully with AndroidX.

### Manual Verification
- Deploy the app to a modern emulator/device.
- Verify that the Navigation Drawer and main lists (Animal List, Milk Production) function correctly.
- Ensure the database initialization still works as expected.
