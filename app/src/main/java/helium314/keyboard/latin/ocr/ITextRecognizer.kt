// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ocr

import android.content.Context
import android.graphics.Bitmap

interface ITextRecognizer {
    fun getInterfaceVersion(): Int = 1
    fun getScriptName(): String
    fun getDisplayName(): String
    fun init(context: Context)
    fun recognize(bitmap: Bitmap, keepLineBreaks: Boolean): List<String>?
    fun isAvailable(): Boolean
    fun release()
}
