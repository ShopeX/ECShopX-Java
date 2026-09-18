package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesCancelDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesAutoRefuseWxaTemplatePort;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesCancelNoticeJobPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderAutoCloseAftersalesCronPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.payment.AdapayScheduleAutoPaymentConfirmationPort;
import cn.shopex.ecshopx.common.port.payment.BspayScheduleAutoPaymentConfirmationPort;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.orders.event.JushuitanTradeAftersalesSyncSpringEvent;
import cn.shopex.ecshopx.orders.event.SaasErpAftersalesSpringEvent;
import cn.shopex.ecshopx.orders.event.TradeAftersalesCancelSpringEvent;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Schedule auto-done aftersales: verifies {@code scheduleAutoDoneAftersales} and the schedule branch of
 * {@code closeAftersalesForSchedule} / {@code executeCloseAftersalesTransaction}.
 *
 * <p>The XXL-Job entry {@code done-aftersales} is handled by
 * {@link cn.shopex.ecshopx.aftersales.cron.ScheduleDoneAftersalesHandler}, which drives the same
 * {@link AftersalesService} close flow as other scheduled auto-close paths. After a successful close,
 * {@code afterCommit} runs callbacks that include {@link TradeAftersalesCancelDispatchPublisher#publish}, routing
 * {@code SystemLinkDispatchEventNames.EVENT_TRADE_AFTERSALES_CANCEL} through the shared {@code DispatchFacade}
 * implementation. {@code DispatchFanOutPlanner} expands that parent dispatch into registered child listeners,
 * including {@link cn.shopex.ecshopx.systemlink.dispatch.SystemLinkTradeAftersaleCancelSendOmeDispatchListener}
 * (Send OME cancel) alongside related neighbors. The job-driven schedule close reuses the same
 * {@code executeCloseAftersalesTransaction} / {@code afterCommit} hook as other close entry points, so this path
 * does not introduce a second {@code EVENT_TRADE_AFTERSALES_CANCEL} registration.
 *
 * <p>Full Bus and registry coverage for that event name lives in {@code ecshopx-bootstrap} tests such as
 * {@code TradeAftersalesCancelSyncEventDispatchFlowTest} and {@code TradeAftersalesCancelSendOmeEventDispatchFlowTest};
 * this module stays at publisher and ordering probes only.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AftersalesServiceScheduleDoneAftersalesTest {

	@Mock
	private AftersalesMapper aftersalesMapper;

	@Mock
	private AftersalesDetailMapper aftersalesDetailMapper;

	@Mock
	private AftersalesAdminDetailService aftersalesAdminDetailService;

	@Mock
	private AftersalesRefundService aftersalesRefundService;

	@Mock
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	@Mock
	private NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort;

	@Mock
	private AftersalesAutoRefuseWxaTemplatePort aftersalesAutoRefuseWxaTemplatePort;

	@Mock
	private NormalOrderAutoCloseAftersalesCronPort normalOrderAutoCloseAftersalesCronPort;

	@Mock
	private AdapayScheduleAutoPaymentConfirmationPort adapayScheduleAutoPaymentConfirmationPort;

	@Mock
	private BspayScheduleAutoPaymentConfirmationPort bspayScheduleAutoPaymentConfirmationPort;

	@Mock
	private CompanysMapper companysMapper;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@Mock
	private AftersalesCancelNoticeJobPort aftersalesCancelNoticeJobPort;

	@Mock
	private JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher;

	@Mock
	private TradeAftersalesCancelDispatchPublisher tradeAftersalesCancelDispatchPublisher;

	@Mock
	private ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;

	private final JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder =
			new JushuitanTradeAftersalesBusPayloadBuilder();

	private AftersalesService service;

	private ListAppender<ILoggingEvent> logAppender;
	private Logger serviceLogger;

	@BeforeEach
	void init() {
		TransactionOperations tx =
				new TransactionOperations() {
					@Override
					public <T> T execute(TransactionCallback<T> callback) {
						boolean started = !TransactionSynchronizationManager.isSynchronizationActive();
						if (started) {
							TransactionSynchronizationManager.initSynchronization();
						}
						try {
							T result = callback.doInTransaction(new SimpleTransactionStatus(true));
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization s :
										TransactionSynchronizationManager.getSynchronizations()) {
									s.afterCommit();
								}
							}
							return result;
						} finally {
							if (started) {
								TransactionSynchronizationManager.clear();
							}
						}
					}
				};
		service =
				new AftersalesService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesAdminDetailService,
						aftersalesRefundService,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						aftersalesAutoRefuseWxaTemplatePort,
						normalOrderAutoCloseAftersalesCronPort,
						adapayScheduleAutoPaymentConfirmationPort,
						bspayScheduleAutoPaymentConfirmationPort,
						tx,
						companysMapper,
						applicationEventPublisher,
						aftersalesCancelNoticeJobPort,
						jushuitanTradeAftersalesDispatchPublisher,
						jushuitanTradeAftersalesBusPayloadBuilder,
						tradeAftersalesCancelDispatchPublisher,
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher);
		logAppender = new ListAppender<>();
		logAppender.start();
		logAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(AftersalesService.class);
		serviceLogger.addAppender(logAppender);
	}

	@AfterEach
	void detachLog() {
		serviceLogger.detachAppender(logAppender);
		logAppender.stop();
	}

	@Test
	@DisplayName("§3-1：无公司早退，processed=0，不对售后 count")
	void noCompanies_returnsZero() {
		when(companysMapper.selectList(any())).thenReturn(Collections.emptyList());
		assertThat(service.scheduleAutoDoneAftersales()).isZero();
		verify(aftersalesMapper, never()).selectCount(any());
	}

	@Test
	@DisplayName("§3-1,2,3：有公司 count=0 跳过内层分页")
	void companyWithZeroCount_skipsPage() {
		Companys c = new Companys();
		c.setCompanyId(9L);
		when(companysMapper.selectList(any())).thenReturn(List.of(c));
		when(aftersalesMapper.selectCount(any())).thenReturn(0L);
		assertThat(service.scheduleAutoDoneAftersales()).isZero();
		verify(aftersalesMapper, never()).selectPage(any(), any());
	}

	@Test
	@DisplayName(
			"§3-1,2,3,4,5,6,7b：101 条伪分页 totalPage=2、两页 current=1（offset=0）；§3-5 逐行 params、§3-6 close、§3-7b 每行失败吞掉")
	void pseudoPagination_twoFirstPages() {
		Companys c = new Companys();
		c.setCompanyId(1L);
		when(companysMapper.selectList(any())).thenReturn(List.of(c));
		when(aftersalesMapper.selectCount(any())).thenReturn(101L);
		Aftersales row = new Aftersales();
		row.setCompanyId(1L);
		row.setAftersalesBn(100L);
		row.setAftersalesStatus(3);
		IPage<Aftersales> p1 = new Page<>(1, 100, 101);
		p1.setRecords(Collections.nCopies(100, row));
		IPage<Aftersales> p2 = new Page<>(1, 100, 101);
		p2.setRecords(List.of(row));
		when(aftersalesMapper.selectPage(any(), any())).thenReturn(p1, p2);
		assertThat(service.scheduleAutoDoneAftersales()).isZero();
		ArgumentCaptor<Page<Aftersales>> pageCap = ArgumentCaptor.forClass(Page.class);
		verify(aftersalesMapper, times(2)).selectPage(pageCap.capture(), any());
		assertThat(pageCap.getAllValues()).allMatch(pg -> pg.getCurrent() == 1L && pg.getSize() == 100L);
	}

	@Test
	@DisplayName("§3-6,7b：状态 3 校验失败吞掉，不进入关单事务，processed=0")
	void statusRejected_swallowedWithWarn() {
		Companys c = new Companys();
		c.setCompanyId(1L);
		when(companysMapper.selectList(any())).thenReturn(List.of(c));
		when(aftersalesMapper.selectCount(any())).thenReturn(1L);
		Aftersales row = new Aftersales();
		row.setCompanyId(1L);
		row.setAftersalesBn(55L);
		row.setAftersalesStatus(3);
		IPage<Aftersales> page = new Page<>(1, 100, 1);
		page.setRecords(List.of(row));
		when(aftersalesMapper.selectPage(any(), any())).thenReturn(page);
		when(aftersalesMapper.selectOne(any())).thenReturn(row);
		assertThat(service.scheduleAutoDoneAftersales()).isZero();
		verify(aftersalesRefundService, never()).findRefundByAftersalesBn(anyLong(), anyLong());
		assertThat(logAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.WARN
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("scheduleAutoDoneAftersales skip"));
	}

	@Test
	@DisplayName("§3-5′,6′：多公司第一司无数据第二司有驳回行，行为与返回一致")
	void twoCompanies_firstEmpty_secondProcesses() {
		Companys c1 = new Companys();
		c1.setCompanyId(1L);
		Companys c2 = new Companys();
		c2.setCompanyId(2L);
		when(companysMapper.selectList(any())).thenReturn(List.of(c1, c2));
		when(aftersalesMapper.selectCount(any())).thenReturn(0L, 1L);
		Aftersales row = new Aftersales();
		row.setCompanyId(2L);
		row.setAftersalesBn(77L);
		row.setAftersalesStatus(3);
		IPage<Aftersales> page = new Page<>(1, 100, 1);
		page.setRecords(List.of(row));
		when(aftersalesMapper.selectPage(any(), any())).thenReturn(page);
		when(aftersalesMapper.selectOne(any())).thenReturn(row);
		assertThat(service.scheduleAutoDoneAftersales()).isZero();
		verify(aftersalesMapper, times(2)).selectCount(any());
	}

	@Test
	@DisplayName(
			"§3-6,7a 与 §3-8 TradeAftersalesCancelSpringEvent、§3-9 ThirdParty SaaS ERP Bus publish、§3-10 JushuitanTradeAftersalesSyncSpringEvent + scheduleSendAftersaleCancelNotice：成功关单 afterCommit")
	void closeForSchedule_applicableStatus_updatesAndPublishesLog() {
		Aftersales main = new Aftersales();
		main.setCompanyId(1L);
		main.setAftersalesBn(10L);
		main.setAftersalesStatus(0);
		main.setOrderId(100L);
		when(aftersalesMapper.selectOne(any())).thenReturn(main);
		AftersalesRefund refund = new AftersalesRefund();
		when(aftersalesRefundService.findRefundByAftersalesBn(1L, 10L)).thenReturn(refund);
		when(aftersalesMapper.update(any(), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(any(), any())).thenReturn(1);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(eq(1L), eq(10L), any())).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(Collections.emptyList());
		Aftersales updated = new Aftersales();
		updated.setCompanyId(1L);
		updated.setAftersalesBn(10L);
		updated.setAftersalesStatus(4);
		updated.setProgress(7);
		updated.setOrderId(100L);
		when(aftersalesMapper.selectOne(any())).thenReturn(main, updated);
		service.closeAftersalesForSchedule(1L, 10L, this);
		verify(orderProcessLogPublishPort, atLeastOnce()).publish(any());
		verify(aftersalesMapper, atLeastOnce()).update(any(), any());
		InOrder inOrder =
				inOrder(
						applicationEventPublisher,
						tradeAftersalesCancelDispatchPublisher,
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher,
						aftersalesCancelNoticeJobPort);
		inOrder.verify(applicationEventPublisher).publishEvent(any(TradeAftersalesCancelSpringEvent.class));
		inOrder.verify(tradeAftersalesCancelDispatchPublisher).publish(any());
		inOrder.verify(thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher).publish(any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> busPayloadCaptor = ArgumentCaptor.forClass(Map.class);
		inOrder.verify(jushuitanTradeAftersalesDispatchPublisher, times(1)).publish(busPayloadCaptor.capture());
		inOrder.verify(applicationEventPublisher).publishEvent(any(JushuitanTradeAftersalesSyncSpringEvent.class));
		inOrder.verify(aftersalesCancelNoticeJobPort).scheduleSendAftersaleCancelNotice(eq(1L), eq(10L));
		Map<String, Object> busPayload = busPayloadCaptor.getValue();
		assertThat(((Number) busPayload.get("progress")).intValue()).isEqualTo(7);
		assertThat(((Number) busPayload.get("aftersales_status")).intValue()).isEqualTo(4);
		verify(applicationEventPublisher, never()).publishEvent(isA(SaasErpAftersalesSpringEvent.class));
	}
}
