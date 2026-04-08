# Add project specific ProGuard rules here.

# Keep Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Keep Room entities
-keep class com.nexterm.app.data.local.entity.** { *; }

# Keep data classes
-keepclassmembers class com.nexterm.app.data.model.** { *; }

# Keep terminal classes (our custom ones)
-keep class com.nexterm.app.terminal.** { *; }
-keepclassmembers class com.nexterm.app.terminal.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-dontwarn androidx.compose.**

# Keep native method names (if we use JNI for bootstrap)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Prevent stripping of generic signatures
-keepattributes Signature
-keepattributes Exceptions