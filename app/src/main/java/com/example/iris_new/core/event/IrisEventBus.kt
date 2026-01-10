package com.example.iris_new.core.event

import kotlinx.coroutines.flow.MutableSharedFlow

object IrisEventBus {

    private val _events = MutableSharedFlow<IrisEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    val events = _events

    suspend fun publish(event: IrisEvent) {
        _events.emit(event)
    }
}