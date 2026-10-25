# Broadcast receivers are referenced from the manifest only.
-keep class com.owaiskhan.waterreminder.ReminderReceiver { *; }
-keep class com.owaiskhan.waterreminder.BootReceiver { *; }

# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
