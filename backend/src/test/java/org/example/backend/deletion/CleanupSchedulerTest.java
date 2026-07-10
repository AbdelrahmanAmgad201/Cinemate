package org.example.backend.deletion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CleanupSchedulerTest {

    @Mock
    private CascadeDeletionService deletionService;

    @InjectMocks
    private CleanupScheduler cleanupScheduler;

    @Test
    void cleanupSoftDeletedData_PurgesAllFourCollectionsWithThirtyDayRetentionInOrder() {
        cleanupScheduler.cleanupSoftDeletedData();

        var inOrder = inOrder(deletionService);
        inOrder.verify(deletionService).hardDeleteOldEntities("forums", 30);
        inOrder.verify(deletionService).hardDeleteOldEntities("posts", 30);
        inOrder.verify(deletionService).hardDeleteOldEntities("comments", 30);
        inOrder.verify(deletionService).hardDeleteOldEntities("votes", 30);
        verifyNoMoreInteractions(deletionService);
    }
}
