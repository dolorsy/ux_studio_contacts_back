package org.uxstudio.contacts.service

import io.minio.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

@Service
class FileStorageService(
    private val minioClient: MinioClient,
    @Value("\${minio.bucket-name}")
    private val bucketName: String,
    @Value("\${minio.public-url}")
    private val publicUrl: String
) {
    fun initializeBucket() {
        try {
            val found = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            )
            if (!found) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                )
            }
            
            // Set bucket policy to allow public read access
            val publicReadPolicy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {"AWS": ["*"]},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::${bucketName}/*"]
                        }
                    ]
                }
            """.trimIndent()
            
            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(publicReadPolicy)
                    .build()
            )
        } catch (e: Exception) {
            throw RuntimeException("Error initializing MinIO bucket", e)
        }
    }

    fun uploadFile(fileName: String, fileStream: InputStream, contentType: String): String {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(fileName)
                    .stream(fileStream, fileStream.available().toLong(), -1)
                    .contentType(contentType)
                    .build()
            )
            return fileName
        } catch (e: Exception) {
            throw RuntimeException("Error uploading file to MinIO", e)
        }
    }

    fun uploadFile(fileName: String, file: MultipartFile): String {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(fileName)
                    .stream(file.inputStream, file.size, -1)
                    .contentType(file.contentType ?: "application/octet-stream")
                    .build()
            )
            return fileName
        } catch (e: Exception) {
            throw RuntimeException("Error uploading file", e)
        }
    }

    fun uploadFile(file: MultipartFile): String {
        try {
            val fileName = file.originalFilename ?: throw IllegalArgumentException("File name is required")
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(fileName)
                    .stream(file.inputStream, file.size, -1)
                    .contentType(file.contentType ?: "application/octet-stream")
                    .build()
            )
            return fileName
        } catch (e: Exception) {
            throw RuntimeException("Error uploading file", e)
        }
    }

    fun downloadFile(fileName: String): InputStream {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(fileName)
                    .build()
            )
        } catch (e: Exception) {
            throw RuntimeException("Error downloading file from MinIO", e)
        }
    }

    fun deleteFile(fileName: String) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(fileName)
                    .build()
            )
        } catch (e: Exception) {
            throw RuntimeException("Error deleting file from MinIO", e)
        }
    }
    
    fun getPublicUrl(fileName: String?): String? {
        return fileName?.let {
            // If it's already a full URL, return it as is
            if (it.startsWith("http://") || it.startsWith("https://")) {
                return it
            }
            // Otherwise, convert object path to public URL
            // Remove leading slash if present
            println(publicUrl)
            val cleanFileName = it.removePrefix("/")
            "$publicUrl/$bucketName/$cleanFileName"
        }
    }
    
    fun extractObjectPath(pictureValue: String?): String? {
        return pictureValue?.let {
            // If it's a URL, extract the object path
            if (it.startsWith("http://") || it.startsWith("https://")) {
                // Extract path after bucket name: http://host:port/bucket-name/path -> path
                val pathAfterBucket = it.substringAfter("/$bucketName/", "")
                if (pathAfterBucket.isNotEmpty()) pathAfterBucket else it.substringAfterLast("/", it)
            } else {
                // Already an object path
                it
            }
        }
    }
}
