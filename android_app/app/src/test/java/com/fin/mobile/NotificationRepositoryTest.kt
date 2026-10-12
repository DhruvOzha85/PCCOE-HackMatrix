package com.fin.mobile

import com.fin.mobile.data.remote.NetworkConfig
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.repository.NotificationRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeSessionStorage: FakeSessionStorage
    private lateinit var repository: NotificationRepositoryImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        fakeSessionStorage = FakeSessionStorage()
        fakeSessionStorage.saveSession("test_jwt_token", "test_refresh", 3600, "user_123", "citizen")

        val config = NetworkConfig(
            baseUrl = mockWebServer.url("/").toString(),
            isDebug = false
        )
        val apiService = NetworkModule.createApiService(fakeSessionStorage, config)
        val moshi = NetworkModule.createMoshi()

        repository = NotificationRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testGetNotificationsSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": [
                    {
                        "id": "notif-1",
                        "title": "Application Submitted",
                        "message": "Your application for PM-KISAN was submitted successfully.",
                        "type": "application_status",
                        "read": false,
                        "unread": true,
                        "applicationId": "app-101",
                        "time": "11/10/2026"
                    },
                    {
                        "id": "notif-2",
                        "title": "Document Verified",
                        "message": "Your Aadhaar Card was verified.",
                        "type": "document_status",
                        "read": true,
                        "unread": false,
                        "time": "10/10/2026"
                    }
                ],
                "unreadCount": 1
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getNotifications()

        assertTrue(result is NetworkResult.Success)
        val response = (result as NetworkResult.Success).data
        assertEquals(2, response.data.size)
        assertEquals(1, response.unreadCount)

        val firstNotif = response.data[0]
        assertEquals("notif-1", firstNotif.id)
        assertEquals("Application Submitted", firstNotif.title)
        assertFalse(firstNotif.isRead)
        assertEquals("app-101", firstNotif.applicationId)

        val secondNotif = response.data[1]
        assertEquals("notif-2", secondNotif.id)
        assertTrue(secondNotif.isRead)
    }

    @Test
    fun testMarkNotificationReadSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Notification marked as read"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.markNotificationRead("notif-1")

        assertTrue(result is NetworkResult.Success)
        assertEquals("Notification marked as read", (result as NetworkResult.Success).data)
    }

    @Test
    fun testMarkAllNotificationsReadSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Marked 3 notifications as read"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.markAllNotificationsRead()

        assertTrue(result is NetworkResult.Success)
        assertEquals("Marked 3 notifications as read", (result as NetworkResult.Success).data)
    }

    @Test
    fun testGetNotificationsError() = runBlocking {
        val json = """
            {
                "success": false,
                "message": "Unauthorized"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody(json))

        val result = repository.getNotifications()

        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(401, error.code)
        assertEquals("Unauthorized", error.message)
    }
}
