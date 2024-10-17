package org.example

import com.kognichain.actuators.SimpleActuator
import com.kognichain.core.Action
import com.kognichain.core.Agent
import com.kognichain.core.Task
import com.kognichain.decisionmakers.SimpleDecisionMaker
import com.kognichain.memories.SimpleMemory
import com.kognichain.sensors.SimpleSensor

fun main() {
    // Create a simple weather monitoring agent
    val weatherSensor = SimpleSensor("temperature", 25)
    val notificationActuator = SimpleActuator { params ->
        println("Weather notification: Temperature is ${params["temperature"]}°C")
    }

    val decisionMaker = SimpleDecisionMaker { input ->
        object : Action {
            override fun execute(): Result<Any> {
                val temperature = input["temperature"] as? Int ?: 0
                return if (temperature > 30) {
                    println("Temperature is above 30°C, notifying...")
                    notificationActuator.execute(input)
                } else {
                    println("Temperature is ${temperature}°C, no notification needed.")
                    Result.success(Unit)
                }
            }
            override fun cancel(): Boolean = true
        }
    }

    val memory = SimpleMemory()
    val weatherAgent = Agent("WeatherMonitor", memory, decisionMaker)

    val monitorWeatherTask = Task(
        name = "MonitorWeather",
        sensors = listOf(weatherSensor),
        actuators = listOf(notificationActuator)
    )

    weatherAgent.addTask(monitorWeatherTask)

    println("Executing agent with temperature 25°C")
    weatherAgent.execute()

    println("\nChanging temperature to 35°C")
    weatherSensor.setValue(35)
    weatherAgent.execute()
}