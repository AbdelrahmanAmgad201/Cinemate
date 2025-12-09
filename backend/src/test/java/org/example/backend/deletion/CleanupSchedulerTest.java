package org.example.backend.deletion;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class CleanupSchedulerTest {

    @Test
    void testCleanupSoftDeletedData() {
        // Arrange
        CascadeDeletionService deletionService = mock(CascadeDeletionService.class);
        CleanupScheduler scheduler = new CleanupScheduler(deletionService);

        // Act
        scheduler.cleanupSoftDeletedData();

        // Assert
        verify(deletionService, times(1)).hardDeleteOldEntities("forums", 30);
        verify(deletionService, times(1)).hardDeleteOldEntities("posts", 30);
        verify(deletionService, times(1)).hardDeleteOldEntities("comments", 30);
        verify(deletionService, times(1)).hardDeleteOldEntities("votes", 30);

        verifyNoMoreInteractions(deletionService);
    }
}
