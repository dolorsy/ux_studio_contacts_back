package org.uxstudio.contacts.config

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.uxstudio.contacts.service.FileStorageService

@Component
class MinIOInitializer(
    private val fileStorageService: FileStorageService
) : CommandLineRunner {
    override fun run(vararg args: String) {
        fileStorageService.initializeBucket()
    }
}
