package cn.shopex.ecshopx.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.promotions.domain.TurntablePrizeDayStock;
import cn.shopex.ecshopx.promotions.mapper.TurntablePrizeDayStockMapper;
import cn.shopex.ecshopx.promotions.mapper.TurntableUserCountMapper;
import cn.shopex.ecshopx.promotions.mapper.TurntableUserDayCountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TurntableReserveServicesBe04Test {

	@Mock private TurntableUserCountMapper userCountMapper;
	@Mock private TurntableUserDayCountMapper userDayCountMapper;
	@Mock private TurntablePrizeDayStockMapper prizeDayStockMapper;

	private TurntableCountReserveService countReserveService;
	private TurntablePrizeDayStockReserveService stockReserveService;

	@BeforeEach
	void setUp() {
		countReserveService = new TurntableCountReserveService(userCountMapper, userDayCountMapper);
		stockReserveService = new TurntablePrizeDayStockReserveService(prizeDayStockMapper);
	}

	@Test
	void countReserve_successWhenCasHits() {
		when(userCountMapper.casIncrTotal(anyLong(), anyLong(), anyLong(), anyLong(), anyInt())).thenReturn(1);
		when(userDayCountMapper.casIncrDay(anyLong(), anyLong(), anyLong(), anyString(), anyLong(), anyInt()))
				.thenReturn(1);

		var r = countReserveService.reserve(1L, 2L, 3L, "2026-09-02", 10L, 5L);
		assertThat(r.ok()).isTrue();
	}

	@Test
	void countReserve_rollsBackTotalWhenDayFails() {
		when(userCountMapper.casIncrTotal(anyLong(), anyLong(), anyLong(), anyLong(), anyInt())).thenReturn(1);
		when(userDayCountMapper.casIncrDay(anyLong(), anyLong(), anyLong(), anyString(), anyLong(), anyInt()))
				.thenReturn(0);
		when(userDayCountMapper.insert(any(cn.shopex.ecshopx.promotions.domain.TurntableUserDayCount.class)))
				.thenThrow(new org.springframework.dao.DuplicateKeyException("dup"));
		when(userCountMapper.casDecrTotal(anyLong(), anyLong(), anyLong(), anyInt())).thenReturn(1);

		var r = countReserveService.reserve(1L, 2L, 3L, "2026-09-02", 10L, 5L);
		assertThat(r.ok()).isFalse();
		assertThat(r.dayFailed()).isTrue();
		verify(userCountMapper).casDecrTotal(eq(1L), eq(2L), eq(3L), anyInt());
	}

	@Test
	void stockReserve_zeroDailyStockAlwaysFails() {
		assertThat(stockReserveService.reserve(1L, "p1", "2026-09-02", 0)).isFalse();
		verify(prizeDayStockMapper, never()).casReserve(anyLong(), anyString(), anyString(), anyInt(), anyInt());
		verify(prizeDayStockMapper, never()).insert(any(TurntablePrizeDayStock.class));
	}

	@Test
	void stockReserve_insertsWhenNoRow() {
		when(prizeDayStockMapper.casReserve(anyLong(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(0);
		when(prizeDayStockMapper.insert(any(TurntablePrizeDayStock.class))).thenReturn(1);
		assertThat(stockReserveService.reserve(1L, "p1", "2026-09-02", 3)).isTrue();
		verify(prizeDayStockMapper).insert(any(TurntablePrizeDayStock.class));
	}
}
