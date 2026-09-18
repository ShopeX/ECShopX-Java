package cn.shopex.ecshopx.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import cn.shopex.ecshopx.common.dispatch.InvoiceCreateJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.InvoiceQueryJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedQueryJobDispatchPublisher;
import cn.shopex.ecshopx.common.port.orders.OrderInvoiceRedFromAftersalesPort;
import cn.shopex.ecshopx.orders.domain.OrderInvoice;
import cn.shopex.ecshopx.orders.mapper.OrderInvoiceMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class OrderInvoiceServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderInvoice.class);
	}

	@Mock
	private OrderInvoiceMapper orderInvoiceMapper;

	@Mock
	private InvoiceSettingRedisService invoiceSettingRedisService;

	@Mock
	private InvoiceCreateJobDispatchPublisher invoiceCreateJobDispatchPublisher;

	@Mock
	private InvoiceQueryJobDispatchPublisher invoiceQueryJobDispatchPublisher;

	@Mock
	private InvoiceRedQueryJobDispatchPublisher invoiceRedQueryJobDispatchPublisher;

	@Mock
	private OrderInvoiceRedFromAftersalesPort orderInvoiceRedFromAftersalesPort;

	@InjectMocks
	private OrderInvoiceService orderInvoiceService;

	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(OrderInvoiceService.class);
		serviceLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	/** analysis §3: 1,3,7 首查空 */
	@Test
	void scheduleCreateInvoice_noPending_startEndAndZeroDispatch() {
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		int n = orderInvoiceService.scheduleCreateInvoice();
		assertThat(n).isEqualTo(0);
		verify(invoiceCreateJobDispatchPublisher, never()).publish(any());
		verify(invoiceSettingRedisService, never()).getInvoiceSetting(any(Long.class));
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.INFO
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("开始执行定时开票任务"));
		assertThat(listAppender.list).anyMatch(
				e -> e.getLevel() == Level.INFO
						&& e.getFormattedMessage() != null
						&& e.getFormattedMessage().contains("执行完成"));
	}

	/** analysis §3: 2,3 首查仅 pending+online */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleCreateInvoice_firstSelect_onlyBaseStatusAndMethod() {
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		orderInvoiceService.scheduleCreateInvoice();
		ArgumentCaptor<LambdaQueryWrapper<OrderInvoice>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(orderInvoiceMapper).selectList(cap.capture());
		String seg = cap.getValue().getSqlSegment();
		assertThat(seg).contains("invoice_status");
		assertThat(seg).contains("invoice_method");
		assertThat(seg).doesNotContain("end_time");
		assertThat(seg).doesNotContain("close_aftersales_time");
	}

	/** analysis §3: 4,5,5-1 同公司去重、每公司一次 Redis */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleCreateInvoice_distinctCompanyIds_oneSettingReadPerCompany() {
		OrderInvoice a = baseInvoice(1L, 10L);
		OrderInvoice b = baseInvoice(2L, 10L);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(a, b))
				.thenReturn(List.of());
		when(invoiceSettingRedisService.getInvoiceSetting(10L)).thenReturn(Map.of("apply_node", 2));
		orderInvoiceService.scheduleCreateInvoice();
		verify(invoiceSettingRedisService, times(1)).getInvoiceSetting(10L);
	}

	/** analysis §3: 5-2 无 apply_node 则不入内层 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleCreateInvoice_noApplyNode_innerNotRun() {
		OrderInvoice a = baseInvoice(1L, 10L);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
		when(invoiceSettingRedisService.getInvoiceSetting(10L)).thenReturn(Map.of("other", 1));
		int n = orderInvoiceService.scheduleCreateInvoice();
		assertThat(n).isEqualTo(0);
		verify(orderInvoiceMapper, times(1)).selectList(any());
		verify(invoiceCreateJobDispatchPublisher, never()).publish(any());
	}

	/** analysis §3: 6,6-1,6-3,6-4,6-5,6-5-a */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleCreateInvoice_applyNodeOne_enqueues() {
		OrderInvoice a = baseInvoice(7L, 20L);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(a))
				.thenReturn(List.of(a));
		when(invoiceSettingRedisService.getInvoiceSetting(20L)).thenReturn(Map.of("apply_node", 1));
		int n = orderInvoiceService.scheduleCreateInvoice();
		assertThat(n).isEqualTo(1);
		verify(invoiceCreateJobDispatchPublisher, times(1)).publish(any());
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("找到")
								&& l.getFormattedMessage().contains("1")
								&& l.getFormattedMessage().contains("待处理"));
	}

	/** analysis §3: 6-2 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleCreateInvoice_applyNodeNotOne_usesCloseAftersales() {
		OrderInvoice a = baseInvoice(8L, 30L);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(a))
				.thenReturn(List.of());
		when(invoiceSettingRedisService.getInvoiceSetting(30L)).thenReturn(Map.of("apply_node", 2));
		ArgumentCaptor<LambdaQueryWrapper<OrderInvoice>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		orderInvoiceService.scheduleCreateInvoice();
		verify(orderInvoiceMapper, times(2)).selectList(cap.capture());
		assertThat(cap.getAllValues().get(1).getSqlSegment()).contains("close_aftersales_time");
	}

	/** analysis §3: 6-5,6-5-b */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleCreateInvoice_enqueueFailure_stillFinishes() {
		OrderInvoice a = baseInvoice(9L, 40L);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(a))
				.thenReturn(List.of(a));
		when(invoiceSettingRedisService.getInvoiceSetting(40L)).thenReturn(Map.of("apply_node", 1));
		doThrow(new RuntimeException("queue down")).when(invoiceCreateJobDispatchPublisher).publish(any());
		int n = orderInvoiceService.scheduleCreateInvoice();
		assertThat(n).isEqualTo(0);
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getLevel() == Level.ERROR
								&& l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("处理发票申请失败 ID: 9"));
	}

	/** analysis §3 §8: 多轮累加时间条件 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleCreateInvoice_accumulatesEndAndCloseKeys() {
		OrderInvoice c10 = baseInvoice(11L, 10L);
		OrderInvoice c20 = baseInvoice(12L, 20L);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(c10, c20))
				.thenReturn(List.of(c10))
				.thenReturn(List.of());
		when(invoiceSettingRedisService.getInvoiceSetting(10L)).thenReturn(Map.of("apply_node", 1));
		when(invoiceSettingRedisService.getInvoiceSetting(20L)).thenReturn(Map.of("apply_node", 2));
		ArgumentCaptor<LambdaQueryWrapper<OrderInvoice>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		orderInvoiceService.scheduleCreateInvoice();
		verify(orderInvoiceMapper, times(3)).selectList(cap.capture());
		String firstInner = cap.getAllValues().get(1).getSqlSegment();
		assertThat(firstInner).contains("end_time");
		assertThat(firstInner).doesNotContain("close_aftersales_time");
		String secondInner = cap.getAllValues().get(2).getSqlSegment();
		assertThat(secondInner).contains("end_time");
		assertThat(secondInner).contains("close_aftersales_time");
	}

	/** analysis §3: 6-3 无命中行、6-4 仍打「找到 0 个」 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleCreateInvoice_innerEmpty_logsZeroFound() {
		OrderInvoice a = baseInvoice(13L, 50L);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(a))
				.thenReturn(List.of());
		when(invoiceSettingRedisService.getInvoiceSetting(50L)).thenReturn(Map.of("apply_node", 1));
		orderInvoiceService.scheduleCreateInvoice();
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getLevel() == Level.INFO
								&& l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("找到 0 个待处理发票申请"));
	}

	// --- ScheduleQueryInvoice (analysis §3: 1,2,3,4,5,6,7,7-1,7-2,8) ---

	/** §3-1, §3-4, §3-5, §3-8 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoice_emptyList_returnsZero_noPublish() {
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		int n = orderInvoiceService.scheduleQueryInvoice();
		assertThat(n).isEqualTo(0);
		verify(invoiceQueryJobDispatchPublisher, never()).publish(any());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("开始执行定时查询开票结果任务"));
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("没有需要查询的发票申请"));
	}

	/** §3-2, §3-3, §3-4: staleBefore = nowSec - 180, inProgress + update_time */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoice_wrapper_usesInProgressAndStale180() {
		int expectedStale = (int) (System.currentTimeMillis() / 1000L) - 180;
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		orderInvoiceService.scheduleQueryInvoice();
		ArgumentCaptor<LambdaQueryWrapper<OrderInvoice>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(orderInvoiceMapper).selectList(cap.capture());
		assertThat(cap.getValue().getSqlSegment()).contains("update_time");
		assertThat(cap.getValue().getSqlSegment()).contains("invoice_status");
		assertThat(cap.getValue().getParamNameValuePairs().values()).contains(expectedStale);
	}

	/** §3-3 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoice_filter_inProgress() {
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		orderInvoiceService.scheduleQueryInvoice();
		ArgumentCaptor<LambdaQueryWrapper<OrderInvoice>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(orderInvoiceMapper).selectList(cap.capture());
		assertThat(cap.getValue().getSqlSegment()).contains("invoice_status");
	}

	/** §3-4 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoice_selectListAtLeastOnce() {
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		orderInvoiceService.scheduleQueryInvoice();
		verify(orderInvoiceMapper, times(1)).selectList(any());
	}

	/** §3-5 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoice_empty_doesNotLogFoundCount() {
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		orderInvoiceService.scheduleQueryInvoice();
		assertThat(listAppender.list)
				.noneMatch(
						l -> l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("找到")
								&& l.getFormattedMessage().contains("需要查询"));
	}

	/** §3-6, §3-7 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoice_twoRows_publishesTwice() {
		OrderInvoice a = queryInvoice(101L, 1L, "BA", "O1", 1);
		OrderInvoice b = queryInvoice(102L, 2L, "BB", "O2", 2);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a, b));
		int n = orderInvoiceService.scheduleQueryInvoice();
		assertThat(n).isEqualTo(2);
		verify(invoiceQueryJobDispatchPublisher, times(2)).publish(any());
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getLevel() == Level.INFO
								&& l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("找到 2 个需要查询的发票申请"));
	}

	/** §3-7-1 载荷字段 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoice_publishPayload_matchesFixture() {
		OrderInvoice a = queryInvoice(201L, 9L, "BN9", "ORD9", 100);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
		orderInvoiceService.scheduleQueryInvoice();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(invoiceQueryJobDispatchPublisher).publish(cap.capture());
		assertThat(cap.getValue().get("invoice_id")).isEqualTo(201L);
		assertThat(cap.getValue().get("company_id")).isEqualTo(9L);
		assertThat(cap.getValue().get("invoice_apply_bn")).isEqualTo("BN9");
		assertThat(cap.getValue().get("order_id")).isEqualTo("ORD9");
	}

	/** §3-7-2: 首条入队失败不阻断、成功次数计 1 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoice_perRowCatchContinues_andCountsSuccessOnly() {
		OrderInvoice a = queryInvoice(301L, 1L, "x", "o1", 1);
		OrderInvoice b = queryInvoice(302L, 1L, "y", "o2", 2);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a, b));
		org.mockito.Mockito.doThrow(new RuntimeException("e1"))
				.doNothing()
				.when(invoiceQueryJobDispatchPublisher)
				.publish(any());
		int n = orderInvoiceService.scheduleQueryInvoice();
		assertThat(n).isEqualTo(1);
		verify(invoiceQueryJobDispatchPublisher, times(2)).publish(any());
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getLevel() == Level.ERROR
								&& l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("处理发票查询入队失败 ID: 301"));
	}

	/** §3-8（非空列表走完全程后） */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoice_completesWithFinishLog() {
		OrderInvoice a = queryInvoice(401L, 1L, "b", "o", 1);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
		orderInvoiceService.scheduleQueryInvoice();
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getLevel() == Level.INFO
								&& l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("定时查询开票结果任务执行完成"));
	}

	// --- ScheduleQueryInvoiceRed (analysis §3: 1,2,3,4,5,6,7,8-1,8-2,9) ---

	/** §3-1 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoiceRed_startLog() {
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		orderInvoiceService.scheduleQueryInvoiceRed();
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("scheduleQueryInvoiceRed")
								&& e.getFormattedMessage().contains("开始执行红冲定时查询任务"));
	}

	/** §3-2 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoiceRed_wrapper_wasteAndRedUrlNull() {
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		orderInvoiceService.scheduleQueryInvoiceRed();
		ArgumentCaptor<LambdaQueryWrapper<OrderInvoice>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(orderInvoiceMapper).selectList(cap.capture());
		assertThat(cap.getValue().getSqlSegment()).contains("invoice_status");
		assertThat(cap.getValue().getSqlSegment()).contains("invoice_file_url_red");
	}

	/** §3-3, §3-4 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoiceRed_selectList_logsCondition() {
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		orderInvoiceService.scheduleQueryInvoiceRed();
		verify(orderInvoiceMapper, times(1)).selectList(any());
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("查询条件")
								&& l.getFormattedMessage().contains("waste"));
	}

	/** §3-5 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoiceRed_nonempty_logsResultCount() {
		OrderInvoice a = redInvoice(1L, 10L, "O1", "RS1");
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
		orderInvoiceService.scheduleQueryInvoiceRed();
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("查询结果")
								&& l.getFormattedMessage().contains("1"));
	}

	/** §3-6 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoiceRed_empty_noPublish_returnsZero() {
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
		int n = orderInvoiceService.scheduleQueryInvoiceRed();
		assertThat(n).isEqualTo(0);
		verify(invoiceRedQueryJobDispatchPublisher, never()).publish(any());
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("没有待处理的红冲发票"));
		assertThat(listAppender.list)
				.noneMatch(
						l -> l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("找到")
								&& l.getFormattedMessage().contains("待处理的红冲"));
	}

	/** §3-7 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoiceRed_twoRows_logsTwo() {
		OrderInvoice a = redInvoice(1L, 10L, "O1", "R1");
		OrderInvoice b = redInvoice(2L, 10L, "O2", "R2");
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a, b));
		int n = orderInvoiceService.scheduleQueryInvoiceRed();
		assertThat(n).isEqualTo(2);
		verify(invoiceRedQueryJobDispatchPublisher, times(2)).publish(any());
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getLevel() == Level.INFO
								&& l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("找到 2 个待处理的红冲发票"));
	}

	/** §3-8-1 缺流水号 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoiceRed_missingRedSerial_skipsNoPublish() {
		OrderInvoice a = redInvoice(9L, 1L, "O", null);
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
		int n = orderInvoiceService.scheduleQueryInvoiceRed();
		assertThat(n).isEqualTo(0);
		verify(invoiceRedQueryJobDispatchPublisher, never()).publish(any());
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getLevel() == Level.WARN
								&& l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("红冲流水号"));
	}

	/** §3-8-1 入队成功 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoiceRed_publishPayload_matchesFixture() {
		OrderInvoice a = redInvoice(201L, 9L, "ORD9", "RED-SN-9");
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
		orderInvoiceService.scheduleQueryInvoiceRed();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(invoiceRedQueryJobDispatchPublisher).publish(cap.capture());
		assertThat(cap.getValue().get("id")).isEqualTo(201L);
		assertThat(cap.getValue().get("company_id")).isEqualTo(9L);
		assertThat(cap.getValue().get("order_id")).isEqualTo("ORD9");
		assertThat(cap.getValue().get("red_confirm_serial_no")).isEqualTo("RED-SN-9");
		assertThat(cap.getValue().get("entry_identity")).isEqualTo("0");
		assertThat(cap.getValue().get("type")).isEqualTo("red");
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("已推送到队列"));
	}

	/** §3-8-2 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoiceRed_perRowCatchContinues_countsSuccessOnly() {
		OrderInvoice a = redInvoice(301L, 1L, "o1", "r1");
		OrderInvoice b = redInvoice(302L, 1L, "o2", "r2");
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a, b));
		org.mockito.Mockito.doThrow(new RuntimeException("e1"))
				.doNothing()
				.when(invoiceRedQueryJobDispatchPublisher)
				.publish(any());
		int n = orderInvoiceService.scheduleQueryInvoiceRed();
		assertThat(n).isEqualTo(1);
		verify(invoiceRedQueryJobDispatchPublisher, times(2)).publish(any());
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getLevel() == Level.ERROR
								&& l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("处理红冲发票失败 ID: 301"));
	}

	/** §3-9 */
	@Test
	@SuppressWarnings("unchecked")
	void scheduleQueryInvoiceRed_completesWithFinishLog() {
		OrderInvoice a = redInvoice(401L, 1L, "o", "r");
		when(orderInvoiceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(a));
		orderInvoiceService.scheduleQueryInvoiceRed();
		assertThat(listAppender.list)
				.anyMatch(
						l -> l.getLevel() == Level.INFO
								&& l.getFormattedMessage() != null
								&& l.getFormattedMessage().contains("红冲定时查询任务执行完成"));
	}

	// --- executeInvoiceRedJob ---

	@Test
	void executeInvoiceRedJob_empty_noSelect() {
		orderInvoiceService.executeInvoiceRedJob(Map.of());
		verify(orderInvoiceMapper, never()).selectOne(any());
		verify(orderInvoiceRedFromAftersalesPort, never()).redInvoice(any());
	}

	@Test
	void executeInvoiceRedJob_missingCompany_noSelect() {
		Map<String, Object> job = new LinkedHashMap<>();
		job.put("order_id", "O1");
		orderInvoiceService.executeInvoiceRedJob(job);
		verify(orderInvoiceMapper, never()).selectOne(any());
		verify(orderInvoiceRedFromAftersalesPort, never()).redInvoice(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void executeInvoiceRedJob_invoiceNotFound() {
		when(orderInvoiceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
		orderInvoiceService.executeInvoiceRedJob(Map.of("company_id", 1L, "order_id", "MISS"));
		verify(orderInvoiceRedFromAftersalesPort, never()).redInvoice(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void executeInvoiceRedJob_cancel_skipsPortAndUpdate() {
		OrderInvoice inv = new OrderInvoice();
		inv.setId(61L);
		inv.setInvoiceStatus("cancel");
		when(orderInvoiceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inv);
		orderInvoiceService.executeInvoiceRedJob(Map.of("company_id", 1L, "order_id", "O1"));
		verify(orderInvoiceRedFromAftersalesPort, never()).redInvoice(any());
		verify(orderInvoiceMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void executeInvoiceRedJob_pending_setsCancel() {
		OrderInvoice inv = new OrderInvoice();
		inv.setId(62L);
		inv.setInvoiceStatus("pending");
		when(orderInvoiceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inv);
		when(orderInvoiceMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
		orderInvoiceService.executeInvoiceRedJob(Map.of("company_id", 1L, "order_id", "O1"));
		verify(orderInvoiceRedFromAftersalesPort, never()).redInvoice(any());
		@SuppressWarnings("rawtypes")
		ArgumentCaptor<LambdaUpdateWrapper> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(orderInvoiceMapper).update(isNull(), cap.capture());
		assertThat(cap.getValue().getSqlSet()).contains("invoice_status").contains("update_time");
	}

	@Test
	@SuppressWarnings("unchecked")
	void executeInvoiceRedJob_success_delegatesToPort() {
		OrderInvoice inv = new OrderInvoice();
		inv.setId(63L);
		inv.setInvoiceStatus("success");
		when(orderInvoiceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inv);
		Map<String, Object> job = Map.of("company_id", 2L, "order_id", "O9");
		orderInvoiceService.executeInvoiceRedJob(job);
		verify(orderInvoiceRedFromAftersalesPort).redInvoice(eq(job));
		verify(orderInvoiceMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void executeInvoiceRedJob_inProgress_noPort() {
		OrderInvoice inv = new OrderInvoice();
		inv.setId(64L);
		inv.setInvoiceStatus("inProgress");
		when(orderInvoiceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inv);
		orderInvoiceService.executeInvoiceRedJob(Map.of("company_id", 1L, "order_id", "O1"));
		verify(orderInvoiceRedFromAftersalesPort, never()).redInvoice(any());
		verify(orderInvoiceMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
	}

	private static OrderInvoice redInvoice(long id, long companyId, String orderId, String redSerialNo) {
		OrderInvoice o = new OrderInvoice();
		o.setId(id);
		o.setCompanyId(companyId);
		o.setOrderId(orderId);
		o.setInvoiceStatus("waste");
		o.setInvoiceFileUrlRed(null);
		o.setRedSerialNo(redSerialNo);
		return o;
	}

	private static OrderInvoice queryInvoice(
			long id, long companyId, String applyBn, String orderId, int oldUpdateTimeSec) {
		OrderInvoice o = new OrderInvoice();
		o.setId(id);
		o.setCompanyId(companyId);
		o.setInvoiceApplyBn(applyBn);
		o.setOrderId(orderId);
		o.setInvoiceStatus("inProgress");
		o.setUpdateTime(oldUpdateTimeSec);
		return o;
	}

	private static OrderInvoice baseInvoice(long id, long companyId) {
		OrderInvoice o = new OrderInvoice();
		o.setId(id);
		o.setCompanyId(companyId);
		o.setOrderId("9001");
		o.setInvoiceType("enterprise");
		o.setInvoiceTypeCode("02");
		o.setCompanyTitle("T");
		o.setCompanyTaxNumber("N");
		o.setCompanyAddress("A");
		o.setCompanyTelephone("T");
		o.setBankName("B");
		o.setBankAccount("1");
		o.setEmail("e@e.com");
		o.setMobile("1");
		return o;
	}
}
