# Keep data models & repository classes for Firestore, Room, and JSON backup/restore
-keep class com.example.data.model.** { *; }
-keep class com.example.data.repository.** { *; }
-keep class com.example.data.local.** { *; }

# Keep Kotlin metadata & annotations used by Room / reflection
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,*Annotation*

# Keep Firebase Firestore & Auth models safely
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn androidx.credentials.**
-dontwarn org.slf4j.**
