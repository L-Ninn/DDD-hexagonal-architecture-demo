package se.exempel.avstamning.integration.systemb

data class SystemBResponse(
    val recordId: String,
    val customer: SystemBCustomer,
)

data class SystemBCustomer(
    val number: String,
    val displayName: String,
    val contact: SystemBContact,
)

data class SystemBContact(val emailAddress: String)
