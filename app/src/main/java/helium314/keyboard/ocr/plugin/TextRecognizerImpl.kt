// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.ocr.plugin

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import helium314.keyboard.latin.ocr.ITextRecognizer
import java.util.concurrent.TimeUnit

class TextRecognizerImpl : ITextRecognizer {

    companion object {
        private const val TAG = "TextRecognizerImpl"
    }

    private var recognizer: TextRecognizer? = null
    private var scriptName: String = "latin"
    private var displayName: String = "Latin"
    private var isInitialized = false

    override fun getInterfaceVersion(): Int = 1

    override fun getScriptName(): String = scriptName

    override fun getDisplayName(): String = displayName

    override fun isAvailable(): Boolean = isInitialized && recognizer != null

    override fun init(context: Context) {
        if (isInitialized) return
        try {
            ensureNativeLibrariesLoaded()
            ensureMlKitInitialized(context)
            recognizer = createRecognizer()
            isInitialized = (recognizer != null)
            Log.i(TAG, "OCR Plugin initialized successfully ($displayName)")
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

    private fun createRecognizer(): TextRecognizer {
        // 1. Try Devanagari
        try {
            val devanagariClass = Class.forName("com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions")
            val builderClass = Class.forName("com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions\$Builder")
            val builder = builderClass.getConstructor().newInstance()
            val buildMethod = builderClass.getMethod("build")
            val options = buildMethod.invoke(builder)
            val getClientMethod = TextRecognition::class.java.methods.firstOrNull { it.name == "getClient" && it.parameterTypes.size == 1 }
            if (getClientMethod != null && options != null) {
                scriptName = "devanagari"
                displayName = "Devanagari"
                return getClientMethod.invoke(null, options) as TextRecognizer
            }
        } catch (_: Throwable) {}

        // 2. Try Chinese
        try {
            val chineseClass = Class.forName("com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions")
            val builderClass = Class.forName("com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions\$Builder")
            val builder = builderClass.getConstructor().newInstance()
            val buildMethod = builderClass.getMethod("build")
            val options = buildMethod.invoke(builder)
            val getClientMethod = TextRecognition::class.java.methods.firstOrNull { it.name == "getClient" && it.parameterTypes.size == 1 }
            if (getClientMethod != null && options != null) {
                scriptName = "chinese"
                displayName = "Chinese"
                return getClientMethod.invoke(null, options) as TextRecognizer
            }
        } catch (_: Throwable) {}

        // 3. Try Japanese
        try {
            val japaneseClass = Class.forName("com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions")
            val builderClass = Class.forName("com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions\$Builder")
            val builder = builderClass.getConstructor().newInstance()
            val buildMethod = builderClass.getMethod("build")
            val options = buildMethod.invoke(builder)
            val getClientMethod = TextRecognition::class.java.methods.firstOrNull { it.name == "getClient" && it.parameterTypes.size == 1 }
            if (getClientMethod != null && options != null) {
                scriptName = "japanese"
                displayName = "Japanese"
                return getClientMethod.invoke(null, options) as TextRecognizer
            }
        } catch (_: Throwable) {}

        // 4. Try Korean
        try {
            val koreanClass = Class.forName("com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions")
            val builderClass = Class.forName("com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions\$Builder")
            val builder = builderClass.getConstructor().newInstance()
            val buildMethod = builderClass.getMethod("build")
            val options = buildMethod.invoke(builder)
            val getClientMethod = TextRecognition::class.java.methods.firstOrNull { it.name == "getClient" && it.parameterTypes.size == 1 }
            if (getClientMethod != null && options != null) {
                scriptName = "korean"
                displayName = "Korean"
                return getClientMethod.invoke(null, options) as TextRecognizer
            }
        } catch (_: Throwable) {}

        // 5. Try Latin
        try {
            val latinClass = Class.forName("com.google.mlkit.vision.text.latin.TextRecognizerOptions")
            val defaultField = latinClass.getField("DEFAULT_OPTIONS")
            val options = defaultField.get(null)
            val getClientMethod = TextRecognition::class.java.methods.firstOrNull { it.name == "getClient" && it.parameterTypes.size == 1 }
            if (getClientMethod != null && options != null) {
                scriptName = "latin"
                displayName = "Latin"
                return getClientMethod.invoke(null, options) as TextRecognizer
            }
        } catch (_: Throwable) {}

        throw IllegalStateException("No ML Kit Text Recognition options class found on classpath")
    }

    override fun recognize(bitmap: Bitmap, keepLineBreaks: Boolean): List<String>? {
        val rec = recognizer ?: return null
        val image = InputImage.fromBitmap(bitmap, 0)
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
        return lines
    }

    override fun release() {
        try {
            recognizer?.close()
        } catch (_: Throwable) {}
        recognizer = null
        isInitialized = false
    }
}
