package io.github.teouk.fomo.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Reads the lines of text in a photo. An interface, so the Search ViewModel can be tested with a fake. */
interface PosterReader {
    /**
     * @param photoUri a content:// URI (from the camera or the photo picker), as text so
     *   that callers and tests don't depend on Android's Uri class.
     * @return the text lines from top to bottom; empty if the photo has no text.
     */
    suspend fun readLines(photoUri: String): List<String>
}

/**
 * On-device OCR with Google ML Kit Text Recognition v2 (Latin script).
 * Docs: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
 *
 * The model is bundled in the app, so the photo never leaves the phone and it works
 * offline and without Google Play services.
 */
class MlKitPosterReader(private val context: Context) : PosterReader {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun readLines(photoUri: String): List<String> {
        // Decoding a camera photo is slow, so it happens off the main thread.
        val image = withContext(Dispatchers.Default) { InputImage.fromFilePath(context, Uri.parse(photoUri)) }
        val text = recognizer.process(image).await()
        // Blocks are paragraphs; their lines keep the poster's reading order.
        return text.textBlocks.flatMap { block -> block.lines.map { it.text } }
    }
}
