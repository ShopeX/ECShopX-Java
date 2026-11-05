package cn.shopex.ecshopx.orders.service.finish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.service.systemtag.SleepingUserTagReconcileService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinishOrderJobServiceTest {

	@Mock
	private FinishOrderBatchService finishOrderBatchService;

	@Mock
	private SleepingUserTagReconcileService sleepingUserTagReconcileService;

	@InjectMocks
	private FinishOrderJobService service;

	@Test
	void execute_runsSleepingAfterBatch() {
		when(finishOrderBatchService.runEquivalentToFinishOrderJobHandle()).thenReturn(3);

		int n = service.execute(Map.of());

		assertEquals(3, n);
		InOrder order = inOrder(finishOrderBatchService, sleepingUserTagReconcileService);
		order.verify(finishOrderBatchService).runEquivalentToFinishOrderJobHandle();
		order.verify(sleepingUserTagReconcileService).executeDaily();
	}

	@Test
	void execute_batchFailure_skipsSleeping() {
		when(finishOrderBatchService.runEquivalentToFinishOrderJobHandle())
				.thenThrow(new IllegalStateException("batch boom"));

		assertThrows(IllegalStateException.class, () -> service.execute(Map.of()));
		verify(sleepingUserTagReconcileService, never()).executeDaily();
	}

	@Test
	void execute_sleepingFailure_doesNotFailJob() {
		when(finishOrderBatchService.runEquivalentToFinishOrderJobHandle()).thenReturn(1);
		doThrow(new RuntimeException("sleep boom")).when(sleepingUserTagReconcileService).executeDaily();

		assertDoesNotThrow(() -> assertEquals(1, service.execute(Map.of())));
		verify(sleepingUserTagReconcileService).executeDaily();
	}
}
