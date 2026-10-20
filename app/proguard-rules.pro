# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class nl.topo.app.**$$serializer { *; }
-keepclassmembers class nl.topo.app.** {
    *** Companion;
}
-keepclasseswithmembers class nl.topo.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
