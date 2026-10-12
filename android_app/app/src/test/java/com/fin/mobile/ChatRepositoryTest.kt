package com.fin.mobile

import com.fin.mobile.data.remote.NetworkConfig
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.repository.ChatRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: ChatRepositoryImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val config = NetworkConfig(
            baseUrl = mockWebServer.url("/").toString(),
            isDebug = false
        )
        val apiService = NetworkModule.createApiService(null, config)
        val moshi = NetworkModule.createMoshi()

        repository = ChatRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testSendMessageSuccessWithCitationsAndSchemes() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "reply": "Here are central and state schemes for students.",
                    "isGrounded": true,
                    "citations": [
                        {
                            "schemeName": "Ministry of Education Guidelines",
                            "source": "Official Portal",
                            "page": 1
                        }
                    ],
                    "schemes": [
                        {
                            "id": "pm-vidyalaxmi",
                            "title": "PM Vidyalaxmi",
                            "category": "Education"
                        }
                    ],
                    "suggestions": [
                        "What documents are required?",
                        "How to apply?"
                    ]
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.sendMessage("schemes for students")
        assertTrue("Expected Success result", result is NetworkResult.Success)

        val data = (result as NetworkResult.Success).data
        assertEquals("Here are central and state schemes for students.", data.reply)
        assertEquals(true, data.isGrounded)
        assertEquals(1, data.citations?.size)
        assertEquals("Ministry of Education Guidelines", data.citations?.first()?.schemeName)
        assertEquals(1, data.schemes?.size)
        assertEquals("pm-vidyalaxmi", data.schemes?.first()?.id)
        assertEquals(2, data.suggestions?.size)
    }

    @Test
    fun testSendMessageFailureHttp400() = runBlocking {
        val json = """
            {
                "success": false,
                "message": "message is required in request body"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody(json))

        val result = repository.sendMessage("")
        assertTrue("Expected Error result", result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals(400, error.code)
    }

    @Test
    fun testGetHistorySuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": [
                    {
                        "id": "conv-1",
                        "title": "Eligibility for PMEGP",
                        "createdAt": "2026-03-10T10:00:00.000Z",
                        "updatedAt": "2026-03-10T10:05:00.000Z",
                        "messageCount": 4,
                        "lastMessage": "You are eligible for PMEGP"
                    }
                ]
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getHistory()
        assertTrue("Expected Success result", result is NetworkResult.Success)
        val list = (result as NetworkResult.Success).data
        assertEquals(1, list.size)
        assertEquals("conv-1", list[0].id)
        assertEquals("Eligibility for PMEGP", list[0].title)
    }

    @Test
    fun testGetConversationDetailSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "id": "conv-1",
                    "userId": "user-123",
                    "title": "Eligibility for PMEGP",
                    "messages": [
                        {
                            "id": "msg-1",
                            "sender": "user",
                            "text": "Check my eligibility for PMEGP"
                        },
                        {
                            "id": "msg-2",
                            "sender": "assistant",
                            "text": "Based on your income, you are eligible for 25% subsidy."
                        }
                    ]
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getConversation("conv-1")
        assertTrue("Expected Success result", result is NetworkResult.Success)
        val conv = (result as NetworkResult.Success).data
        assertEquals("conv-1", conv.id)
        assertEquals(2, conv.messages.size)
        assertEquals("user", conv.messages[0].sender)
        assertEquals("assistant", conv.messages[1].sender)
    }

    @Test
    fun testDeleteConversationSuccess() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Conversation deleted successfully"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.deleteConversation("conv-1")
        assertTrue("Expected Success result", result is NetworkResult.Success)
    }
}
