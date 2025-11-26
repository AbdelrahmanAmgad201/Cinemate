package org.example.backend.requests;

import org.example.backend.organization.RequestsOverView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
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

    List<Requests> findByAdmin_Id(Long id);

    @Query("""
        SELECT new org.example.backend.organization.RequestsOverView(
            SUM(CASE WHEN r.state = 'PENDING' THEN 1 ELSE 0 END),
            SUM(CASE WHEN r.state = 'REJECTED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN r.state = 'ACCEPTED' THEN 1 ELSE 0 END)
        )
        FROM Requests r
        WHERE r.organization.id = :orgId
        """)
    RequestsOverView getRequestsOverviewByOrgId(@Param("orgId") Long orgId);
}
