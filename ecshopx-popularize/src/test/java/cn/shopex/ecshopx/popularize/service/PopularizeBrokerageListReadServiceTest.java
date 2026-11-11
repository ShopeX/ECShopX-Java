package cn.shopex.ecshopx.popularize.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PopularizeBrokerageListReadServiceTest {

	@Mock
	private BrokerageMapper brokerageMapper;

	@Mock
	private SalesmanBrokerageLogsQueryService salesmanBrokerageLogsQueryService;

	private PopularizeBrokerageListReadService service;

	@BeforeEach
	void setUp() {
		service =
				new PopularizeBrokerageListReadService(
						brokerageMapper, salesmanBrokerageLogsQueryService, new ObjectMapper());
	}

	@Test
	@DisplayName("distributor_id=0 对齐 PHP：走默认佣金列表，不进入导购 SQL")
	void distributorIdZeroUsesDefaultList() {
		Page<Brokerage> page = new Page<>(1, 20);
		page.setRecords(Collections.emptyList());
		page.setTotal(0L);
		when(brokerageMapper.selectPage(any(Page.class), any())).thenReturn(page);

		Map<String, Object> data =
				service.getBrokerageList(1L, "1", "20", "3890", null, "0");

		assertEquals(0L, data.get("total_count"));
		verify(brokerageMapper).selectPage(any(Page.class), any());
		verify(salesmanBrokerageLogsQueryService, never()).countByFilter(anyMap());
	}

	@Test
	@DisplayName("distributor_id>0 走导购佣金列表")
	void distributorIdPositiveUsesSalesmanList() {
		when(salesmanBrokerageLogsQueryService.countByFilter(anyMap())).thenReturn(0L);
		when(salesmanBrokerageLogsQueryService.selectPageByFilter(anyMap(), anyInt(), anyInt()))
				.thenReturn(Collections.emptyList());
		when(salesmanBrokerageLogsQueryService.enrichListWithSalesperson(eq(1L), anyList()))
				.thenReturn(Collections.emptyList());

		service.getBrokerageList(1L, "1", "20", "3890", null, "104");

		verify(salesmanBrokerageLogsQueryService).countByFilter(anyMap());
		verify(brokerageMapper, never()).selectPage(any(Page.class), any());
	}

	@Test
	@DisplayName("非法 distributor_id 仍返回 422 语义异常")
	void invalidDistributorIdThrows() {
		assertThrows(
				BadRequestException.class,
				() -> service.getBrokerageList(1L, "1", "20", "3890", null, "abc"));
	}
}
