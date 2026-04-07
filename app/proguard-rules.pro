# -----------------------------
# ShareText release proguard rules
# -----------------------------

# Keep annotations/signatures to avoid issues with Kotlin/Compose/runtime metadata.
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature

# Keep app entry components (explicit and stable in release).
-keep class com.cy.shareText.MainActivity { *; }
-keep class com.cy.shareText.SettingsActivity { *; }
-keep class com.cy.shareText.QRCodeActivity { *; }
-keep class com.cy.shareText.WebService { *; }

# Keep Ktor server entry points used by embeddedServer/routing lambdas.
-keep class io.ktor.server.** { *; }
-keep class io.ktor.http.** { *; }

# Ktor/slf4j optional logger bindings may be absent on Android.
-dontwarn org.slf4j.**
-dontwarn com.sun.nio.file.SensitivityWatchEventModifier
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# Keep ZXing core package used for QR code generation.
-keep class com.google.zxing.** { *; }

