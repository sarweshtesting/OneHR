package com.nforceone.nforcehq.org;

import java.util.UUID;

public record DepartmentSummary(UUID id, String name) {

    static DepartmentSummary from(Department department) {
        return new DepartmentSummary(department.getId(), department.getName());
    }
}
