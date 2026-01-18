package org.uxstudio.contacts.controller

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.uxstudio.contacts.model.Contact
import org.uxstudio.contacts.service.ContactService

@RestController
@RequestMapping("/api/contacts")
@CrossOrigin(origins = ["*"])
class ContactController(
    private val contactService: ContactService
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createContact(
        @RequestParam("name") name: String,
        @RequestParam("email") email: String,
        @RequestParam("phone", required = false) phone: String?,
        @RequestParam("picture", required = false) picture: MultipartFile?
        @RequestParam("isFavourite", required = false, defaultValue = "false") isFavourite: Boolean
    ): ResponseEntity<Contact> {
        if (name.isBlank()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        if (email.isBlank() || !email.contains("@")) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        
        val contact = Contact().apply {
            this.name = name
            this.email = email
            this.phone = phone
            this.isFavourite = isFavourite
        }
        val createdContact = contactService.createContact(contact, picture)
        return ResponseEntity(createdContact, HttpStatus.CREATED)
    }

    @GetMapping
    fun getAllContacts(): ResponseEntity<List<Contact>> {
        val contacts = contactService.getAllContacts()
        return ResponseEntity(contacts, HttpStatus.OK)
    }

    @GetMapping("/{id}")
    fun getContactById(@PathVariable id: Long): ResponseEntity<Contact> {
        return contactService.getContactById(id)
            .map { contact -> ResponseEntity(contact, HttpStatus.OK) }
            .orElse(ResponseEntity(HttpStatus.NOT_FOUND))
    }

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateContact(
        @PathVariable id: Long,
        @RequestParam("name") name: String,
        @RequestParam("email") email: String,
        @RequestParam("phone", required = false) phone: String?,
        @RequestParam("picture", required = false) picture: MultipartFile?
        @RequestParam("isFavourite", required = false) isFavourite: Boolean?
    ): ResponseEntity<Contact> {
        if (name.isBlank()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        if (email.isBlank() || !email.contains("@")) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        
        return try {
            val contact = Contact().apply {
                this.name = name
                this.email = email
                this.phone = phone
                this.isFavourite = isFavourite ?: false
            }
            val updatedContact = contactService.updateContact(id, contact, picture)
            ResponseEntity(updatedContact, HttpStatus.OK)
        } catch (e: RuntimeException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        } catch (e: IllegalArgumentException) {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteContact(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            contactService.deleteContact(id)
            ResponseEntity(HttpStatus.NO_CONTENT)
        } catch (e: RuntimeException) {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }
}
