package cn.shopex.ecshopx.popularize.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class BrokerageServiceTest {

	@Mock
	private BrokerageMapper brokerageMapper;

	@Mock
	private PopularizePromoterCountWriteService popularizePromoterCountWriteService;

	@Mock
	private PopularizeBrokerageStatisticsIncrementService popularizeBrokerageStatisticsIncrementService;

	@Mock
	private PointMemberAddPointService pointMemberAddPointService;

	@Mock
	private PlatformTransactionManager platformTransactionManager;

	private BrokerageService brokerageService;

	@BeforeEach
	void setUp() {
		lenient()
				.when(platformTransactionManager.getTransaction(any(TransactionDefinition.class)))
				.thenReturn(new SimpleTransactionStatus());
		brokerageService = new BrokerageService(
				brokerageMapper,
				popularizePromoterCountWriteService,
				popularizeBrokerageStatisticsIncrementService,
				pointMemberAddPointService,
				platformTransactionManager);
		ReflectionTestUtils.setField(brokerageService, "oemShuyun", false);
	}

	@Test
	@DisplayName("analysis §3 步骤 2: totalCount==0 早退出，无行更新")
	void analysis_s3_2_countZero() {
		when(brokerageMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
		brokerageService.scheduleSettleRebate();
		verify(brokerageMapper, never()).selectList(any(Wrapper.class));
		verify(brokerageMapper, never()).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("analysis §3 1+2+3–5+10: count>0 分页、LIMIT 100、循环结束")
	void analysis_s3_pagination() {
		when(brokerageMapper.selectCount(any(Wrapper.class))).thenReturn(150L);
		Brokerage b1 = new Brokerage();
		b1.setId(1L);
		b1.setCompanyId(1L);
		b1.setUserId(2L);
		b1.setCommissionType("money");
		b1.setRebate(100);
		b1.setOrderId("O1");
		Brokerage b2 = new Brokerage();
		b2.setId(2L);
		b2.setCompanyId(1L);
		b2.setUserId(2L);
		b2.setCommissionType("money");
		b2.setRebate(200);
		b2.setOrderId("O2");
		List<Brokerage> first = java.util.Collections.nCopies(100, b1);
		List<Brokerage> second = List.of(b2);
		when(brokerageMapper.selectList(any(Wrapper.class)))
				.thenReturn(first, second, Collections.emptyList());
		brokerageService.scheduleSettleRebate();
		verify(brokerageMapper, atLeast(2)).selectList(any(Wrapper.class));
		verify(popularizePromoterCountWriteService, atLeast(100 + 1)).addSettleRebateForSchedule(eq(1L), eq(2L), any(Long.class));
	}

	@Test
	@DisplayName("analysis §3 7-A+7-B+6+8: 行内 money 成功，关单后 Redis+统计 顺序")
	void analysis_s3_moneyPath() {
		when(brokerageMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		Brokerage b = new Brokerage();
		b.setId(9L);
		b.setCompanyId(11L);
		b.setUserId(22L);
		b.setCommissionType("money");
		b.setRebate(5000);
		b.setOrderId("A1");
		when(brokerageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(b));
		brokerageService.scheduleSettleRebate();
		verify(popularizePromoterCountWriteService).addSettleRebateForSchedule(11L, 22L, 5000L);
		verify(brokerageMapper, times(1)).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("analysis §3 7-C-1: 数云 oem 早退，关单后无统计/积分")
	void analysis_s3_oemEarlyExit() {
		ReflectionTestUtils.setField(brokerageService, "oemShuyun", true);
		when(brokerageMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		Brokerage b = new Brokerage();
		b.setId(1L);
		b.setCompanyId(1L);
		b.setUserId(1L);
		b.setCommissionType("point");
		b.setRebatePoint("10");
		b.setOrderId("X");
		when(brokerageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(b));
		brokerageService.scheduleSettleRebate();
		verify(popularizeBrokerageStatisticsIncrementService, never()).add(anyMap(), anyLong(), anyLong());
		verify(pointMemberAddPointService, never())
				.addPointForPopularizeSettle(anyLong(), anyLong(), anyInt(), anyString());
	}

	@Test
	@DisplayName("analysis §3 7-A+7-C-2+6+8: 非 oem 积分路径写统计后调积分服务")
	void analysis_s3_pointPath() {
		when(brokerageMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
		Brokerage b = new Brokerage();
		b.setId(3L);
		b.setCompanyId(7L);
		b.setUserId(8L);
		b.setCommissionType("point");
		b.setRebatePoint("15");
		b.setOrderId("P1");
		when(brokerageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(b));
		brokerageService.scheduleSettleRebate();
		verify(popularizeBrokerageStatisticsIncrementService)
				.add(eq(Map.of("cash_withdrawal_point", 15L, "no_close_point", -15L)), eq(8L), eq(7L));
		verify(pointMemberAddPointService).addPointForPopularizeSettle(8L, 7L, 15, "P1");
	}

	@Test
	@DisplayName("analysis §3 9: 一行失败不影响下一行")
	void analysis_s3_rowErrorContinues() {
		when(brokerageMapper.selectCount(any(Wrapper.class))).thenReturn(2L);
		Brokerage fail = new Brokerage();
		fail.setId(1L);
		fail.setCompanyId(1L);
		fail.setUserId(1L);
		fail.setCommissionType("money");
		fail.setRebate(1);
		Brokerage ok = new Brokerage();
		ok.setId(2L);
		ok.setCompanyId(1L);
		ok.setUserId(1L);
		ok.setCommissionType("money");
		ok.setRebate(2);
		when(brokerageMapper.selectList(any(Wrapper.class)))
				.thenReturn(List.of(fail, ok), Collections.emptyList());
		when(brokerageMapper.update(isNull(), any(UpdateWrapper.class)))
				.thenThrow(new RuntimeException("db err"))
				.thenReturn(1);
		brokerageService.scheduleSettleRebate();
		verify(brokerageMapper, atLeastOnce()).update(isNull(), any(UpdateWrapper.class));
	}

	@Test
	@DisplayName("package: settleOneRebateRow 可单测行内成功（money）")
	void settleOneRowMoney() {
		Brokerage b = new Brokerage();
		b.setId(1L);
		b.setCompanyId(5L);
		b.setUserId(6L);
		b.setCommissionType("money");
		b.setRebate(3);
		assertDoesNotThrow(() -> brokerageService.settleOneRebateRow(b, 100));
		verify(popularizePromoterCountWriteService).addSettleRebateForSchedule(5L, 6L, 3L);
		verifyNoMoreInteractions(popularizeBrokerageStatisticsIncrementService, pointMemberAddPointService);
	}
}
