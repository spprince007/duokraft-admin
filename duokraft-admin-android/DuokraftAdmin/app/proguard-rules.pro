# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers @kotlinx.serialization.Serializable class se.duokraft.admin.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class se.duokraft.admin.**$$serializer { *; }
# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.**
