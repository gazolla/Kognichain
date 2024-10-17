package com.kognichain.memories;

import com.kognichain.core.Memory

class SimpleMemory : Memory {
    private val storage = mutableMapOf<String, Any>()

    override fun store(key: String, value: Any) {
        storage[key] = value
    }

    override fun retrieve(key: String): Any? = storage[key]

    override fun clear() {
        storage.clear()
    }
}