package com.example.iris_new.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AttentionController {

    private val _state =
        MutableStateFlow(AttentionState.FREE)

    val state: StateFlow<AttentionState> = _state

    fun lock() {
        _state.value = AttentionState.BUSY
    }

    fun release() {
        _state.value = AttentionState.FREE
    }
}
