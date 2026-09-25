package com.cardscanner.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/** Runs ML Kit's on-device text recognition; nothing leaves the phone. */
object CardTextRecognizer {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun recognize(context: Context, imageFile: File): List<OcrLine> {
        val image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
        val result = recognizer.process(image).await()
        return result.textBlocks
            .flatMap { it.lines }
            .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
            .map { OcrLine(it.text, it.boundingBox?.height() ?: 0) }
    }
}
