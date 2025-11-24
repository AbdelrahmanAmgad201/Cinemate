package org.example.backend.requests;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestsRepository extends JpaRepository<Requests, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM Requests r " +
            "WHERE r.stateUpdatedAt < :cutoff " +
            "AND r.state IN ('ACCEPTED', 'REJECTED')")
    void deleteOldNonPending(LocalDateTime cutoff);

    List<Requests> findAllByState(State state);
    List<Requests> findAllByOrganization_Id(Long id);
}
