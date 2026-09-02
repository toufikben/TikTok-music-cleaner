# إضافة نموذج `music_separator.tflite` وتفعيل معالجة الصوت الحقيقية

## أولاً: اختر النوع الصحيح من النموذج

ملف `.tflite` وحده لا يكفي. يجب أن يكون النموذج **نموذج فصل مصادر صوتية**، وليس نموذج تصنيف مثل YAMNet. نموذج التصنيف يقول إن المقطع يحتوي على Speech أو Music، لكنه لا ينتج مساراً صوتياً منفصلاً يمكن إعادة دمجه في الفيديو.

يلزمك نموذج موثق يحدد بوضوح:

| الخاصية | المطلوب توثيقه |
|---|---|
| المدخل | الشكل، النوع، عدد القنوات، ومعدل العينة |
| المخرجات | هل هي `speech`, `music`, `accompaniment` أو قناع طيفي؟ |
| التحضير | التطبيع، حجم النافذة، hop size، وSTFT/ISTFT إن وجدت |
| الأداء | حجم النموذج، الذاكرة، وزمن inference على الهاتف |
| الترخيص | السماح بالتوزيع داخل تطبيق Android |

توثيق TensorFlow يوضح أن نماذج الصوت قد تتطلب صوتاً أحادي القناة بمعدل 16 kHz وقيماً مطبّعة ضمن `[-1, 1]`؛ لكن هذه ليست مواصفات عامة لكل نموذج، ولذلك يجب اتباع بطاقة النموذج الذي تختاره تحديداً [1] [2].

## 1. ضع الملف داخل المشروع

أنشئ مجلد الأصول ثم انسخ النموذج المرخّص إليه:

```bash
mkdir -p app/src/main/assets
cp /path/to/music_separator.tflite app/src/main/assets/music_separator.tflite
```

لا تضع نموذجاً تم تنزيله عشوائياً في GitHub ولا تزيل معلومات الترخيص. إذا كان حجمه كبيراً، استخدم Git LFS أو نزّله أثناء عملية البناء مع التحقق من checksum، بدلاً من إخفائه داخل التطبيق.

## 2. أضف Runtime الخاص بـ LiteRT/TensorFlow Lite

في `app/build.gradle.kts` أضف dependency مناسبة. في المشاريع التي تستخدم Interpreter التقليدي يمكن البدء بـ:

```kotlin
dependencies {
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
}
```

يمكن استخدام LiteRT الأحدث بدلاً من الاسم القديم TensorFlow Lite، مع مراجعة تعليمات الترحيل والإصدار المتوافق مع Android Gradle Plugin في مشروعك [3]. لا تخلط بين مكتبة تصنيف الصوت ومكتبة تشغيل نموذج فصل مخصص؛ واجهات Tasks تتوقع metadata ومخرجات تصنيف معينة، بينما نموذج الفصل غالباً يحتاج `Interpreter` أو واجهة مخصصة [4].

## 3. افحص النموذج قبل كتابة Kotlin

افحص tensors من Python حتى تعرف العقدة الصحيحة. أنشئ ملفاً محلياً باسم `inspect_model.py`:

```python
import tensorflow as tf

interpreter = tf.lite.Interpreter(model_path="music_separator.tflite")
interpreter.allocate_tensors()

print("inputs:")
for item in interpreter.get_input_details():
    print(item["name"], item["shape"], item["dtype"], item["quantization"])

print("outputs:")
for item in interpreter.get_output_details():
    print(item["name"], item["shape"], item["dtype"], item["quantization"])
```

إذا كانت المخرجات مجرد scores بحجم `[1, N]`، فهذا نموذج تصنيف وليس نموذج فصل. نموذج الفصل المطلوب يجب أن يعيد waveform أو mask/stem يمكن تحويله إلى waveform.

## 4. نفّذ `AudioProcessor` حقيقياً

المشروع يحتوي حالياً على العقدة:

```kotlin
interface AudioProcessor {
    suspend fun process(
        input: Uri,
        musicBlockLevel: Float,
        vocalBoost: Boolean,
        noiseReduction: Boolean,
        onProgress: suspend (Int) -> Unit,
    ): ProcessingResult
}
```

أنشئ مثلاً `TfliteAudioProcessor.kt`. الهيكل الأدنى لتشغيل النموذج يكون كالتالي، لكن أسماء الأشكال وطريقة التحضير يجب تعديلها وفق تقرير `inspect_model.py`:

