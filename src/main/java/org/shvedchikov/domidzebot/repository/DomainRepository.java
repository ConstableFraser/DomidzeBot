package org.shvedchikov.domidzebot.repository;

import org.shvedchikov.domidzebot.model.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {
    Domain findDomainByDomain(String domainName);
}
