# ── Steam auth (JavaSteam) ────────────────────────────────────────────────────
# Transitive build-time annotations (protobuf/guava) R8 can't find — safe to ignore.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.j2objc.annotations.**

# JavaSteam drives protobuf message classes by reflection — keep them intact.
-keep class in.dragonbra.javasteam.** { *; }
-dontwarn in.dragonbra.javasteam.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# BouncyCastle JCE provider is registered/used reflectively at runtime.
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
