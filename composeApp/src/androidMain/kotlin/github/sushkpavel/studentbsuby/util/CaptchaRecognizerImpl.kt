package github.sushkpavel.studentbsuby.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CaptchaRecognizerImpl : CaptchaRecognizer {

    private val recognizer = TextRecognition
        .getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(image: ImageBitmap): String {
        return runCatchingSuspend {
            val inputImage = InputImage.fromBitmap(image.asAndroidBitmap(), 0)
            suspendCoroutine { cont ->
                recognizer.process(inputImage).addOnCompleteListener { task ->
                    when {
                        task.isSuccessful ->
                            cont.resume(task.result?.textBlocks?.firstOrNull()?.text.orEmpty())
                        else ->
                            cont.resumeWithException(task.exception ?: IllegalStateException("Text recognition cancelled"))
                    }
                }
            }
        }.getOrDefault("")
    }
}

actual fun createCaptchaRecognizer(): CaptchaRecognizer = CaptchaRecognizerImpl()
