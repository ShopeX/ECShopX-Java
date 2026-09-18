package cn.shopex.ecshopx.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.orders.domain.OrderItemsProfit;
import cn.shopex.ecshopx.orders.domain.OrderProfit;
import cn.shopex.ecshopx.orders.mapper.OrderItemsProfitMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class OrderProfitServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderProfit.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderItemsProfit.class);
	}

	@Mock
	private OrderProfitMapper orderProfitMapper;

	@Mock
	private OrderItemsProfitMapper orderItemsProfitMapper;

	@Mock
	private PlatformTransactionManager platformTransactionManager;

	private OrderProfitService service;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;

	@BeforeEach
	void setUp() throws Exception {
		lenient()
				.when(platformTransactionManager.getTransaction(any(TransactionDefinition.class)))
				.thenReturn(new SimpleTransactionStatus());
		service = new OrderProfitService(orderProfitMapper, orderItemsProfitMapper, platformTransactionManager);

		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(OrderProfitService.class);
		serviceLogger.setLevel(Level.DEBUG);
		serviceLogger.addAppender(listAppender);
	}

	@AfterEach
	void tearDown() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("无待结算早退；analysis §3 步骤 1+2+3（步骤 3 为 totalCount==0）")
	void scheduleSettleProfit_totalZero_earlyExit() {
		when(orderProfitMapper.countPendingFrozenProfitWithNormalOrderJoin(anyLong())).thenReturn(0L);
		assertThat(service.scheduleSettleProfit()).isZero();
		verify(orderProfitMapper, never()).selectPageForSettle(anyLong(), anyInt(), anyInt());
		verify(orderProfitMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
		verify(orderItemsProfitMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
	}

	@Test
	@DisplayName("单页单条双表更新成功；analysis §3 步骤 1+2+3+4+5+6+7+8+8.1+8.2+8.3+10")
	void scheduleSettleProfit_singleRow_updatesBothTables() {
		when(orderProfitMapper.countPendingFrozenProfitWithNormalOrderJoin(anyLong())).thenReturn(1L);
		OrderProfit row = new OrderProfit();
		row.setId(10L);
		row.setOrderId(20L);
		when(orderProfitMapper.selectPageForSettle(anyLong(), eq(0), eq(100))).thenReturn(List.of(row));
		when(orderProfitMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
		when(orderItemsProfitMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

		assertThat(service.scheduleSettleProfit()).isEqualTo(1);

		verify(orderProfitMapper, times(1)).selectPageForSettle(anyLong(), eq(0), eq(100));
		verify(orderProfitMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
		verify(orderItemsProfitMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
		verify(platformTransactionManager, times(1)).commit(any(TransactionStatus.class));
	}

	@Test
	@DisplayName("单行 UPDATE 失败吞异常继续；analysis §3 步骤 1+2+3+4+5+6+7+8+8.1+8.2+8.3+9+9.1+9.2+9.3+10")
	void scheduleSettleProfit_rowFails_logsDebugAndContinues() {
		when(orderProfitMapper.countPendingFrozenProfitWithNormalOrderJoin(anyLong())).thenReturn(2L);
		OrderProfit row1 = new OrderProfit();
		row1.setId(1L);
		row1.setOrderId(101L);
		OrderProfit row2 = new OrderProfit();
		row2.setId(2L);
		row2.setOrderId(102L);
		List<OrderProfit> page = new ArrayList<>();
		page.add(row1);
		page.add(row2);
		when(orderProfitMapper.selectPageForSettle(anyLong(), eq(0), eq(100))).thenReturn(page);

		AtomicInteger headCalls = new AtomicInteger();
		when(orderProfitMapper.update(isNull(), any(LambdaUpdateWrapper.class)))
				.thenAnswer(
						inv -> {
							int n = headCalls.incrementAndGet();
							if (n == 1) {
								throw new RuntimeException("boom");
							}
							return 1;
						});
		when(orderItemsProfitMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

		assertThat(service.scheduleSettleProfit()).isEqualTo(1);

		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("定时执行导购分销佣金结算失败"));
		verify(orderProfitMapper, times(2)).update(isNull(), any(LambdaUpdateWrapper.class));
		verify(orderItemsProfitMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
	}

	@Test
	@DisplayName("分页多页；analysis §3 步骤 1+2+3+4+5+4+5+6+7+8+8.1+8.2+8.3+10")
	void scheduleSettleProfit_multiPage_secondPageFetched() {
		when(orderProfitMapper.countPendingFrozenProfitWithNormalOrderJoin(anyLong())).thenReturn(150L);
		OrderProfit r1 = new OrderProfit();
		r1.setId(1L);
		r1.setOrderId(10L);
		List<OrderProfit> first = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			OrderProfit r = new OrderProfit();
			r.setId((long) i);
			r.setOrderId((long) (1000 + i));
			first.add(r);
		}
		when(orderProfitMapper.selectPageForSettle(anyLong(), eq(0), eq(100))).thenReturn(first);
		when(orderProfitMapper.selectPageForSettle(anyLong(), eq(100), eq(100))).thenReturn(List.of(r1));
		when(orderProfitMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
		when(orderItemsProfitMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

		assertThat(service.scheduleSettleProfit()).isEqualTo(101);

		ArgumentCaptor<Integer> offsetCap = ArgumentCaptor.forClass(Integer.class);
		verify(orderProfitMapper, times(2)).selectPageForSettle(anyLong(), offsetCap.capture(), eq(100));
		assertThat(offsetCap.getAllValues()).containsExactly(0, 100);
	}
}
