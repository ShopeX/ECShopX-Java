package cn.shopex.ecshopx.orders.service.espier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.service.AftersalesShopPartialCancelCreateService;
import cn.shopex.ecshopx.aftersales.support.AftersalesRefundEntityTradeRefundPayloadMapper;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderPartialCancelService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 管理端批量导入取消行驱动「部分发货仍可取消」链路：{@link NormalOrdersCancelUploadImportRowService#acceptRow} 经
 * {@link AdminNormalOrderPartialCancelService}、{@link AftersalesShopPartialCancelCreateService#createForShopPartialCancel}
 * 落仅退款售后；旺店通售后由创单末尾 {@code registerWdtErpTradeAfterSalePublishPostCommit} 在事务提交后发布。
 * 平台开启自动售后时，{@link AftersalesShopPartialCancelCreateService#autoApproveOnlyRefund} 另行注册聚水潭
 * {@code registerJushuitanTradeAftersalesPublishPostCommit}（<strong>不</strong>调用旺店通发布端）。
 * 本夹具以内存 Dispatch 观察聚水潭异步入队与 {@link DispatchConsumerRuntime#consume} 出队，并校验旺店通 mock 与创单侧契约。
 */
@DisplayName("部分发货取消导入行：创单路径旺店通发布与自动售后路径聚水潭异步入队")
class NormalOrdersCancelPartailImportRowJushuitanDispatchFlowTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesDetail.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
	}

	@Test
	@DisplayName("自动售后开启：聚水潭由 autoApproveOnlyRefund 入队；旺店通由创单 registerWdt… 在提交后发布一次")
	void acceptRow_partail_reachesAsyncBusPublish_andConsumerInvokesListener() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		AtomicInteger listenerInvocations = new AtomicInteger();
		registry.registerEventListener(
				SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
				"listener:systemlink.trade_aftersales_send_jushuitan",
				ListenerDispatchOptions.asyncDefaults(),
				payload -> listenerInvocations.incrementAndGet());

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		JushuitanTradeAftersalesDispatchPublisher asyncHarnessPublisher =
				payload ->
						facade.publishEvent(
								SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
								payload,
								new DispatchOptions(
										DispatchMode.ASYNC,
										DispatchDriverType.REDIS,
										null,
										null,
										RetryPolicy.platformDefault()));

		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);

		long companyId = 11L;
		long orderId = 9001L;
		long userId = 100L;

		AtomicLong insertedAftersalesBn = new AtomicLong();
		AtomicInteger aftersalesSelectSeq = new AtomicInteger();
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		when(aftersalesMapper.insert(any(Aftersales.class)))
				.thenAnswer(
						inv -> {
							Aftersales m = inv.getArgument(0);
							insertedAftersalesBn.set(m.getAftersalesBn());
							return 1;
						});
		when(aftersalesMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							long bn = insertedAftersalesBn.get();
							if (bn == 0L) {
								return null;
							}
							int n = aftersalesSelectSeq.incrementAndGet();
							Aftersales a = new Aftersales();
							a.setCompanyId(companyId);
							a.setAftersalesBn(bn);
							a.setOrderId(orderId);
							a.setUserId(userId);
							a.setAftersalesType("ONLY_REFUND");
							a.setShopId(1L);
							a.setDistributorId(0L);
							a.setSupplierId(0);
							a.setMerchantId(0L);
							a.setIsPartialCancel(true);
							a.setReason("buyer changed mind");
							a.setRefundFee(300);
							a.setRefundPoint(0);
							a.setFreight(0);
							if (n == 1) {
								a.setAftersalesStatus(0);
								a.setProgress(0);
							} else {
								a.setAftersalesStatus(1);
								a.setProgress(9);
								a.setRefundFee(300);
							}
							return a;
						});
		when(aftersalesMapper.update(any(), any())).thenReturn(1);

		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		when(aftersalesDetailMapper.insert(any(AftersalesDetail.class))).thenReturn(1);
		when(aftersalesDetailMapper.update(any(), any())).thenReturn(1);

		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		AtomicReference<AftersalesRefund> refundSnap = new AtomicReference<>();
		when(aftersalesRefundMapper.insert(any(AftersalesRefund.class)))
				.thenAnswer(
						inv -> {
							refundSnap.set(copyAftersalesRefund(inv.getArgument(0)));
							return 1;
						});
		when(aftersalesRefundMapper.selectOne(any())).thenAnswer(inv -> refundSnap.get());
		when(aftersalesRefundMapper.update(any(), any())).thenReturn(1);

		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapper =
				new AftersalesRefundEntityTradeRefundPayloadMapper();

		AftersalesShopPartialCancelCreateService aftersalesCreate =
				new AftersalesShopPartialCancelCreateService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundMapper,
						successTradePort(),
						orderLinesPort(),
						orderHeaderPort(),
						mock(OrderProcessLogPublishPort.class),
						mock(AftersalesRefundAsyncPort.class),
						mockRedisTemplate(),
						asyncHarnessPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						tradeRefundDispatchPublisher,
						tradeRefundPayloadMapper);

		OrderValiditySettingRedisReadService validity = mock(OrderValiditySettingRedisReadService.class);
		when(validity.readPlatformSetting(anyLong()))
				.thenReturn(
						new LinkedHashMap<>(Map.of("auto_aftersales", true, "order_finish_time", 7)));

		long lineId = 501L;

		NormalOrders order = partailPayedOrder(companyId, orderId, userId);
		NormalOrdersItems line = partialLine(companyId, orderId, userId, lineId);
		NormalOrdersItems lineAfterCancel = partialLine(companyId, orderId, userId, lineId);
		lineAfterCancel.setCancelItemNum(3);

		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		when(normalOrdersItemsMapper.selectList(any()))
				.thenReturn(List.of(line))
				.thenReturn(List.of(lineAfterCancel));
		when(normalOrdersItemsMapper.selectOne(any())).thenReturn(line);
		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		AdminNormalOrderPartialCancelService partialCancel =
				new AdminNormalOrderPartialCancelService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						mock(SupplierOrderMapper.class),
						aftersalesCreate,
						validity,
						mock(SendAfterSaleWaitDealNoticeJobDispatchPublisher.class));

		NormalOrdersCancelUploadImportRowService importRowService =
				new NormalOrdersCancelUploadImportRowService(
						normalOrdersMapper,
						mock(OrderAssociationsMapper.class),
						mock(CancelOrdersMapper.class),
						partialCancel);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("order_id", String.valueOf(orderId));
		row.put("cancel_reason", "buyer changed mind");

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
		transactionTemplate.executeWithoutResult(
				st ->
						importRowService.acceptRow(companyId, 2L, 0L, 0L, 0L, row, "admin"));

		assertEquals(
				1,
				captured.size(),
				"expected async enqueue after auto-approved partial cancel (trade-aftersales bus)");
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.EVENT, msg.messageType());
		assertEquals(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES, msg.messageName());
		assertEquals("listener:systemlink.trade_aftersales_send_jushuitan", msg.listenerName());
		assertEquals(companyId, ((Number) msg.payload().get("company_id")).longValue());
		assertEquals(orderId, ((Number) msg.payload().get("order_id")).longValue());
		assertTrue(((Number) msg.payload().get("aftersales_bn")).longValue() > 0L);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		assertEquals(1, listenerInvocations.get());
		verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(any());
		verify(tradeRefundDispatchPublisher, times(1)).publish(any());
	}

	@Test
	void acceptRow_whenAutoAftersalesDisabled_doesNotPublishJushuitanTradeAftersales() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		JushuitanTradeAftersalesDispatchPublisher asyncHarnessPublisher =
				payload ->
						facade.publishEvent(
								SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
								payload,
								new DispatchOptions(
										DispatchMode.ASYNC,
										DispatchDriverType.REDIS,
										null,
										null,
										RetryPolicy.platformDefault()));

		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);

		long companyId = 12L;
		long orderId = 9101L;
		long userId = 101L;

		AtomicLong insertedAftersalesBn = new AtomicLong();
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		when(aftersalesMapper.insert(any(Aftersales.class)))
				.thenAnswer(
						inv -> {
							Aftersales m = inv.getArgument(0);
							insertedAftersalesBn.set(m.getAftersalesBn());
							return 1;
						});
		when(aftersalesMapper.selectOne(any())).thenReturn(null);

		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		when(aftersalesDetailMapper.insert(any(AftersalesDetail.class))).thenReturn(1);

		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		AtomicReference<AftersalesRefund> refundSnapDisabled = new AtomicReference<>();
		when(aftersalesRefundMapper.insert(any(AftersalesRefund.class)))
				.thenAnswer(
						inv -> {
							refundSnapDisabled.set(copyAftersalesRefund(inv.getArgument(0)));
							return 1;
						});
		when(aftersalesRefundMapper.selectOne(any())).thenAnswer(inv -> refundSnapDisabled.get());

		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapper =
				new AftersalesRefundEntityTradeRefundPayloadMapper();

		AftersalesShopPartialCancelCreateService aftersalesCreate =
				new AftersalesShopPartialCancelCreateService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundMapper,
						successTradePort(),
						orderLinesPort(),
						orderHeaderPort(),
						mock(OrderProcessLogPublishPort.class),
						mock(AftersalesRefundAsyncPort.class),
						mockRedisTemplate(),
						asyncHarnessPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						tradeRefundDispatchPublisher,
						tradeRefundPayloadMapper);

		OrderValiditySettingRedisReadService validity = mock(OrderValiditySettingRedisReadService.class);
		when(validity.readPlatformSetting(anyLong()))
				.thenReturn(
						new LinkedHashMap<>(Map.of("auto_aftersales", false, "order_finish_time", 7)));

		long lineId = 501L;
		NormalOrders order = partailPayedOrder(companyId, orderId, userId);
		NormalOrdersItems line = partialLine(companyId, orderId, userId, lineId);
		NormalOrdersItems lineAfterCancel = partialLine(companyId, orderId, userId, lineId);
		lineAfterCancel.setCancelItemNum(3);

		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		when(normalOrdersItemsMapper.selectList(any()))
				.thenReturn(List.of(line))
				.thenReturn(List.of(lineAfterCancel));
		when(normalOrdersItemsMapper.selectOne(any())).thenReturn(line);
		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		AdminNormalOrderPartialCancelService partialCancel =
				new AdminNormalOrderPartialCancelService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						mock(SupplierOrderMapper.class),
						aftersalesCreate,
						validity,
						mock(SendAfterSaleWaitDealNoticeJobDispatchPublisher.class));

		NormalOrdersCancelUploadImportRowService importRowService =
				new NormalOrdersCancelUploadImportRowService(
						normalOrdersMapper,
						mock(OrderAssociationsMapper.class),
						mock(CancelOrdersMapper.class),
						partialCancel);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("order_id", String.valueOf(orderId));
		row.put("cancel_reason", "manual flow");

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
		transactionTemplate.executeWithoutResult(
				st -> importRowService.acceptRow(companyId, 2L, 0L, 0L, 0L, row, "admin"));

		assertTrue(captured.isEmpty(), "no bus publish when manual after-sales (approve later)");
		verify(wdtErpTradeAfterSaleDispatchPublisher, never()).publish(any());
		verify(tradeRefundDispatchPublisher, times(1)).publish(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void acceptRow_partialCancelAfterCommit_tradeRefundPublishPayload_containsCoreKeys() {
		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));
		JushuitanTradeAftersalesDispatchPublisher asyncHarnessPublisher =
				payload ->
						facade.publishEvent(
								SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES,
								payload,
								new DispatchOptions(
										DispatchMode.ASYNC,
										DispatchDriverType.REDIS,
										null,
										null,
										RetryPolicy.platformDefault()));

		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);

		long companyId = 13L;
		long orderId = 9201L;
		long userId = 102L;

		AtomicLong insertedAftersalesBn = new AtomicLong();
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		when(aftersalesMapper.insert(any(Aftersales.class)))
				.thenAnswer(
						inv -> {
							Aftersales m = inv.getArgument(0);
							insertedAftersalesBn.set(m.getAftersalesBn());
							return 1;
						});
		when(aftersalesMapper.selectOne(any())).thenReturn(null);

		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		when(aftersalesDetailMapper.insert(any(AftersalesDetail.class))).thenReturn(1);

		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		AtomicReference<AftersalesRefund> refundSnap = new AtomicReference<>();
		when(aftersalesRefundMapper.insert(any(AftersalesRefund.class)))
				.thenAnswer(
						inv -> {
							refundSnap.set(copyAftersalesRefund(inv.getArgument(0)));
							return 1;
						});
		when(aftersalesRefundMapper.selectOne(any())).thenAnswer(inv -> refundSnap.get());

		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapper =
				new AftersalesRefundEntityTradeRefundPayloadMapper();

		AftersalesShopPartialCancelCreateService aftersalesCreate =
				new AftersalesShopPartialCancelCreateService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundMapper,
						successTradePort(),
						orderLinesPort(),
						orderHeaderPort(),
						mock(OrderProcessLogPublishPort.class),
						mock(AftersalesRefundAsyncPort.class),
						mockRedisTemplate(),
						asyncHarnessPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						tradeRefundDispatchPublisher,
						tradeRefundPayloadMapper);

		OrderValiditySettingRedisReadService validity = mock(OrderValiditySettingRedisReadService.class);
		when(validity.readPlatformSetting(anyLong()))
				.thenReturn(
						new LinkedHashMap<>(Map.of("auto_aftersales", false, "order_finish_time", 7)));

		long lineId = 501L;
		NormalOrders order = partailPayedOrder(companyId, orderId, userId);
		NormalOrdersItems line = partialLine(companyId, orderId, userId, lineId);
		NormalOrdersItems lineAfterCancel = partialLine(companyId, orderId, userId, lineId);
		lineAfterCancel.setCancelItemNum(3);

		NormalOrdersMapper normalOrdersMapper = mock(NormalOrdersMapper.class);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		NormalOrdersItemsMapper normalOrdersItemsMapper = mock(NormalOrdersItemsMapper.class);
		when(normalOrdersItemsMapper.selectList(any()))
				.thenReturn(List.of(line))
				.thenReturn(List.of(lineAfterCancel));
		when(normalOrdersItemsMapper.selectOne(any())).thenReturn(line);
		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);

		AdminNormalOrderPartialCancelService partialCancel =
				new AdminNormalOrderPartialCancelService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						mock(SupplierOrderMapper.class),
						aftersalesCreate,
						validity,
						mock(SendAfterSaleWaitDealNoticeJobDispatchPublisher.class));

		NormalOrdersCancelUploadImportRowService importRowService =
				new NormalOrdersCancelUploadImportRowService(
						normalOrdersMapper,
						mock(OrderAssociationsMapper.class),
						mock(CancelOrdersMapper.class),
						partialCancel);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("order_id", String.valueOf(orderId));
		row.put("cancel_reason", "import row refund payload probe");

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
		transactionTemplate.executeWithoutResult(
				st ->
						importRowService.acceptRow(companyId, 2L, 0L, 0L, 0L, row, "admin"));

		assertTrue(captured.isEmpty(), "manual after-sales: no async jushuitan event");
		verify(wdtErpTradeAfterSaleDispatchPublisher, never()).publish(any());

		ArgumentCaptor<Map<String, Object>> tradeRefundPayloadCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeRefundDispatchPublisher, times(1)).publish(tradeRefundPayloadCaptor.capture());
		Map<String, Object> published = tradeRefundPayloadCaptor.getValue();
		AftersalesRefund persisted = refundSnap.get();
		assertThat(persisted).isNotNull();
		assertThat(published).isEqualTo(tradeRefundPayloadMapper.toDispatchPayload(persisted));
		assertThat(published.get("company_id")).isEqualTo(companyId);
		assertThat(published.get("order_id")).isEqualTo(orderId);
		assertThat(((Number) published.get("aftersales_bn")).longValue()).isEqualTo(insertedAftersalesBn.get());
		assertThat(published.get("refund_bn")).isNotNull();
	}

	private static AftersalesRefund copyAftersalesRefund(AftersalesRefund r) {
		AftersalesRefund c = new AftersalesRefund();
		c.setRefundBn(r.getRefundBn());
		c.setAftersalesBn(r.getAftersalesBn());
		c.setOrderId(r.getOrderId());
		c.setTradeId(r.getTradeId());
		c.setCompanyId(r.getCompanyId());
		c.setSupplierId(r.getSupplierId());
		c.setUserId(r.getUserId());
		c.setShopId(r.getShopId());
		c.setDistributorId(r.getDistributorId());
		c.setRefundType(r.getRefundType());
		c.setRefundChannel(r.getRefundChannel());
		c.setRefundStatus(r.getRefundStatus());
		c.setRefundFee(r.getRefundFee());
		c.setRefundPoint(r.getRefundPoint());
		c.setReturnFreight(r.getReturnFreight());
		c.setFreight(r.getFreight());
		c.setFreightType(r.getFreightType());
		c.setPayType(r.getPayType());
		c.setCurrency(r.getCurrency());
		c.setCurFeeType(r.getCurFeeType());
		c.setCurFeeRate(r.getCurFeeRate());
		c.setCurFeeSymbol(r.getCurFeeSymbol());
		c.setCurPayFee(r.getCurPayFee());
		c.setMerchantId(r.getMerchantId());
		c.setReturnPoint(r.getReturnPoint());
		c.setCreateTime(r.getCreateTime());
		c.setUpdateTime(r.getUpdateTime());
		return c;
	}

	private static NormalOrders partailPayedOrder(long companyId, long orderId, long userId) {
		NormalOrders o = new NormalOrders();
		o.setCompanyId(companyId);
		o.setOrderId(orderId);
		o.setUserId(userId);
		o.setOrderType("normal");
		o.setDeliveryStatus("PARTAIL");
		o.setOrderStatus("PAYED");
		return o;
	}

	private static NormalOrdersItems partialLine(long companyId, long orderId, long userId, long lineId) {
		NormalOrdersItems it = new NormalOrdersItems();
		it.setId(lineId);
		it.setCompanyId(companyId);
		it.setOrderId(orderId);
		it.setUserId(userId);
		it.setNum(5);
		it.setDeliveryItemNum(2);
		it.setCancelItemNum(0);
		it.setSupplierId(0);
		return it;
	}

	private static OrderSuccessTradeReadPort successTradePort() {
		OrderSuccessTradeReadPort port = mock(OrderSuccessTradeReadPort.class);
		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "T9001");
		trade.put("pay_type", "wxpay");
		trade.put("cur_fee_rate", 1.0);
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_symbol", "¥");
		trade.put("merchant_id", 0L);
		when(port.primarySuccessTrade(anyLong(), anyLong())).thenReturn(Optional.of(trade));
		return port;
	}

	private static OrderNormalOrderHeaderReadPort orderHeaderPort() {
		OrderNormalOrderHeaderReadPort port = mock(OrderNormalOrderHeaderReadPort.class);
		Map<String, Object> head = new LinkedHashMap<>();
		head.put("shop_id", 1L);
		head.put("distributor_id", 0L);
		head.put("merchant_id", 0L);
		head.put("pay_type", "wxpay");
		head.put("freight_type", "cash");
		when(port.getHeader(anyLong(), anyLong())).thenReturn(Optional.of(head));
		return port;
	}

	private static OrderNormalOrderItemsReadPort orderLinesPort() {
		OrderNormalOrderItemsReadPort port = mock(OrderNormalOrderItemsReadPort.class);
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("id", 501L);
		line.put("num", 5);
		line.put("total_fee", 500);
		line.put("point", 0);
		line.put("supplier_id", 0L);
		line.put("distributor_id", 0L);
		line.put("goods_id", 1L);
		line.put("item_id", 10L);
		line.put("item_bn", "SKU1");
		line.put("item_name", "item");
		line.put("order_item_type", "normal");
		line.put("pic", "");
		when(port.listItems(anyLong(), anyLong())).thenReturn(List.of(line));
		return port;
	}

	@SuppressWarnings("unchecked")
	private static StringRedisTemplate mockRedisTemplate() {
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		HashOperations<String, Object, Object> hash = mock(HashOperations.class);
		when(redis.opsForHash()).thenReturn(hash);
		when(hash.increment(anyString(), anyString(), anyLong())).thenReturn(1L);
		return redis;
	}
}
