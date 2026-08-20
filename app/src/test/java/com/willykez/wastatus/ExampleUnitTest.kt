package com.willykez.wastatus

import com.willykez.wastatus.data.SafUtils
import com.willykez.wastatus.model.StatusType
import com.willykez.wastatus.model.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure unit tests for the real (non-Android) helper logic WaStatus relies
 * on: byte-size formatting and file classification for statuses discovered
 * via the Storage Access Framework.
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun formatBytes_scalesToTheRightUnit() {
        assertEquals("512 B", formatBytes(512))
        assertEquals("2 KB", formatBytes(2048))
        assertEquals("1.5 MB", formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.00 GB", formatBytes(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun statusTypeFromName_classifiesKnownExtensions() {
        assertEquals(StatusType.IMAGE, SafUtils.statusTypeFromName("IMG-20260101-WA0001.jpg"))
        assertEquals(StatusType.IMAGE, SafUtils.statusTypeFromName("photo.PNG"))
        assertEquals(StatusType.VIDEO, SafUtils.statusTypeFromName("VID-20260101-WA0002.mp4"))
        assertNull(SafUtils.statusTypeFromName("notes.txt"))
        assertNull(SafUtils.statusTypeFromName("no_extension"))
    }

    @Test
    fun mimeTypeFromName_matchesExtension() {
        assertEquals("image/jpeg", SafUtils.mimeTypeFromName("a.jpg"))
        assertEquals("video/mp4", SafUtils.mimeTypeFromName("a.mp4"))
        assertEquals("application/octet-stream", SafUtils.mimeTypeFromName("a.unknown"))
    }

    @Test
    fun titleFromFileName_stripsExtensionAndSeparators() {
        assertEquals("IMG 20260101 WA0001", SafUtils.titleFromFileName("IMG_20260101-WA0001.jpg"))
        assertEquals("WhatsApp Status", SafUtils.titleFromFileName(".jpg"))
    }
}
