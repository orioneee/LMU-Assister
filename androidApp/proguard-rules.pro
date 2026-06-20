# androidx.security.crypto pulls in Google Tink, which references build-time-only
# annotations (errorprone / jsr305 / j2objc) that aren't on the runtime classpath.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.j2objc.annotations.**

# ── Steam auth (JavaSteam) ────────────────────────────────────────────────────
# JavaSteam drives protobuf message classes by reflection — keep them intact.
-keep class in.dragonbra.javasteam.** { *; }
-dontwarn in.dragonbra.javasteam.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# BouncyCastle JCE provider is registered/used reflectively at runtime.
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
