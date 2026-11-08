# Keep line numbers for readable crash reports
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Firestore maps documents to these classes via reflection
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Room entities / DAOs
-keep class com.example.data.local.** { *; }

# Firebase / Play services / Ads / InMobi
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**
-dontwarn com.google.android.gms.**

# Kotlin coroutines / metadata
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**
