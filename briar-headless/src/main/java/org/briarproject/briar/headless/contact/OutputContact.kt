package org.briarproject.briar.headless.contact

import org.briarproject.bramble.api.contact.Contact
import org.briarproject.bramble.identity.OutputAuthor
import org.briarproject.briar.headless.output
import javax.annotation.concurrent.Immutable

@Immutable
internal data class OutputContact(
    val id: Int,
    val author: OutputAuthor,
    val verified: Boolean
) {
    internal constructor(c: Contact) : this(
        id = c.id.int,
        author = c.author.output(),
        verified = c.isVerified
    )
}
