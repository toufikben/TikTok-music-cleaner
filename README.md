# TikTok Audio Cleaner

Android application for reducing background music in TikTok videos while preserving speech.

## Processing architecture

The processing flow is exposed through `AudioProcessor`, an injectable suspend contract. The ViewModel now forwards the selected URI and all user settings to that contract, reports bounded progress, cancels stale jobs, and records history only after a model-backed processor reports success.

The default `ModelUnavailableAudioProcessor` intentionally fails clearly when no separation model is packaged. This prevents the previous false-positive behaviour where a delay animation was presented as completed audio processing. To enable local inference, add the licensed `music_separator.tflite` model under `app/src/main/assets/` and provide an `AudioProcessor` implementation that performs the model's documented input/output tensor mapping.

`PreviewAudioProcessor` is provided for deterministic previews and unit tests; it must not be used as production separation.

## Verification

Run `./gradlew test` on a machine with Android SDK configured. The repository includes the Gradle wrapper; the current sandbox did not contain an Android SDK, so Gradle dependency resolution could not execute the Android test task.

## FFmpeg pipeline

The app now uses `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.7`. `FfmpegAudioProcessor` copies a `content://` input into cache, runs FFmpeg with the original video stream copied (`-c:v copy`), applies a voice-focused audio filter, encodes only the audio as AAC, and returns a new MP4 URI. The original video is not overwritten. The filter reduces music energy and improves speech clarity; it is not neural source separation, so a licensed separation model is still required for true vocal/music stems.

The output can be saved through `MediaStore` from the processor screen. The floating control opens that processing screen directly, while Android does not guarantee interception or modification of another app's live playback audio.
