# androidx.security.crypto pulls in Google Tink, which references build-time-only
# annotations (errorprone / jsr305 / j2objc) that aren't on the runtime classpath.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.j2objc.annotations.**
