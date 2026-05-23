# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard configuration.

# Keep Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }

# Keep our agent bridge interfaces
-keep class com.pocketagent.app.agent.** { *; }
-keep class com.pocketagent.app.termux.** { *; }