# ProGuard / R8 Hardening Rules for ARISE

# Strip debug and verbose logging in release builds to prevent credential/PII leaks
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Preserve Security & Crypto Models
-keep class com.example.data.security.** { *; }
-keepclassmembers class com.example.data.security.** { *; }

# Protect Data Models used in Firestore and Serialization
-keep class com.example.data.HunterProfile { *; }
-keep class com.example.data.HunterQuest { *; }
-keep class com.example.data.HunterCommunityMessage { *; }
-keep class com.example.data.HunterShopItem { *; }
-keep class com.example.data.HunterPurchaseRecord { *; }
-keep class com.example.data.HunterHabit { *; }
-keep class com.example.data.HunterPlannerItem { *; }
-keep class com.example.data.HunterGoal { *; }
-keep class com.example.data.HunterFocusSession { *; }

# Obfuscation and Source Attribute protection
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
