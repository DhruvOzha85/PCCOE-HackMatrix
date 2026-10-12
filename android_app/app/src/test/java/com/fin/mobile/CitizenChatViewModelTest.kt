package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ChatCitationDto
import com.fin.mobile.data.remote.dto.ChatConversationDetailDto
import com.fin.mobile.data.remote.dto.ChatHistorySummaryDto
import com.fin.mobile.data.remote.dto.ChatMessageItemDto
import com.fin.mobile.data.remote.dto.ChatResponseDataDto
import com.fin.mobile.data.remote.dto.ChatTurnDto
import com.fin.mobile.data.remote.dto.SchemeDto
import com.fin.mobile.data.repository.ChatRepository
import com.fin.mobile.ui.chat.CitizenChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeChatRepository : ChatRepository {
    var sendResult: NetworkResult<ChatResponseDataDto> = NetworkResult.Success(
        ChatResponseDataDto(
            reply = "Here is statutory guidance.",
            isGrounded = true,
            citations = listOf(ChatCitationDto(schemeName = "PMEGP Guideline", source = "Ministry of MSME")),
            schemes = listOf(SchemeDto(id = "pmegp", title = "PMEGP")),
            suggestions = listOf("Check required documents")
        )
    )
    var historyResult: NetworkResult<List<ChatHistorySummaryDto>> = NetworkResult.Success(emptyList())
    var conversationResult: NetworkResult<ChatConversationDetailDto> = NetworkResult.Success(
        ChatConversationDetailDto(id = "conv-1", title = "PMEGP Query")
    )
    var deleteResult: NetworkResult<Unit> = NetworkResult.Success(Unit)

    override suspend fun sendMessage(
        message: String,
        history: List<ChatTurnDto>,
        conversationId: String?,
        language: String?
    ): NetworkResult<ChatResponseDataDto> = sendResult

    override suspend fun getHistory(): NetworkResult<List<ChatHistorySummaryDto>> = historyResult

    override suspend fun getConversation(id: String): NetworkResult<ChatConversationDetailDto> = conversationResult

    override suspend fun deleteConversation(id: String): NetworkResult<Unit> = deleteResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class CitizenChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeChatRepository
    private lateinit var viewModel: CitizenChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeChatRepository()
        viewModel = CitizenChatViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateHasWelcomeMessage() {
        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("assistant", state.messages[0].sender)
        assertTrue(state.messages[0].text.contains("Namaste"))
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun testSendMessageSuccess() = runTest {
        viewModel.onInputTextChanged("Tell me about PMEGP")
        viewModel.sendMessage()

        // Immediate state before coroutine completes: user message added, loading true
        val intermediateState = viewModel.uiState.value
        assertEquals(2, intermediateState.messages.size)
        assertEquals("user", intermediateState.messages[1].sender)
        assertEquals("Tell me about PMEGP", intermediateState.messages[1].text)
        assertTrue(intermediateState.isLoading)
        assertEquals("", intermediateState.inputText)

        advanceUntilIdle()

        // After completion: assistant message added, loading false
        val finalState = viewModel.uiState.value
        assertEquals(3, finalState.messages.size)
        assertEquals("assistant", finalState.messages[2].sender)
        assertEquals("Here is statutory guidance.", finalState.messages[2].text)
        assertEquals(1, finalState.messages[2].citations.size)
        assertEquals(1, finalState.messages[2].schemes.size)
        assertFalse(finalState.isLoading)
        assertNull(finalState.errorMessage)
    }

    @Test
    fun testSendMessageNetworkErrorAndRetry() = runTest {
        fakeRepository.sendResult = NetworkResult.Error(500, "Backend unavailable")

        viewModel.onInputTextChanged("Will fail query")
        viewModel.sendMessage()
        advanceUntilIdle()

        val stateAfterError = viewModel.uiState.value
        assertFalse(stateAfterError.isLoading)
        assertNotNull(stateAfterError.errorMessage)
        val lastMsg = stateAfterError.messages.last()
        assertTrue(lastMsg.isError)

        // Now test retry with success
        fakeRepository.sendResult = NetworkResult.Success(
            ChatResponseDataDto(reply = "Success after retry", isGrounded = true)
        )
        viewModel.retryLastMessage()
        advanceUntilIdle()

        val stateAfterRetry = viewModel.uiState.value
        assertFalse(stateAfterRetry.isLoading)
        val successfulMsg = stateAfterRetry.messages.last()
        assertEquals("Success after retry", successfulMsg.text)
        assertFalse(successfulMsg.isError)
    }

    @Test
    fun testStartNewConversationResetsState() = runTest {
        viewModel.onInputTextChanged("First message")
        viewModel.sendMessage()
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.messages.size)

        viewModel.startNewConversation()
        val newState = viewModel.uiState.value
        assertEquals(1, newState.messages.size)
        assertNull(newState.conversationId)
        assertEquals("", newState.inputText)
    }

    @Test
    fun testSelectConversationLoadsSavedMessages() = runTest {
        fakeRepository.conversationResult = NetworkResult.Success(
            ChatConversationDetailDto(
                id = "saved-conv-42",
                title = "Historical PMEGP query",
                messages = listOf(
                    ChatMessageItemDto(id = "m1", sender = "user", text = "Old query"),
                    ChatMessageItemDto(id = "m2", sender = "assistant", text = "Old response")
                )
            )
        )

        viewModel.selectConversation("saved-conv-42")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("saved-conv-42", state.conversationId)
        assertEquals(2, state.messages.size)
        assertEquals("Old query", state.messages[0].text)
        assertEquals("Old response", state.messages[1].text)
    }
}
