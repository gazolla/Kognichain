package com.kognichain.tasks;

import com.kognichain.core.Task
import java.util.Properties
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class SaveFileTask : Task {

    private fun loadProperties(): Properties {
        val properties = Properties()
        val inputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("application.properties")
        inputStream?.use {
            properties.load(it)
        }
        return properties
    }

    override fun execute(input: Map<String, Any>): Result<String> {

        val path = input["path"] as? String ?: loadProperties().getProperty("savefile.path") ?: System.getenv("SAVEFILE_PATH")
        val filename = input["filename"] as? String
        val content = input["content"] as? String

        return if (path != null && filename != null && content != null) {
            try {
                val filePath = Paths.get(path, filename)
                Files.write(filePath, content.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                Result.success("File saved successfully at ${filePath.toAbsolutePath()}")
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(IllegalArgumentException("Invalid input parameters"))
        }
    }
}
