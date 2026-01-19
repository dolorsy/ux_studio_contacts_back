package org.uxstudio.contacts.controller

import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.uxstudio.contacts.service.FileStorageService

/**
 * Controller for serving files from RustFS S3-compatible storage.
 * Files are retrieved from the RustFS bucket and served through the Spring Boot application.
 * 
 * Example URL: GET /api/files/contacts/uuid.jpg
 */
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = ["*"])
class FileController(
    private val fileStorageService: FileStorageService
) {
    /**
     * Get a file from RustFS S3-compatible storage by name and serve it through the application.
     * 
     * @param fileName The file name/path in the bucket (e.g., "contacts/uuid.jpg")
     * @return File stream with appropriate content type headers
     */
    @GetMapping("/{*fileName}")
    fun getFile(@PathVariable fileName: String): ResponseEntity<InputStreamResource> {
        return try {
            // Remove leading slash if present (from path variable)
            val cleanFileName = fileName.trimStart('/')
            // Download file from RustFS bucket
            val fileStream = fileStorageService.downloadFile(cleanFileName)
            val contentType = determineContentType(cleanFileName)
            val resource = InputStreamResource(fileStream)
            
            val headers = HttpHeaders().apply {
                setContentType(MediaType.parseMediaType(contentType))
                setContentDispositionFormData("inline", cleanFileName.substringAfterLast("/"))
                cacheControl = "public, max-age=3600" // Cache for 1 hour
            }
            
            ResponseEntity(resource, headers, HttpStatus.OK)
        } catch (e: Exception) {
            // Log error for debugging
            val cleanFileName = fileName.trimStart('/')
            println("Error retrieving file '$cleanFileName': ${e.message}")
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
    
    private fun determineContentType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> "application/octet-stream"
        }
    }
}
