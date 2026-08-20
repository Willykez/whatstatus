package com.willykez.wastatus

import androidx.documentfile.provider.DocumentFile
import com.willykez.wastatus.data.SafUtils
import com.willykez.wastatus.model.StatusType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Real, non-mocked coverage for the two things "batch download" actually
 * depends on: finding every linked WhatsApp account's `.Statuses` folder,
 * and correctly filtering a real folder of mixed files down to just the
 * real status media before a batch save is attempted. Both tests operate
 * on a genuine temp-directory tree via [DocumentFile.fromFile] — no SAF
 * grant or MediaStore required, so they run anywhere `testDebugUnitTest`
 * runs, including CI.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatchDownloadTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `discovers the primary account plus every linked account`() {
        val whatsAppRoot = tempFolder.newFolder("WhatsApp")
        File(whatsAppRoot, "Media/.Statuses").apply { mkdirs() }
        File(whatsAppRoot, "accounts/1004/Media/.Statuses").apply { mkdirs() }
        File(whatsAppRoot, "accounts/2002/Media/.Statuses").apply { mkdirs() }

        val roots = SafUtils.discoverAccountRootsFrom(DocumentFile.fromFile(whatsAppRoot), "Personal")

        assertEquals("Primary account plus two linked accounts", 3, roots.size)
        assertTrue(roots.any { it.label == "Personal" })
        assertTrue(roots.any { it.label == "Personal · 1004" })
        assertTrue(roots.any { it.label == "Personal · 2002" })
        roots.forEach { root ->
            assertNotNull("Every discovered account should have a real .Statuses folder", SafUtils.statusesFolderFor(root.mediaFolder))
        }
    }

    @Test
    fun `an older single-account WhatsApp with no accounts folder still resolves`() {
        val whatsAppRoot = tempFolder.newFolder("WhatsApp")
        File(whatsAppRoot, "Media/.Statuses").apply { mkdirs() }

        val roots = SafUtils.discoverAccountRootsFrom(DocumentFile.fromFile(whatsAppRoot), "Personal")

        assertEquals(1, roots.size)
        assertEquals("Personal", roots.first().label)
    }

    @Test
    fun `batch of real status files is filtered and typed correctly before saving`() {
        val statusesDir = tempFolder.newFolder("WhatsApp", "Media", ".Statuses")
        listOf("IMG-001.jpg", "IMG-002.png", "VID-001.mp4", "sender.nomedia", ".pending.tmp")
            .forEach { name -> File(statusesDir, name).writeText("fake-bytes") }

        val batch = DocumentFile.fromFile(statusesDir).listFiles().filter { doc ->
            doc.isFile && doc.name?.let { SafUtils.statusTypeFromName(it) != null } == true
        }

        // Only the 3 real media files should enter the batch — the .nomedia
        // marker and the in-progress temp file are correctly excluded.
        assertEquals(3, batch.size)
        assertEquals(2, batch.count { SafUtils.statusTypeFromName(it.name!!) == StatusType.IMAGE })
        assertEquals(1, batch.count { SafUtils.statusTypeFromName(it.name!!) == StatusType.VIDEO })
    }
}
