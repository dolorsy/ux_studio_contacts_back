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
    private val bucketName: String
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
}
