package com.kognichain.tasks

import com.kognichain.core.Task
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.Properties

class PdfGeneratorTask : Task {
    companion object {
        private const val DEFAULT_FONT_SIZE = 12f
        private const val MAX_CHARS_PER_LINE = 90
        private const val MARGIN_X = 50f
        private const val MARGIN_Y = 750f
        private const val LINE_HEIGHT = 15f
        private const val MAX_CONTENT_LENGTH = 100000
    }

    private data class PdfConfig(
        val path: String,
        val filename: String,
        val content: String,
        val fontSize: Float = DEFAULT_FONT_SIZE
    )

    private fun loadProperties(): Properties {
        return Properties().apply {
            Thread.currentThread().contextClassLoader
                .getResourceAsStream("application.properties")
                ?.use { load(it) }
        }
    }

    private fun validateInput(input: Map<String, Any>): PdfConfig? {
        val path = input["path"] as? String
            ?: loadProperties().getProperty("pdf.save.path")
            ?: System.getenv("PDF_SAVE_PATH")
            ?: return null

        val filename = input["filename"] as? String ?: return null
        val content = input["content"] as? String ?: return null

        if (content.isEmpty() || content.length > MAX_CONTENT_LENGTH) {
            return null
        }

        val fontSize = input["fontSize"] as? Float ?: DEFAULT_FONT_SIZE
        return PdfConfig(path, filename, content, fontSize)
    }

    private fun createPdfDocument(config: PdfConfig): PDDocument {
        val document = PDDocument()
        val lines = splitContentIntoLines(config.content)
        var currentPage = PDPage().also { document.addPage(it) }
        var yPosition = MARGIN_Y

        PDPageContentStream(document, currentPage).use { stream ->
            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, config.fontSize)
            stream.newLineAtOffset(MARGIN_X, yPosition)

            for (line in lines) {
                if (yPosition < 50f) {
                    stream.endText()
                    currentPage = PDPage().also { document.addPage(it) }
                    PDPageContentStream(document, currentPage).use { newStream ->
                        newStream.beginText()
                        newStream.setFont(PDType1Font.HELVETICA, config.fontSize)
                        yPosition = MARGIN_Y
                        newStream.newLineAtOffset(MARGIN_X, yPosition)
                        newStream.showText(line)
                    }
                } else {
                    stream.showText(line)
                }
                yPosition -= LINE_HEIGHT
                stream.newLineAtOffset(0f, -LINE_HEIGHT)
            }
            stream.endText()
        }

        return document
    }

    private fun splitContentIntoLines(content: String): List<String> {
        return content.split("\n").flatMap { paragraph ->
            if (paragraph.length <= MAX_CHARS_PER_LINE) {
                listOf(paragraph)
            } else {
                paragraph.chunked(MAX_CHARS_PER_LINE)
            }
        }
    }

    override fun execute(input: Map<String, Any>): kotlin.Result<String> {
        return try {
            val config = validateInput(input) ?:
            return kotlin.Result.failure(IllegalArgumentException("Invalid input parameters"))

            val filePath = Paths.get(config.path, "${config.filename}.pdf")
            Files.createDirectories(filePath.parent)

            createPdfDocument(config).use { document ->
                document.save(Files.newOutputStream(filePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ))
            }

            kotlin.Result.success("PDF saved successfully at ${filePath.toAbsolutePath()}")
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}