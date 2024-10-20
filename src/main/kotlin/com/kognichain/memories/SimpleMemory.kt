package com.kognichain.memories

import com.kognichain.core.Memory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SimpleMemory : Memory {
    private val storage = mutableMapOf<String, Any>()
    private val mutex = Mutex()

    override suspend fun store(key: String, value: Any) {
        mutex.withLock {
            storage[key] = value
        }
    }

    override suspend fun retrieve(key: String): Any? {
        return mutex.withLock {
            storage[key]
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            storage.clear()
        }
    }
}
