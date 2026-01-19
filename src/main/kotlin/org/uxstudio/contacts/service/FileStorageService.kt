package org.uxstudio.contacts.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.InputStream
import java.net.URL

@Service
class FileStorageService(
    private val s3Client: S3Client,
    @Value("\${s3.bucket-name}")
    private val bucketName: String,
    @Value("\${s3.public-url}")
    private val publicUrl: String
) {
    
    fun initializeStorage() {
        try {
            // Check if bucket exists
            val headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build()
            
            try {
                s3Client.headBucket(headBucketRequest)
                println("Bucket '$bucketName' already exists")
            } catch (e: NoSuchBucketException) {
                // Bucket doesn't exist, create it
                val createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build()
                
                s3Client.createBucket(createBucketRequest)
                println("Created bucket '$bucketName'")
            }
        } catch (e: Exception) {
            throw RuntimeException("Error initializing S3 bucket", e)
        }
    }
    
    fun uploadFile(fileName: String, file: MultipartFile) {
        try {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.contentType)
                .build()
            
            s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(file.inputStream, file.size)
            )
        } catch (e: Exception) {
            throw RuntimeException("Error uploading file '$fileName'", e)
        }
    }
    
    fun downloadFile(fileName: String): InputStream {
        try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build()
            
            return s3Client.getObject(getObjectRequest)
        } catch (e: NoSuchKeyException) {
            throw RuntimeException("File '$fileName' not found", e)
        } catch (e: Exception) {
            throw RuntimeException("Error downloading file '$fileName'", e)
        }
    }
    
    fun deleteFile(fileName: String) {
        try {
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build()
            
            s3Client.deleteObject(deleteObjectRequest)
        } catch (e: Exception) {
            throw RuntimeException("Error deleting file '$fileName'", e)
        }
    }
    
    fun getPublicUrl(fileName: String?): String? {
        if (fileName.isNullOrBlank()) {
            return null
        }
        
        // If it's already a URL, return it as is
        if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
            return fileName
        }
        
        // Otherwise, construct the public URL
        val baseUrl = publicUrl.trimEnd('/')
        val key = fileName.trimStart('/')
        return "$baseUrl/$bucketName/$key"
    }
    
    fun extractObjectPath(url: String?): String? {
        if (url.isNullOrBlank()) {
            return null
        }
        
        // If it's already a path (not a URL), return it
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return url
        }
        
        // Extract the object path from URL
        // URL format: http://host:port/bucket-name/object-path
        try {
            val urlObj = URL(url)
            val path = urlObj.path
            // Remove leading slash and bucket name
            val parts = path.trimStart('/').split('/', limit = 2)
            if (parts.size == 2) {
                return parts[1] // Return the object path after bucket name
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }
}
