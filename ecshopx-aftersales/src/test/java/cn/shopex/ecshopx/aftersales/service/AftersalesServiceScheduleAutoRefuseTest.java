package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesAutoRefuseWxaTemplatePort;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesCancelNoticeJobPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderAutoCloseAftersalesCronPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.payment.AdapayScheduleAutoPaymentConfirmationPort;
import cn.shopex.ecshopx.common.port.payment.BspayScheduleAutoPaymentConfirmationPort;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesCancelDispatchPublisher;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AftersalesServiceScheduleAutoRefuseTest {

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

	private final JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher =
			mock(JushuitanTradeAftersalesDispatchPublisher.class);

	private final JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder =
			new JushuitanTradeAftersalesBusPayloadBuilder();

	private AftersalesService service;

	@BeforeEach
	void init() {
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
						TransactionOperations.withoutTransaction(),
						companysMapper,
						applicationEventPublisher,
						aftersalesCancelNoticeJobPort,
						jushuitanTradeAftersalesDispatchPublisher,
						jushuitanTradeAftersalesBusPayloadBuilder,
						mock(TradeAftersalesCancelDispatchPublisher.class),
						mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class));
	}

	@Test
	@DisplayName("步骤1+2+6+步骤5：count=0 早退出，无分页查询，返回 true")
	void whenNoRows_returnsTrueWithoutPageQuery() {
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(0L);
		assertThat(service.scheduleAutoRefuse()).isTrue();
		verify(aftersalesDetailMapper, never()).selectPage(any(), any());
	}

	@Test
	@DisplayName("步骤1+2+3+4+4.1+步骤5：count=25 分两页拉取明细，返回 true")
	void whenTwoPages_queriesSecondPage() {
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(25L);
		when(aftersalesDetailMapper.selectPage(any(), any()))
				.thenAnswer(
						invocation -> {
							Page<?> p = invocation.getArgument(0);
							if (p.getCurrent() == 1) {
								AftersalesDetail d1 = mockDetail(1L);
								d1.setCompanyId(1L);
								d1.setAftersalesBn(10L);
								d1.setNum(1);
								return pageOf(d1, 1, 20);
							}
							if (p.getCurrent() == 2) {
								AftersalesDetail d2 = mockDetail(2L);
								d2.setCompanyId(1L);
								d2.setAftersalesBn(10L);
								d2.setNum(1);
								return pageOf(d2, 2, 5);
							}
							return pageOf();
						});
		Aftersales main = new Aftersales();
		main.setAftersalesBn(10L);
		main.setCompanyId(1L);
		main.setUserId(2L);
		main.setOrderId(100L);
		main.setAftersalesType("REFUND_GOODS");
		main.setRefuseReason("old");
		main.setRefundFee(99);
		when(aftersalesMapper.selectOne(any())).thenReturn(main);
		when(aftersalesMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(eq(1L), eq(10L), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		assertThat(service.scheduleAutoRefuse()).isTrue();
		verify(aftersalesDetailMapper, times(2)).selectPage(any(), any());
	}

	@Test
	@DisplayName("4.2.1+步骤5：主表 selectOne 为 null 时跳过，无 aftersales update、无 wxa、无 OrderProcessLog，返回 true")
	void whenMainRowMissing_skipsWithoutUpdateWxaOrLog() {
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(1L);
		when(aftersalesDetailMapper.selectPage(any(), any()))
				.thenAnswer(
						invocation -> {
							AftersalesDetail d = mockDetail(9L);
							d.setAftersalesBn(10L);
							d.setCompanyId(1L);
							return pageOf(d, 1, 1);
						});
		when(aftersalesMapper.selectOne(any())).thenReturn(null);
		assertThat(service.scheduleAutoRefuse()).isTrue();
		verify(aftersalesMapper, never()).update(isNull(), any(UpdateWrapper.class));
		verify(aftersalesAutoRefuseWxaTemplatePort, never()).sendSellerRefuseBuyer(any());
		verify(orderProcessLogPublishPort, never()).publish(any());
	}

	@Test
	@DisplayName("4.2.2：templateData 中 refuse_reason 取更新前主表内存值")
	void templateDataUsesRefuseReasonBeforeUpdate() {
		Aftersales main = new Aftersales();
		main.setAftersalesBn(1L);
		main.setCompanyId(1L);
		main.setUserId(1L);
		main.setOrderId(1L);
		main.setAftersalesType("REFUND_GOODS");
		main.setRefuseReason("保留旧因");
		main.setRefundFee(100);
		AftersalesDetail d = new AftersalesDetail();
		d.setItemName("X");
		Map<String, Object> t = service.buildAutoRefuseTemplateData(main, d);
		assertThat(t.get("refuse_reason")).isEqualTo("保留旧因");
		assertThat(t.get("item_name")).isEqualTo("X");
	}

	@Test
	@DisplayName("4.2.4/4.2.5+步骤5：退款 update 0 行抛错，无 wxa、无 OrderProcessLog，返回 true")
	void refundUpdateZero_skipsWxaAndLog() {
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(1L);
		when(aftersalesDetailMapper.selectPage(any(), any()))
				.thenAnswer(
						invocation -> {
							AftersalesDetail d = mockDetail(9L);
							d.setAftersalesBn(10L);
							d.setCompanyId(1L);
							return pageOf(d, 1, 1);
						});
		Aftersales main = new Aftersales();
		main.setAftersalesBn(10L);
		main.setCompanyId(1L);
		main.setUserId(1L);
		main.setOrderId(1L);
		main.setAftersalesType("REFUND_GOODS");
		main.setRefuseReason("x");
		main.setRefundFee(1);
		when(aftersalesMapper.selectOne(any())).thenReturn(main);
		when(aftersalesMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(eq(1L), eq(10L), any())).thenReturn(0);
		assertThat(service.scheduleAutoRefuse()).isTrue();
		verify(aftersalesMapper, times(1)).update(isNull(), any(UpdateWrapper.class));
		verify(aftersalesAutoRefuseWxaTemplatePort, never()).sendSellerRefuseBuyer(any());
		verify(orderProcessLogPublishPort, never()).publish(any());
	}

	@Test
	@DisplayName("4.2.2+4.2.3+4.2.4+步骤5：整单成功路径，wxa 与 OrderProcessLog 各 1 次，返回 true")
	void fullPath_callsWxaAndLog() {
		// 与 refundUpdateZero 相同的打桩结构（该用例可稳定进入主表 UPDATE+后续）
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(1L);
		when(aftersalesDetailMapper.selectPage(any(), any()))
				.thenAnswer(
						invocation -> {
							AftersalesDetail d = mockDetail(9L);
							d.setAftersalesBn(10L);
							d.setCompanyId(1L);
							return pageOf(d, 1, 1);
						});
		Aftersales main = new Aftersales();
		main.setAftersalesBn(10L);
		main.setCompanyId(1L);
		main.setUserId(1L);
		main.setOrderId(20L);
		main.setAftersalesType("REFUND_GOODS");
		main.setRefuseReason("原因");
		main.setRefundFee(1);
		when(aftersalesMapper.selectOne(any())).thenReturn(main);
		when(aftersalesMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(eq(1L), eq(10L), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		assertThat(service.scheduleAutoRefuse()).isTrue();
		verify(aftersalesMapper, atLeast(1)).update(isNull(), any(UpdateWrapper.class));
		verify(aftersalesAutoRefuseWxaTemplatePort, times(1)).sendSellerRefuseBuyer(any());
		ArgumentCaptor<Map<String, Object>> logCap = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(logCap.capture());
		assertThat(logCap.getValue().get("order_id")).isEqualTo(20L);
		assertThat(logCap.getValue().get("company_id")).isEqualTo(1L);
		assertThat((String) logCap.getValue().get("detail"))
				.contains("自动驳回")
				.contains("未收到商品自动驳回");
	}

	@Test
	@DisplayName("4.2.5+步骤5：Wxa sendSellerRefuseBuyer 抛错，无 OrderProcessLog publish，返回 true")
	void wxaThrows_skipsLog() {
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(1L);
		when(aftersalesDetailMapper.selectPage(any(), any()))
				.thenAnswer(
						invocation -> {
							AftersalesDetail d = mockDetail(1L);
							d.setAftersalesBn(10L);
							d.setCompanyId(1L);
							return pageOf(d, 1, 1);
						});
		Aftersales main = new Aftersales();
		main.setAftersalesBn(10L);
		main.setCompanyId(1L);
		main.setUserId(1L);
		main.setOrderId(1L);
		main.setAftersalesType("REFUND_GOODS");
		main.setRefuseReason("r");
		main.setRefundFee(1);
		when(aftersalesMapper.selectOne(any())).thenReturn(main);
		when(aftersalesMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(eq(1L), eq(10L), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		doThrow(new RuntimeException("wxa"))
				.when(aftersalesAutoRefuseWxaTemplatePort)
				.sendSellerRefuseBuyer(any());
		assertThat(service.scheduleAutoRefuse()).isTrue();
		verify(orderProcessLogPublishPort, never()).publish(any());
	}

	@Test
	@DisplayName("4.2.5+步骤5：NormalOrders addLeftAftersalesNum 抛错，无 wxa、无 OrderProcessLog，返回 true")
	void normalOrderWriteThrows_skipsWxaAndLog() {
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(1L);
		when(aftersalesDetailMapper.selectPage(any(), any()))
				.thenAnswer(
						invocation -> {
							AftersalesDetail d = mockDetail(9L);
							d.setAftersalesBn(10L);
							d.setCompanyId(1L);
							d.setNum(3);
							return pageOf(d, 1, 1);
						});
		Aftersales main = new Aftersales();
		main.setAftersalesBn(10L);
		main.setCompanyId(1L);
		main.setUserId(1L);
		main.setOrderId(99L);
		main.setAftersalesType("REFUND_GOODS");
		main.setRefuseReason("x");
		main.setRefundFee(1);
		when(aftersalesMapper.selectOne(any())).thenReturn(main);
		when(aftersalesMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(eq(1L), eq(10L), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
		doThrow(new RuntimeException("normal-orders"))
				.when(normalOrderLeftAftersalesWritePort)
				.addLeftAftersalesNum(eq(1L), eq(99L), eq(3));
		assertThat(service.scheduleAutoRefuse()).isTrue();
		verify(aftersalesAutoRefuseWxaTemplatePort, never()).sendSellerRefuseBuyer(any());
		verify(orderProcessLogPublishPort, never()).publish(any());
	}

	private static AftersalesDetail mockDetail(long detailId) {
		AftersalesDetail d = new AftersalesDetail();
		d.setDetailId(detailId);
		return d;
	}

	@SuppressWarnings("unchecked")
	private static IPage<AftersalesDetail> pageOf(AftersalesDetail d, int current, int size) {
		Page<AftersalesDetail> p = new Page<>(current, size);
		if (d == null) {
			p.setRecords(List.of());
			return p;
		}
		p.setRecords(List.of(d));
		return p;
	}

	private static IPage<AftersalesDetail> pageOf() {
		Page<AftersalesDetail> p = new Page<>();
		p.setRecords(List.of());
		return p;
	}
}
