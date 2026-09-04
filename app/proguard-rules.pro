# Proguard rules for LeanType OCR Plugin

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Keep the entry point class, its constructor, and all public methods,
# as it is loaded dynamically by class name reflection.
-keep class helium314.keyboard.ocr.plugin.TextRecognizerImpl {
    public <init>();
    public <methods>;
}

# Keep the interface methods to match the host app
-keep interface helium314.keyboard.latin.ocr.ITextRecognizer {
    <methods>;
}

-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepnames class com.google.mlkit.** extends androidx.work.ListenableWorker

# Keep ML Kit components, JNI classes and Firebase/GMS dependencies
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }

-dontwarn com.google.**
-dontwarn androidx.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**

