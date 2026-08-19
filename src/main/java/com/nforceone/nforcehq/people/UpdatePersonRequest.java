package com.nforceone.nforcehq.people;

/** Both fields are optional — send only the one(s) being changed. */
public record UpdatePersonRequest(String role, Boolean active) {
}
