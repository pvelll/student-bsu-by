package github.sushkpavel.studentbsuby.repo

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun readCachedPhoto(fileName: String): ByteArray?

internal expect fun writeCachedPhoto(fileName: String, bytes: ByteArray)

internal expect fun ImageBitmap.encodeToJpegBytes(): ByteArray
