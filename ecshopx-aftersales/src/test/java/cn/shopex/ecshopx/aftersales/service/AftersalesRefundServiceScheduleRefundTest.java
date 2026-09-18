package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.AftersalesRefundJobDispatchPublisher;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class AftersalesRefundServiceScheduleRefundTest {

	@Mock
	private AftersalesRefundMapper aftersalesRefundMapper;

	@Mock
	private AftersalesRefundJobDispatchPublisher aftersalesRefundJobDispatchPublisher;

	private AftersalesRefundService service;
	private ListAppender<ILoggingEvent> listAppender;
	private Logger serviceLogger;

	@BeforeEach
	void init() {
		service = new AftersalesRefundService(aftersalesRefundMapper, aftersalesRefundJobDispatchPublisher);
		listAppender = new ListAppender<>();
		listAppender.start();
		listAppender.list.clear();
		serviceLogger = (Logger) LoggerFactory.getLogger(AftersalesRefundService.class);
		serviceLogger.addAppender(listAppender);
	}

	@AfterEach
	void tearDown() {
		serviceLogger.detachAppender(listAppender);
		listAppender.stop();
	}

	@Test
	@DisplayName("§3.1 1,2,3,3-A,4,7：空集，enqueue 0 次，return 0")
	void empty() {
		when(aftersalesRefundMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());
		assertThat(service.scheduleRefund()).isEqualTo(0);
		verify(aftersalesRefundJobDispatchPublisher, times(0)).publish(anyLong(), anyLong(), any(), anyInt());
		assertThat(listAppender.list)
				.anyMatch(
						e -> e.getLevel() == Level.INFO
								&& e.getFormattedMessage() != null
								&& e.getFormattedMessage().contains("无待排程")
								&& e.getFormattedMessage().contains("dispatched=0"));
	}

	@Test
	@DisplayName("First refund per order ID is scheduled with zero delay seconds")
	void firstOnOrderDelayZero() {
		AftersalesRefund r = row(1L, 10L, 100L, 200_000_000);
		when(aftersalesRefundMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(r));
		assertThat(service.scheduleRefund()).isEqualTo(1);
		verify(aftersalesRefundJobDispatchPublisher, times(1)).publish(eq(1L), eq(10L), eq(100L), eq(0));
	}

	@Test
	@DisplayName("§3.1-5-1 同单第二笔 delay 61、第三笔 121")
	void sameOrderSecondAndThirdDelays() {
		AftersalesRefund a = row(1L, 1L, 100L, 200_000_000);
		AftersalesRefund b = row(2L, 1L, 100L, 200_000_000);
		AftersalesRefund c = row(3L, 1L, 100L, 200_000_000);
		when(aftersalesRefundMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(a, b, c));
		assertThat(service.scheduleRefund()).isEqualTo(3);
		verify(aftersalesRefundJobDispatchPublisher).publish(eq(1L), eq(1L), eq(100L), eq(0));
		verify(aftersalesRefundJobDispatchPublisher).publish(eq(2L), eq(1L), eq(100L), eq(61));
		verify(aftersalesRefundJobDispatchPublisher).publish(eq(3L), eq(1L), eq(100L), eq(121));
	}

	@Test
	@DisplayName("§3.1-5-1,5-2 多订单 101,101,102 交错 delay 规则")
	void multiOrderInterleaved() {
		AftersalesRefund a = row(1L, 1L, 101L, 200_000_000);
		AftersalesRefund b = row(2L, 1L, 101L, 200_000_000);
		AftersalesRefund c = row(3L, 1L, 102L, 200_000_000);
		when(aftersalesRefundMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(a, b, c));
		assertThat(service.scheduleRefund()).isEqualTo(3);
		verify(aftersalesRefundJobDispatchPublisher).publish(eq(1L), eq(1L), eq(101L), eq(0));
		verify(aftersalesRefundJobDispatchPublisher).publish(eq(2L), eq(1L), eq(101L), eq(61));
		verify(aftersalesRefundJobDispatchPublisher).publish(eq(3L), eq(1L), eq(102L), eq(0));
	}

	/**
	 * plan §5.3：建单时间下界 1607616000（含）；边界前一秒 1607615999、非 {@code AUDIT_SUCCESS} 由 SQL
	 * 条件排除（{@link AftersalesRefundService#buildScheduleRefundCandidateQuery}）。
	 */
	@Test
	@DisplayName("§5.3 过滤边界：create_time>=1607616000（1607615999 排除）、refund_status=AUDIT_SUCCESS（非此状态排除）")
	@SuppressWarnings("unchecked")
	void queryFilterBoundary_minCreateTimeAndAuditStatus() {
		QueryWrapper<AftersalesRefund> w = service.buildScheduleRefundCandidateQuery();
		assertThat(w.getSqlSegment()).contains("refund_status");
		assertThat(w.getSqlSegment()).contains("create_time");
		assertThat(w.getSqlSegment()).contains(">=");
		assertThat(w.getParamNameValuePairs().values())
				.contains(AftersalesRefundService.SCHEDULE_REFUND_MIN_CREATE_TIME_EPOCH_SEC)
				.contains("AUDIT_SUCCESS");
	}

	@Test
	@DisplayName("§5.3 scheduleRefund 的 selectList 与候选查询同形（下界 1607616000 + AUDIT_SUCCESS）")
	@SuppressWarnings("unchecked")
	void scheduleRefund_passesCandidateQueryToMapper() {
		when(aftersalesRefundMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());
		service.scheduleRefund();
		ArgumentCaptor<QueryWrapper<AftersalesRefund>> cap = ArgumentCaptor.forClass(QueryWrapper.class);
		verify(aftersalesRefundMapper).selectList(cap.capture());
		QueryWrapper<AftersalesRefund> w = cap.getValue();
		assertThat(w.getSqlSegment()).contains(">=");
		assertThat(w.getParamNameValuePairs().values())
				.contains(AftersalesRefundService.SCHEDULE_REFUND_MIN_CREATE_TIME_EPOCH_SEC)
				.contains("AUDIT_SUCCESS");
	}

	@Test
	@DisplayName("§3.1-6/7：enqueue 抛错则异常外逸")
	void enqueueFailurePropagates() {
		AftersalesRefund r = row(1L, 1L, 1L, 200_000_000);
		when(aftersalesRefundMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(r));
		doThrow(new RuntimeException("redis down"))
				.when(aftersalesRefundJobDispatchPublisher)
				.publish(anyLong(), anyLong(), any(), anyInt());
		assertThatThrownBy(() -> service.scheduleRefund()).isInstanceOf(RuntimeException.class).hasMessageContaining("redis down");
	}

	private static AftersalesRefund row(long refundBn, long companyId, long orderId, int createTime) {
		AftersalesRefund r = new AftersalesRefund();
		r.setRefundBn(refundBn);
		r.setCompanyId(companyId);
		r.setOrderId(orderId);
		r.setCreateTime(createTime);
		r.setRefundStatus("AUDIT_SUCCESS");
		return r;
	}
}
