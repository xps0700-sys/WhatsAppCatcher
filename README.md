# مقتنص المحذوفات - WhatsAppCatcherPro

مشروع Android Kotlin محلي، يستهدف Android 12 و13 و14.

## بناء APK عبر GitHub Actions

هذه النسخة **لا تعتمد على ملف `gradlew`**. يقوم GitHub Actions بإعداد Gradle 8.7 على خادم البناء ثم ينفذ مهمة:

`gradle :app:assembleDebug`

بعد نجاح البناء، ستجد APK تحت Artifacts باسم:

`WhatsAppCatcherPro-APK`

## حقوق التطبيق

إعداد وتطوير محمد أمين © 2026

## ملاحظة الخصوصية

التطبيق لا يحتوي على خادم أو API لرفع البيانات. لا يمكنه استعادة رسالة لم يتمكن من قراءتها من إشعار WhatsApp، ولا ملفاً لم يتم تنزيله فعلياً إلى الجهاز.
