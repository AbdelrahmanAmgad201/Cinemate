
package org.example.backend.deletion;

import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(deletionService);
    }

    @Test
    void cleanupSoftDeletedData_CallsAllCollections() {
        doNothing().when(deletionService).hardDeleteOldEntities(anyString(), anyInt());

        cleanupScheduler.cleanupSoftDeletedData();

        verify(deletionService).hardDeleteOldEntities("forums", 30);
        verify(deletionService).hardDeleteOldEntities("posts", 30);
        verify(deletionService).hardDeleteOldEntities("comments", 30);
        verify(deletionService).hardDeleteOldEntities("votes", 30);
        verifyNoMoreInteractions(deletionService);
    }

    @Test
    void cleanupSoftDeletedData_CallsInCorrectOrder() {
        doNothing().when(deletionService).hardDeleteOldEntities(anyString(), anyInt());

        cleanupScheduler.cleanupSoftDeletedData();

        var inOrder = inOrder(deletionService);
        inOrder.verify(deletionService).hardDeleteOldEntities("forums", 30);
        inOrder.verify(deletionService).hardDeleteOldEntities("posts", 30);
        inOrder.verify(deletionService).hardDeleteOldEntities("comments", 30);
        inOrder.verify(deletionService).hardDeleteOldEntities("votes", 30);
    }

    @Test
    void cleanupSoftDeletedData_Uses30DaysForAll() {
        doNothing().when(deletionService).hardDeleteOldEntities(anyString(), anyInt());

        cleanupScheduler.cleanupSoftDeletedData();

        verify(deletionService, times(4)).hardDeleteOldEntities(anyString(), eq(30));
    }


    @Test
    void cleanupSoftDeletedData_AllCollections_CalledExactlyOnce() {
        doNothing().when(deletionService).hardDeleteOldEntities(anyString(), anyInt());

        cleanupScheduler.cleanupSoftDeletedData();

        verify(deletionService, times(1)).hardDeleteOldEntities("forums", 30);
        verify(deletionService, times(1)).hardDeleteOldEntities("posts", 30);
        verify(deletionService, times(1)).hardDeleteOldEntities("comments", 30);
        verify(deletionService, times(1)).hardDeleteOldEntities("votes", 30);
    }
}