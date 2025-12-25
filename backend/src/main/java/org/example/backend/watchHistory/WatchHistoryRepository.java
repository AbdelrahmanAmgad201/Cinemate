package org.example.backend.watchHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

public interface WatchHistoryRepository extends CrudRepository<WatchHistory, Long> {
    Page<WatchHistoryViewDTO> findAllByUserIdAndIsDeletedFalse(Long watcherId, Pageable pageable);
}
