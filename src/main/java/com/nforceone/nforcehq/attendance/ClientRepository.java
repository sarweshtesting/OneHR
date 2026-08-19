package com.nforceone.nforcehq.attendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<Client> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
