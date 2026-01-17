package org.uxstudio.contacts.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.uxstudio.contacts.model.Contact
import java.util.Optional

@Repository
interface ContactRepository : JpaRepository<Contact, Long> {
    fun findByEmail(email: String): Optional<Contact>
}
