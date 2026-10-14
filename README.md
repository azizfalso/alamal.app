# العمل — تطبيق تتبع أرباح العمل بالساعة

تطبيق أندرويد عربي (RTL) يعمل دون إنترنت. الجزء الأول يشمل: الأساس، الثيم، شاشة القفل، والإعدادات.

- **اسم الحزمة:** `com.alamal.app`
- **الحد الأدنى:** أندرويد 12L (API 32)
- **الهدف:** أندرويد 15 (API 35)
- **المعمارية:** `arm64-v8a`
- **البناء:** release موقّع من GitHub Actions

---

## قبل البناء — أضف Repository secrets

من المستودع: **Settings** ← **Secrets and variables** ← **Actions** ← تبويب **Repository secrets** ← `New repository secret`.

| الاسم | إلزامي | الوصف |
|---|---|---|
| `KEYSTORE_BASE64` | ✅ | ملف keystore (`release.jks`) بصيغة Base64 على سطر واحد. |
| `KEYSTORE_PASSWORD` | ✅ | كلمة مرور keystore. |
| `KEY_ALIAS` | ✅ | اسم المفتاح داخل keystore. |
| `KEY_PASSWORD` | ✅ | كلمة مرور المفتاح. |

**بديل:** يمكنك استخدام `KEYSTORE` بدلًا من `KEYSTORE_BASE64` — كلاهما مدعوم.

> ⚠️ تأكد أن Base64 سطر واحد بدون مسافات أو أسطر فارغة.

---

## كيف تحصل على APK

1. ارفع الملفات إلى المستودع (فرع `main` أو `master`).
2. افتح تبويب **Actions**.
3. انتظر ظهور ✅ خضراء على تشغيل **Build Release APK**.
4. ادخل إلى آخر تشغيل، في الأسفل: **Artifacts** ← نزّل `alamal-release`.
5. افتح الـ ZIP، ستجد `app-release.apk` — ثبّته.

---

## إذا فشل البناء

افتح التشغيل الفاشل، اضغط الخطوة الحمراء، انسخ آخر 30 سطرًا من السجل وأرسلها لي. سأصلح الخطأ وأعطيك الملفات التي تغيّرت فقط.

---

## بنية المشروع (مختصرة)

```

.github/workflows/build.yml   ← سير عمل البناء
gradle/libs.versions.toml     ← إصدارات المكتبات
build.gradle.kts              ← إعدادات الجذر
settings.gradle.kts
gradle.properties
app/
build.gradle.kts            ← إعدادات التطبيق والتوقيع
proguard-rules.pro
src/main/
AndroidManifest.xml
res/                      ← موارد (نصوص، ثيمات، أيقونة)
java/com/alamal/app/      ← كود كوتلن

```

## الإصدار

- v1.0.0 — الجزء الأول: الأساس، الثيم، القفل، الإعدادات.