```kotlin
class TfliteAudioProcessor(
    private val context: Context,
) : AudioProcessor {
    private val interpreter by lazy {
        Interpreter(
            FileUtil.loadMappedFile(context, "music_separator.tflite"),
            Interpreter.Options().apply { setNumThreads(4) }
        )
    }

    override suspend fun process(
        input: Uri,
        musicBlockLevel: Float,
        vocalBoost: Boolean,
        noiseReduction: Boolean,
        onProgress: suspend (Int) -> Unit,
    ): ProcessingResult = withContext(Dispatchers.Default) {
        require(musicBlockLevel in 0f..1f)

        // 1. استخرج المسار الصوتي من الفيديو عبر MediaExtractor/MediaCodec.
        // 2. حوّله إلى mono، وأعد sampling إلى معدل النموذج، وطبّع float إلى [-1, 1].
        // 3. قسّمه إلى نوافذ بالحجم الذي يتوقعه النموذج.
        // 4. نفّذ interpreter.runForMultipleInputsOutputs لكل نافذة.
        // 5. أعد تركيب النوافذ overlap-add أو حوّل masks عبر ISTFT.
        // 6. طبّق musicBlockLevel وvocalBoost/noiseReduction على مسار الكلام.
        // 7. اخلط الصوت المنقى مع الفيديو واكتب ملف MP4 جديداً.
        // 8. أعد Uri للملف الناتج، وليس Uri الإدخال.

        onProgress(100)
        ProcessingResult(input = input, output = null, usedModel = true)
    }
}
```

هذا الهيكل ليس تنفيذاً كاملاً عن قصد: استخراج الصوت، إعادة الترميز، وISTFT تعتمد على مخرجات النموذج. لا يجوز تحويله إلى نجاح شكلي قبل أن يعيد ملفاً صوتياً/فيديو حقيقياً.

## 5. عالج الفيديو والصوت

`MediaExtractor` يقرأ track الصوت من MP4، لكن إنشاء MP4 نهائي يحتاج عادةً إلى:

1. استخراج audio samples وفك ترميزها بواسطة `MediaCodec`.
2. تمرير PCM إلى النموذج على نوافذ متتابعة.
3. إعادة ترميز المسار الصوتي المنقى إلى AAC عبر `MediaCodec`.
4. نسخ video track الأصلي وmuxing المسارين باستخدام `MediaMuxer`.

يجب تنفيذ هذه العملية في `Dispatchers.Default` أو `Dispatchers.IO`، مع احترام cancellation، وكتابة الناتج إلى `cacheDir` أو MediaStore. لا تعتمد على `videoUrl` مباشرة إذا كان `content://`؛ استخدم `ContentResolver.openFileDescriptor` أو انسخ المصدر إلى ملف مؤقت قابل للقراءة.

## 6. اربط المعالج بالـ ViewModel

بدلاً من الاعتماد على `ModelUnavailableAudioProcessor` الافتراضي، مرّر المعالج الحقيقي من Factory أو dependency injection. مثال مبسط في `NavGraph`:

```kotlin
val context = LocalContext.current
val viewModel: AudioCleanerViewModel = viewModel(
    factory = AudioCleanerViewModelFactory(
        audioProcessor = TfliteAudioProcessor(context.applicationContext)
    )
)
```

احذف المعالج الافتراضي فقط بعد التأكد من أن النموذج موجود وأن التحقق من tensors ينجح. يجب أن تعرض الواجهة زر الحفظ/المشاركة فقط عندما تكون `ProcessingResult.output != null`.

## 7. تحقق من الجودة

اختبر الحالات التالية قبل اعتبار الميزة مكتملة:

| الاختبار | النتيجة المتوقعة |
|---|---|
| فيديو بلا audio track | رسالة خطأ واضحة دون crash |
| `content://` من مدير الملفات | قراءة ناجحة دون افتراض filesystem path |
| صوت 44.1 kHz أو stereo | تحويل صحيح إلى مواصفات النموذج |
| فيديو أطول من نافذة النموذج | معالجة كل النوافذ دون فجوات أو تكرار مسموع |
| إلغاء المعالجة | إيقاف inference وحذف الملف المؤقت |
| نموذج مفقود أو غير صالح | Error واضح عند بدء التطبيق/المعالجة |
| حفظ الناتج | `output Uri` صالح ويمكن تشغيله في مشغل Android |
| أذونات Android الحديثة | استخدام MediaStore وواجهة المشاركة بدلاً من صلاحيات واسعة قدر الإمكان |

## ما أنصح به عملياً

ابدأ بنموذج فصل صغير وموثق، ثم نفّذ أولاً pipeline مستقل على الكمبيوتر: `MP4/WAV -> PCM -> model -> speech WAV`. بعد التأكد من أن الناتج الصوتي صحيح، انقل نفس preprocessing وpostprocessing إلى Kotlin. لا تستخدم YAMNet كبديل لفصل الصوت؛ YAMNet مصمم للتعرف على 521 فئة صوتية، وليس لإخراج stems منفصلة [1].

### المراجع

[1]: https://www.tensorflow.org/hub/tutorials/yamnet "TensorFlow: Sound classification with YAMNet"
[2]: https://www.tensorflow.org/tutorials/audio/transfer_learning_audio "TensorFlow: Transfer learning with YAMNet"
[3]: https://developers.google.com/edge/litert "Google AI Edge: LiteRT"
[4]: https://ai.google.dev/edge/api/mediapipe/python/mp/tasks/audio/AudioClassifier "Google AI Edge: AudioClassifier model requirements"
