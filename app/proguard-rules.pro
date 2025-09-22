# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep drag and drop related classes
-keep class com.example.draganddrop.UnifiedDataManager { *; }
-keep class com.example.draganddrop.SimpleDragListener { *; }
-keep class com.example.draganddrop.CrossRecyclerViewTouchHelper { *; }

# Keep Item data class
-keep class com.example.draganddrop.Item { *; }

# Keep RecyclerView adapters
-keep class com.example.draganddrop.ItemAdapter { *; }
-keep class com.example.draganddrop.ItemAdapter$ItemViewHolder { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowshrinking,allowobfuscation interface retrofit2.Call
-keep,allowshrinking,allowobfuscation class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation