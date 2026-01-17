package org.uxstudio.contacts.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.uxstudio.contacts.model.Contact
import org.uxstudio.contacts.repository.ContactRepository
import java.util.UUID

@Service
@Transactional
class ContactService(
    private val contactRepository: ContactRepository,
    private val fileStorageService: FileStorageService
) {
    fun createContact(contact: Contact, pictureFile: MultipartFile?): Contact {
        // Check if email already exists
        val existingContact = contactRepository.findByEmail(contact.email)
        if (existingContact.isPresent) {
            throw IllegalArgumentException("Contact with email ${contact.email} already exists")
        }
        
        // Handle picture upload
        if (pictureFile != null && !pictureFile.isEmpty) {
            val fileName = generateFileName(pictureFile.originalFilename ?: "picture")
            contact.picture = fileStorageService.uploadFile(fileName, pictureFile)
        }
        
        return contactRepository.save(contact)
    }

    fun getAllContacts(): List<Contact> {
        return contactRepository.findAll()
    }

    fun getContactById(id: Long): java.util.Optional<Contact> {
        return contactRepository.findById(id)
    }

    fun updateContact(id: Long, contactDetails: Contact, pictureFile: MultipartFile?): Contact {
        val contact = contactRepository.findById(id)
            .orElseThrow { RuntimeException("Contact not found with id: $id") }

        // Check if email is being changed and if new email already exists
        if (contact.email != contactDetails.email) {
            val existingContact = contactRepository.findByEmail(contactDetails.email)
            if (existingContact.isPresent) {
                throw IllegalArgumentException("Contact with email ${contactDetails.email} already exists")
            }
        }

        contact.name = contactDetails.name
        contact.email = contactDetails.email
        contact.phone = contactDetails.phone

        // Handle picture upload/update
        if (pictureFile != null && !pictureFile.isEmpty) {
            // Delete old picture if exists
            contact.picture?.let { oldPicture ->
                try {
                    fileStorageService.deleteFile(oldPicture)
                } catch (e: Exception) {
                    // Log error but continue with update
                    println("Error deleting old picture: ${e.message}")
                }
            }
            
            // Upload new picture
            val fileName = generateFileName(pictureFile.originalFilename ?: "picture")
            contact.picture = fileStorageService.uploadFile(fileName, pictureFile)
        }

        return contactRepository.save(contact)
    }

    fun deleteContact(id: Long) {
        val contact = contactRepository.findById(id)
            .orElseThrow { RuntimeException("Contact not found with id: $id") }
        
        // Delete picture from bucket if exists
        contact.picture?.let { picture ->
            try {
                fileStorageService.deleteFile(picture)
            } catch (e: Exception) {
                // Log error but continue with deletion
                println("Error deleting picture: ${e.message}")
            }
        }
        
        contactRepository.delete(contact)
    }
    
    private fun generateFileName(originalFilename: String): String {
        val extension = originalFilename.substringAfterLast('.', "")
        val uuid = UUID.randomUUID().toString()
        return if (extension.isNotEmpty()) {
            "contacts/$uuid.$extension"
        } else {
            "contacts/$uuid.jpg"
        }
    }
}
