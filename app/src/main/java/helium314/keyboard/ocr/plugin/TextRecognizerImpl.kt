// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.ocr.plugin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import helium314.keyboard.latin.ocr.ITextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class TextRecognizerImpl : ITextRecognizer {

    companion object {
        private const val TAG = "TextRecognizerImpl"
        private const val PREF_OCR_SCRIPT = "pref_ocr_script"
    }

    private var pluginContext: Context? = null
    private var devanagariRecognizer: TextRecognizer? = null
    private var chineseRecognizer: TextRecognizer? = null
    private var japaneseRecognizer: TextRecognizer? = null
    private var koreanRecognizer: TextRecognizer? = null
    private var latinRecognizer: TextRecognizer? = null

    private var isInitialized = false

    override fun getInterfaceVersion(): Int = 1

    override fun getScriptName(): String = "universal"

    override fun getDisplayName(): String = "Universal (All Scripts)"

    override fun isAvailable(): Boolean = isInitialized && (
        devanagariRecognizer != null ||
        chineseRecognizer != null ||
        japaneseRecognizer != null ||
        koreanRecognizer != null ||
        latinRecognizer != null
    )

    override fun init(context: Context) {
        if (isInitialized) return
        this.pluginContext = context
        try {
            ensureNativeLibrariesLoaded()
            ensureMlKitInitialized(context)
            initRecognizers()
            isInitialized = (
                devanagariRecognizer != null ||
                chineseRecognizer != null ||
                japaneseRecognizer != null ||
                koreanRecognizer != null ||
                latinRecognizer != null
            )
            Log.i(TAG, "OCR Plugin initialized successfully (${getDisplayName()})")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize TextRecognizerImpl", e)
            isInitialized = false
        }
    }

    private fun ensureNativeLibrariesLoaded() {
        try {
            System.loadLibrary("mlkit_google_ocr_pipeline")
            Log.i(TAG, "Loaded mlkit_google_ocr_pipeline")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not explicitly load mlkit_google_ocr_pipeline (might already be loaded)", e)
        }
    }

    private fun ensureMlKitInitialized(ctx: Context) {
        try {
            val mlKitContextClass = Class.forName("com.google.mlkit.common.sdkinternal.MlKitContext")
            try {
                val field = mlKitContextClass.getDeclaredField("zzb")
                field.isAccessible = true
                field.set(null, null)
            } catch (_: Throwable) {}

            try {
                val initMethodSimple = mlKitContextClass.getDeclaredMethod("initialize", Context::class.java)
                initMethodSimple.invoke(null, ctx)
                Log.i(TAG, "MlKitContext.initialize(Context) completed")
                return
            } catch (_: Throwable) {}

            val registrars = mutableListOf<Any>()
            try {
                registrars.add(Class.forName("com.google.mlkit.common.internal.CommonComponentRegistrar").getConstructor().newInstance())
            } catch (_: Throwable) {}
            try {
                registrars.add(Class.forName("com.google.mlkit.vision.common.internal.VisionCommonRegistrar").getConstructor().newInstance())
            } catch (_: Throwable) {}
            try {
                registrars.add(Class.forName("com.google.mlkit.vision.text.internal.TextRegistrar").getConstructor().newInstance())
            } catch (_: Throwable) {}

            val initMethod = mlKitContextClass.getDeclaredMethod(
                "initialize",
                Context::class.java,
                List::class.java
            )
            initMethod.invoke(null, ctx, registrars)
            Log.i(TAG, "MlKitContext.initialize(Context, List) completed")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize MlKitContext", e)
        }
    }

    private fun initRecognizers() {
        try {
            devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            Log.i(TAG, "Initialized Devanagari text recognizer")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not initialize Devanagari recognizer", e)
        }
        try {
            chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            Log.i(TAG, "Initialized Chinese text recognizer")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not initialize Chinese recognizer", e)
        }
        try {
            japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            Log.i(TAG, "Initialized Japanese text recognizer")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not initialize Japanese recognizer", e)
        }
        try {
            koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            Log.i(TAG, "Initialized Korean text recognizer")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not initialize Korean recognizer", e)
        }
        try {
            latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            Log.i(TAG, "Initialized Latin text recognizer")
        } catch (e: Throwable) {
            Log.w(TAG, "Could not initialize Latin recognizer", e)
        }
    }

    override fun recognize(bitmap: Bitmap, keepLineBreaks: Boolean): List<String>? {
        val scriptPref = getSelectedScript()
        val image = InputImage.fromBitmap(bitmap, 0)

        // If user selected a specific script, run only that recognizer
        val selectedRecognizer = when (scriptPref) {
            "devanagari" -> devanagariRecognizer ?: latinRecognizer
            "chinese" -> chineseRecognizer ?: latinRecognizer
            "japanese" -> japaneseRecognizer ?: latinRecognizer
            "korean" -> koreanRecognizer ?: latinRecognizer
            "latin" -> latinRecognizer
            else -> null
        }

        if (selectedRecognizer != null) {
            return runSingleRecognizer(selectedRecognizer, image)
        }

        // "all" / Universal mode: run all available composite recognizers concurrently
        val activeRecognizers = listOfNotNull(
            devanagariRecognizer,
            chineseRecognizer,
            japaneseRecognizer,
            koreanRecognizer
        ).ifEmpty { listOfNotNull(latinRecognizer) }

        if (activeRecognizers.isEmpty()) return null

        val results = runBlocking(Dispatchers.Default) {
            activeRecognizers.map { rec ->
                async {
                    try {
                        val task = rec.process(image)
                        Tasks.await(task, 15, TimeUnit.SECONDS)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Recognizer pass failed", e)
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (results.isEmpty()) return null
        return mergeAndDeduplicate(results)
    }

    private fun getSelectedScript(): String {
        return try {
            val ctx = pluginContext ?: return "all"
            val targetCtx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !ctx.isDeviceProtectedStorage) {
                ctx.createDeviceProtectedStorageContext() ?: ctx
            } else {
                ctx
            }
            val prefs = targetCtx.getSharedPreferences("${targetCtx.packageName}_preferences", Context.MODE_PRIVATE)
            prefs.getString(PREF_OCR_SCRIPT, "all") ?: "all"
        } catch (_: Throwable) {
            "all"
        }
    }

    private fun runSingleRecognizer(rec: TextRecognizer, image: InputImage): List<String>? {
        return try {
            val task = rec.process(image)
            val result = Tasks.await(task, 25, TimeUnit.SECONDS) ?: return null
            val lines = mutableListOf<String>()
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    if (line.text.isNotBlank()) {
                        lines.add(line.text.trim())
                    }
                }
            }
            lines
        } catch (e: Throwable) {
            Log.e(TAG, "Single recognizer failed", e)
            null
        }
    }

    private data class ScoredLine(
        val text: String,
        val rect: Rect?,
        val score: Int
    )

    private fun mergeAndDeduplicate(results: List<Text>): List<String> {
        val candidateLines = mutableListOf<ScoredLine>()
        for (result in results) {
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val rawText = line.text.trim()
                    if (rawText.isNotBlank()) {
                        val score = computeScriptScore(rawText)
                        candidateLines.add(ScoredLine(rawText, line.boundingBox, score))
                    }
                }
            }
        }

        if (candidateLines.isEmpty()) return emptyList()

        // Deduplicate spatial overlaps and matching lines
        val finalLines = mutableListOf<ScoredLine>()
        for (candidate in candidateLines) {
            val overlappingIdx = finalLines.indexOfFirst { existing ->
                isOverlapping(existing, candidate)
            }
            if (overlappingIdx == -1) {
                finalLines.add(candidate)
            } else {
                val existing = finalLines[overlappingIdx]
                // Keep the one with higher script score or longer text
                if (candidate.score > existing.score || 
                    (candidate.score == existing.score && candidate.text.length > existing.text.length)) {
                    finalLines[overlappingIdx] = candidate
                }
            }
        }

        // Sort lines top-to-bottom, left-to-right to preserve natural reading order
        finalLines.sortWith(compareBy({ it.rect?.top ?: 0 }, { it.rect?.left ?: 0 }))
        return finalLines.map { it.text }
    }

    private fun isOverlapping(lineA: ScoredLine, lineB: ScoredLine): Boolean {
        // Direct string match
        if (lineA.text.equals(lineB.text, ignoreCase = true)) return true

        // Spatial bounding box overlap check
        val rectA = lineA.rect ?: return false
        val rectB = lineB.rect ?: return false

        val intersect = Rect()
        if (!intersect.setIntersect(rectA, rectB)) return false

        val intersectArea = intersect.width() * intersect.height()
        val minArea = minOf(rectA.width() * rectA.height(), rectB.width() * rectB.height())
        if (minArea <= 0) return false

        return (intersectArea.toFloat() / minArea.toFloat()) > 0.4f
    }

    private fun computeScriptScore(text: String): Int {
        var score = 0
        for (ch in text) {
            val code = ch.code
            when {
                // Devanagari (Hindi, Marathi, Sanskrit, Nepali)
                code in 0x0900..0x097F -> score += 5
                // Chinese / CJK Unified Ideographs
                code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF -> score += 5
                // Japanese Hiragana / Katakana
                code in 0x3040..0x309F || code in 0x30A0..0x30FF -> score += 5
                // Korean Hangul
                code in 0xAC00..0xD7AF || code in 0x1100..0x11FF || code in 0x3130..0x318F -> score += 5
                // Standard Latin / Digits
                ch.isLetterOrDigit() -> score += 1
            }
        }
        return score
    }

    override fun release() {
        try {
            devanagariRecognizer?.close()
            chineseRecognizer?.close()
            japaneseRecognizer?.close()
            koreanRecognizer?.close()
            latinRecognizer?.close()
        } catch (_: Throwable) {}
        devanagariRecognizer = null
        chineseRecognizer = null
        japaneseRecognizer = null
        koreanRecognizer = null
        latinRecognizer = null
        isInitialized = false
        pluginContext = null
    }
}
