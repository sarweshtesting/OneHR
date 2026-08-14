package com.nforceone.nforcehq.org;

import java.util.UUID;

public record OrganizationSummary(UUID id, String name, String slug) {

    static OrganizationSummary from(Organization organization) {
        return new OrganizationSummary(organization.getId(), organization.getName(), organization.getSlug());
    }
}
