package org.example.backend.watchLater;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchLaterRepository extends JpaRepository<WatchLater, WatchLaterID> {
    Page<WatchLaterView> findAllByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);
}
