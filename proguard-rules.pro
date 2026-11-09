# قواعد R8 لـ «رصد»
# الحفاظ على كيانات Room (يُبنى عليها توليد الكود والانعكاس)
-keep class com.rasd.app.data.model.** { *; }

# الحفاظ على مُستقبِلات البث المُعلنة في المانيفست
-keep class com.rasd.app.receiver.** { *; }

# Hilt / Dagger
-keep,allowobfuscation @interface dagger.hilt.android.*

# إسكات تحذيرات مكتبات اختيارية
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**

# إبقاء أسماء الطرق الأصلية مفيدة لتقارير الانهيار
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
