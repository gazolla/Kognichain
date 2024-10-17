package com.kognichain.core

class Agent(
    val id: String,
    private val memory: Memory,
    private val decisionMaker: DecisionMaker
) {
    private val tasks: MutableList<Task> = mutableListOf()

    fun addTask(task: Task) {
        tasks.add(task)
    }

    fun execute() {
        tasks.forEach { task ->
            val sensorData = task.sensors.flatMap { it.sense().entries }.associate { it.key to it.value }
            val contextData = (memory.retrieve("context") as? Map<String, Any>) ?: emptyMap()
            val inputData = sensorData + contextData
            val action = decisionMaker.decide(inputData)
            val result = action.execute()
            memory.store("lastResult", result)
        }
    }
}