package com.nforceone.nforcehq.attendance;

import java.util.UUID;

public record ClientView(UUID id, String name, String contactPerson, String notes) {
}
