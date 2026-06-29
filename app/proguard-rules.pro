# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# Keep all data-layer classes (models AND API DTOs like AdMyListResponse)
-keep class ru.agromarket.data.** { *; }
-keepclassmembers class ru.agromarket.data.** { *; }

# kotlinx.serialization
# (DTO-классы и сгенерированные $$serializer лежат в ru.agromarket.data.** —
#  уже сохранены keep-правилом выше; ниже — общие правила рантайма сериализации.)
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static <1>$$serializer INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# Retrofit
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.* { *; }
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-dontwarn dagger.hilt.**
