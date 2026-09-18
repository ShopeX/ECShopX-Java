package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.TurntableLog;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableDrawStatus;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableProcessStep;
import cn.shopex.ecshopx.promotions.mapper.TurntableLogMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TurntableDrawRecoverServiceTest {

	@Mock private TurntableLogMapper turntableLogMapper;
	@Mock private TurntableFrontJoinTurntableService joinService;

	private TurntableDrawRecoverService service;

	@BeforeEach
	void setUp() {
		service = new TurntableDrawRecoverService(turntableLogMapper, joinService);
	}

	@Test
	void recoverOne_delegatesToJoinResume() {
		TurntableLog log = new TurntableLog();
		log.setId(9L);
		log.setStatus(TurntableDrawStatus.PROCESSING);
		log.setProcessStep(TurntableProcessStep.CREATED);
		when(joinService.resumeProcessing(log)).thenReturn(true);

		assertThat(service.recoverOne(log)).isTrue();
		verify(joinService).resumeProcessing(log);
	}

	@Test
	void recoverOne_skipsNonProcessing() {
		TurntableLog log = new TurntableLog();
		log.setStatus(TurntableDrawStatus.SUCCESS);
		assertThat(service.recoverOne(log)).isFalse();
		verify(joinService, never()).resumeProcessing(any());
	}

	@Test
	void recoverStuck_scansLimitedBatch() {
		TurntableLog log = new TurntableLog();
		log.setId(1L);
		log.setStatus(TurntableDrawStatus.PROCESSING);
		when(turntableLogMapper.selectStuckProcessing(anyInt(), anyInt())).thenReturn(List.of(log));
		when(joinService.resumeProcessing(log)).thenReturn(true);

		assertThat(service.recoverStuck(60)).isEqualTo(1);
		verify(turntableLogMapper).selectStuckProcessing(anyInt(), anyInt());
	}
}
