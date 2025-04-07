package org.shvedchikov.domidzebot.repository;

import org.shvedchikov.domidzebot.model.House;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface HouseRepository extends JpaRepository<House, Long> {
    Optional<House> findHouseByOwnerIdAndNumber(Long ownerId, Integer number);
    List<House> findAllByOwnerId(Long ownerId);

    @NativeQuery(value = """
            SELECT
                h.number AS NUMBER,
                creds.login AS LOGIN,
                creds.password AS PWD,
                domain.domain AS DOMAIN,
                users.email AS EMAIL
            FROM houses AS h
            INNER JOIN credentials AS creds
                ON h.credential_id = creds.id
            INNER JOIN domains AS domain
                ON h.domain_id = domain.id
            INNER JOIN users
                ON h.owner_id = users.id
            WHERE users.id = ?1""")
    List<Map<String, Object>> findAllByOwner(Long userId);
}
