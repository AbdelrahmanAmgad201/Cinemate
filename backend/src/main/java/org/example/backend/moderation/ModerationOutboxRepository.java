package org.example.backend.moderation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModerationOutboxRepository extends JpaRepository<ModerationOutboxEntry, Long> {

    /** Oldest-first (BIGINT identity is monotonic) so the relay preserves enqueue order. */
    List<ModerationOutboxEntry> findAllByOrderByIdAsc(Pageable pageable);
}
