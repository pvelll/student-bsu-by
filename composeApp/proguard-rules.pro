# The original release build disabled obfuscation (postprocessing.isObfuscate = false).
-dontobfuscate

# Crashlytics: keep file names and line numbers in stack traces, and the names of custom exceptions.
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
