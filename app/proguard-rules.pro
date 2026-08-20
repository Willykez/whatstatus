# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line-number info for readable stack traces in crash reports,
# without exposing full source file paths.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- WaStatus-specific rules ---

# WorkManager instantiates Workers by class name via reflection
# (Class.forName), which the manifest-derived auto-keep rules do NOT cover
# (unlike Activities/Services/Receivers, Workers aren't manifest-declared).
# Without this, AutoSaveWorker would crash at runtime in a minified release
# build with a ClassNotFoundException / NoSuchMethodException.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Coil, Media3, DataStore, and AndroidX core libraries all ship their own
# consumer ProGuard rules bundled in their AARs, which R8 applies
# automatically — no manual rules needed for them here.
