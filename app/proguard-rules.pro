# GlobeRater release rules. Most libraries (Room, Hilt, WorkManager, OkHttp, Retrofit 2.11)
# ship their own consumer rules; only our Gson models need to be kept here.

# JSON models parsed reflectively by Gson: keep fields and their names.
-keep class com.example.currencywidget.data.api.** { *; }
-keep class com.example.currencywidget.data.model.** { *; }

# Retrofit service interfaces are created via reflection with generic signatures.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Gson TypeToken generic info
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Widget providers are referenced from the manifest (kept by AAPT rules already);
# RemoteViews reflection targets are framework methods, nothing to keep on our side.
