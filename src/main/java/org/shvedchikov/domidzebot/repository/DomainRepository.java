package org.shvedchikov.domidzebot.repository;

import lombok.NonNull;
import org.shvedchikov.domidzebot.model.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {
    Domain findDomainByDomain(String domainName);

    @Override
    @NonNull
    List<Domain> findAll();
}
