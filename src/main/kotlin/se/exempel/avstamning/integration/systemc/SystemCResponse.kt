package se.exempel.avstamning.integration.systemc

import com.fasterxml.jackson.annotation.JsonProperty

data class SystemCResponse(
    @param:JsonProperty("local_reference") val localReference: String,
    @param:JsonProperty("business_reference") val businessReference: String,
    @param:JsonProperty("full_name") val fullName: String,
    @param:JsonProperty("email_address") val emailAddress: String,
)
