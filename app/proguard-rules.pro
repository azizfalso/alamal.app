# ---- Room ----
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---- Kotlin ----
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# ---- App entities ----
-keep class com.alamal.app.data.db.** { *; }

# ---- Coroutines ----
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ---- Compose ----
-dontwarn androidx.compose.**

# ---- DataStore ----
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ---- Keep enums ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
