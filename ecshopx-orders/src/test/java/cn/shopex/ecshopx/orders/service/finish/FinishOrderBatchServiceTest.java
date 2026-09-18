package cn.shopex.ecshopx.orders.service.finish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.common.cron.port.TurntablePayGetTimesOnOrderPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderBrokerageOnFinishService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderConfirmReceiptTransactionService;
import cn.shopex.ecshopx.orders.service.normal.OrderInvoiceEndTimeOnOrderFinishService;
import cn.shopex.ecshopx.orders.service.normal.OrderProfitPlanCloseTimeWriteService;
import cn.shopex.ecshopx.orders.service.normal.OrdersRelChinaumspayDivisionWriteService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class FinishOrderBatchServiceTest {

	@Mock
	NormalOrdersMapper normalOrdersMapper;
	@Mock
	NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock
	OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;
	@Mock
	NormalOrderConfirmReceiptTransactionService normalOrderConfirmReceiptTransactionService;
	@Mock
	OrderProcessLogPublishPort orderProcessLogPublishPort;
	@Mock
	NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService;
	@Mock
	OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService;
	@Mock
	TurntablePayGetTimesOnOrderPort turntablePayGetTimesOnOrderPort;
	@Mock
	PointMemberAddPointService pointMemberAddPointService;
	@Mock
	OrderProfitPlanCloseTimeWriteService orderProfitPlanCloseTimeWriteService;
	@Mock
	OrderInvoiceEndTimeOnOrderFinishService orderInvoiceEndTimeOnOrderFinishService;

	@InjectMocks
	FinishOrderBatchService service;

	@Captor
	ArgumentCaptor<Long> finishBeforeSecCaptor;

	private ListAppender<ILoggingEvent> logAppender;
	private Logger batchLogger;

	@BeforeEach
	void attachLogger() {
		logAppender = new ListAppender<>();
		logAppender.start();
		logAppender.list.clear();
		batchLogger = (Logger) LoggerFactory.getLogger(FinishOrderBatchService.class);
		batchLogger.setLevel(Level.DEBUG);
		batchLogger.addAppender(logAppender);
	}

	@AfterEach
	void detachLogger() {
		batchLogger.detachAppender(logAppender);
		logAppender.stop();
	}

	/** plan §5、analysis §3 **3**：auto_finish 上界与 time()+60 对齐 */
	@Test
	void runEquivalent_countAndPage_useFinishBeforeSecThreshold() {
		when(normalOrdersMapper.countAutoFinishCandidates(anyLong())).thenReturn(0L);
		long before = System.currentTimeMillis() / 1000L + 60L;
		service.runEquivalentToFinishOrderJobHandle();
		verify(normalOrdersMapper).countAutoFinishCandidates(finishBeforeSecCaptor.capture());
		assertThat(finishBeforeSecCaptor.getValue()).isBetween(before - 2L, before + 2L);
	}

	/** plan §5 **4-1**、**6** */
	@Test
	void runEquivalent_totalCountZero_noPageQuery_debugTail() {
		when(normalOrdersMapper.countAutoFinishCandidates(anyLong())).thenReturn(0L);
		int n = service.runEquivalentToFinishOrderJobHandle();
		assertThat(n).isEqualTo(0);
		verify(normalOrdersMapper, never()).selectAutoFinishCandidatePage(anyLong(), anyLong(), anyInt());
		assertThat(logAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("成功执行自动确认收货"));
		assertThat(logAppender.list).anyMatch(
				e -> e.getLevel() == Level.DEBUG
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("未执行自动确认收货"));
	}

	/** plan §5 **4-2**、**5**：翻页第二页 offset=20 */
	@Test
	void runEquivalent_twoPages_secondOffset20() {
		when(normalOrdersMapper.countAutoFinishCandidates(anyLong())).thenReturn(25L);
		List<NormalOrders> page1 = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			NormalOrders o = new NormalOrders();
			o.setOrderId(100L + i);
			o.setCompanyId(1L);
			o.setOrderStatus("DONE");
			page1.add(o);
		}
		NormalOrders oLast = new NormalOrders();
		oLast.setOrderId(999L);
		oLast.setCompanyId(1L);
		oLast.setOrderStatus("DONE");
		List<NormalOrders> page2 = List.of(oLast);
		when(normalOrdersMapper.selectAutoFinishCandidatePage(anyLong(), eq(0L), eq(20))).thenReturn(page1);
		when(normalOrdersMapper.selectAutoFinishCandidatePage(anyLong(), eq(20L), eq(20))).thenReturn(page2);
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		service.runEquivalentToFinishOrderJobHandle();
		verify(normalOrdersMapper, times(1)).selectAutoFinishCandidatePage(anyLong(), eq(0L), eq(20));
		verify(normalOrdersMapper, times(1)).selectAutoFinishCandidatePage(anyLong(), eq(20L), eq(20));
	}

	/** plan §5 R1、analysis §3 **2.1**：子单映射（只拉子单+依赖注入可观测） */
	@Test
	void runEquivalent_loadsItemsForOrderIds() {
		when(normalOrdersMapper.countAutoFinishCandidates(anyLong())).thenReturn(1L);
		NormalOrders o = skipOrder(501L);
		when(normalOrdersMapper.selectAutoFinishCandidatePage(anyLong(), eq(0L), eq(20))).thenReturn(List.of(o));
		NormalOrdersItems line = new NormalOrdersItems();
		line.setOrderId(501L);
		line.setAftersalesStatus("NO_APPLY");
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(line));
		service.runEquivalentToFinishOrderJobHandle();
		verify(normalOrdersItemsMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
	}

	/** plan §5 **5.x.1** */
	@Test
	void runEquivalent_skip_badOrderStatus() {
		when(normalOrdersMapper.countAutoFinishCandidates(anyLong())).thenReturn(1L);
		NormalOrders o = new NormalOrders();
		o.setOrderId(1L);
		o.setCompanyId(1L);
		o.setOrderStatus("DONE");
		o.setDeliveryStatus("DONE");
		o.setCancelStatus("NO_APPLY_CANCEL");
		when(normalOrdersMapper.selectAutoFinishCandidatePage(anyLong(), eq(0L), eq(20))).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		service.runEquivalentToFinishOrderJobHandle();
		verify(normalOrderConfirmReceiptTransactionService, never())
				.applyConfirmReceiptInTransaction(anyLong(), anyLong(), any(), any(), anyLong(), anyInt());
	}

	/** plan §5 **5.x.2** */
	@Test
	void runEquivalent_skip_badDelivery() {
		when(normalOrdersMapper.countAutoFinishCandidates(anyLong())).thenReturn(1L);
		NormalOrders o = new NormalOrders();
		o.setOrderId(1L);
		o.setCompanyId(1L);
		o.setOrderStatus("WAIT_BUYER_CONFIRM");
		o.setDeliveryStatus("PENDING");
		o.setCancelStatus("NO_APPLY_CANCEL");
		when(normalOrdersMapper.selectAutoFinishCandidatePage(anyLong(), eq(0L), eq(20))).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		service.runEquivalentToFinishOrderJobHandle();
		verify(normalOrderConfirmReceiptTransactionService, never())
				.applyConfirmReceiptInTransaction(anyLong(), anyLong(), any(), any(), anyLong(), anyInt());
	}

	/** plan §5 **5.x.3** */
	@Test
	void runEquivalent_skip_badCancel() {
		when(normalOrdersMapper.countAutoFinishCandidates(anyLong())).thenReturn(1L);
		NormalOrders o = new NormalOrders();
		o.setOrderId(1L);
		o.setCompanyId(1L);
		o.setOrderStatus("WAIT_BUYER_CONFIRM");
		o.setDeliveryStatus("DONE");
		o.setCancelStatus("WAIT_PROCESS");
		when(normalOrdersMapper.selectAutoFinishCandidatePage(anyLong(), eq(0L), eq(20))).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		service.runEquivalentToFinishOrderJobHandle();
		verify(normalOrderConfirmReceiptTransactionService, never())
				.applyConfirmReceiptInTransaction(anyLong(), anyLong(), any(), any(), anyLong(), anyInt());
	}

	/** plan §5 **5.x.4** */
	@Test
	void runEquivalent_skip_aftersalesBlocking() {
		when(normalOrdersMapper.countAutoFinishCandidates(anyLong())).thenReturn(1L);
		NormalOrders o = new NormalOrders();
		o.setOrderId(1L);
		o.setCompanyId(1L);
		o.setOrderStatus("WAIT_BUYER_CONFIRM");
		o.setDeliveryStatus("DONE");
		o.setCancelStatus("NO_APPLY_CANCEL");
		when(normalOrdersMapper.selectAutoFinishCandidatePage(anyLong(), eq(0L), eq(20))).thenReturn(List.of(o));
		NormalOrdersItems li = new NormalOrdersItems();
		li.setOrderId(1L);
		li.setAftersalesStatus("WAIT_SELLER_AGREE");
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(li));
		service.runEquivalentToFinishOrderJobHandle();
		verify(normalOrderConfirmReceiptTransactionService, never())
				.applyConfirmReceiptInTransaction(anyLong(), anyLong(), any(), any(), anyLong(), anyInt());
	}

	/** plan §5 **5.x.5** 主链路顺序 + 银商/积分/大转盘 */
	@Test
	void runEquivalent_successPath_sideEffectOrder() {
		when(normalOrdersMapper.countAutoFinishCandidates(anyLong())).thenReturn(1L);
		NormalOrders o = new NormalOrders();
		o.setOrderId(55L);
		o.setCompanyId(7L);
		o.setUserId(9L);
		o.setOrderStatus("WAIT_BUYER_CONFIRM");
		o.setDeliveryStatus("DONE");
		o.setCancelStatus("NO_APPLY_CANCEL");
		o.setPayType("chinaums");
		o.setDistributorId(3L);
		o.setTotalFee("5000");
		o.setBonusPoints(10);
		when(normalOrdersMapper.selectAutoFinishCandidatePage(anyLong(), eq(0L), eq(20))).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		Map<String, Object> setting = new HashMap<>();
		setting.put("latest_aftersale_time", 7);
		when(orderValiditySettingRedisReadService.readPlatformSetting(7L)).thenReturn(setting);

		NormalOrders fresh = new NormalOrders();
		fresh.setOrderId(55L);
		fresh.setCompanyId(7L);
		fresh.setUserId(9L);
		fresh.setPayType("chinaums");
		fresh.setDistributorId(3L);
		fresh.setTotalFee("5000");
		fresh.setBonusPoints(10);
		when(normalOrderConfirmReceiptTransactionService.applyConfirmReceiptInTransaction(
						eq(7L), eq(55L), eq("55"), any(NormalOrders.class), anyLong(), anyInt()))
				.thenReturn(fresh);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCap = ArgumentCaptor.forClass(Map.class);

		int n = service.runEquivalentToFinishOrderJobHandle();
		assertThat(n).isEqualTo(1);

		InOrder inOrder = inOrder(
				normalOrderConfirmReceiptTransactionService,
				orderProcessLogPublishPort,
				normalOrderBrokerageOnFinishService,
				orderInvoiceEndTimeOnOrderFinishService,
				ordersRelChinaumspayDivisionWriteService,
				turntablePayGetTimesOnOrderPort,
				pointMemberAddPointService,
				orderProfitPlanCloseTimeWriteService);
		inOrder.verify(normalOrderConfirmReceiptTransactionService, times(1))
				.applyConfirmReceiptInTransaction(anyLong(), anyLong(), any(), any(), anyLong(), anyInt());
		inOrder.verify(orderProcessLogPublishPort, times(1)).publish(payloadCap.capture());
		inOrder.verify(normalOrderBrokerageOnFinishService, times(1)).orderFinishBrokerage(eq(7L), eq(55L), eq(fresh));
		inOrder.verify(orderInvoiceEndTimeOnOrderFinishService, times(1))
				.updateInvoiceEndTime(eq(7L), eq(55L), anyInt(), anyInt());
		inOrder.verify(ordersRelChinaumspayDivisionWriteService, times(1)).addRelChinaumsPayDivision(eq(7L), eq(fresh));
		inOrder.verify(turntablePayGetTimesOnOrderPort, times(1)).payGetTimes(eq(9L), eq(7L), eq(5000));
		inOrder.verify(pointMemberAddPointService, times(1)).addPointForNormalOrderBonus(eq(9L), eq(7L), eq(10), eq(55L));
		inOrder.verify(orderProfitPlanCloseTimeWriteService, times(1)).orderProfitPlanCloseTime(eq(7L), eq(55L));

		Map<String, Object> ent = payloadCap.getValue();
		assertThat(ent).doesNotContainKey("params");
		assertThat(ent.get("order_id")).isEqualTo(55L);
		assertThat(ent.get("company_id")).isEqualTo(7L);
		assertThat(ent.get("operator_type")).isEqualTo("system");
		assertThat(ent.get("operator_id")).isEqualTo(0L);
		assertThat(ent.get("remarks")).isEqualTo("订单完成");
		assertThat(ent.get("detail").toString()).contains("订单单号：55").endsWith("，订单自动完成");
	}

	/** plan §5：bonus=0 不调积分；非 chinaums 不插分账 */
	@Test
	void runEquivalent_noBonus_noPoint_nonChinaums_noDivision() {
		when(normalOrdersMapper.countAutoFinishCandidates(anyLong())).thenReturn(1L);
		NormalOrders o = new NormalOrders();
		o.setOrderId(2L);
		o.setCompanyId(1L);
		o.setUserId(3L);
		o.setOrderStatus("WAIT_BUYER_CONFIRM");
		o.setDeliveryStatus("DONE");
		o.setCancelStatus("NO_APPLY_CANCEL");
		o.setPayType("wechat");
		o.setDistributorId(0L);
		o.setTotalFee("100");
		o.setBonusPoints(0);
		when(normalOrdersMapper.selectAutoFinishCandidatePage(anyLong(), eq(0L), eq(20))).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		when(orderValiditySettingRedisReadService.readPlatformSetting(1L)).thenReturn(Map.of("latest_aftersale_time", 0));
		NormalOrders fresh = o;
		when(normalOrderConfirmReceiptTransactionService.applyConfirmReceiptInTransaction(
						anyLong(), anyLong(), any(), any(), anyLong(), anyInt()))
				.thenReturn(fresh);
		service.runEquivalentToFinishOrderJobHandle();
		verify(pointMemberAddPointService, never()).addPointForNormalOrderBonus(anyLong(), anyLong(), anyInt(), anyLong());
		verify(ordersRelChinaumspayDivisionWriteService, never()).addRelChinaumsPayDivision(anyLong(), any());
		verify(turntablePayGetTimesOnOrderPort, times(1)).payGetTimes(3L, 1L, 100);
	}

	private static NormalOrders skipOrder(long orderId) {
		NormalOrders o = new NormalOrders();
		o.setOrderId(orderId);
		o.setCompanyId(1L);
		o.setOrderStatus("DONE");
		return o;
	}
}
