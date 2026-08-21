package com.willykez.wastatus

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.willykez.wastatus.data.StatusRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WaStatus", appName)
    }

    @Test
    fun `statuses are empty until a real folder is granted`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = StatusRepository(context)

        repository.refreshStatuses()

        assertTrue("No folder has been granted, so there should be no statuses", repository.statuses.value.isEmpty())
    }

    @Test
    fun `direct chat messages persist across loads`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = StatusRepository(context)

        repository.addDirectChatMessage("+15550001234", "Hello from a real test")
        repository.loadChatHistory()

        val history = repository.directChatHistory.value
        assertEquals(1, history.size)
        assertEquals("+15550001234", history.first().phoneNumber)
        assertEquals("Hello from a real test", history.first().messageText)
    }
}
