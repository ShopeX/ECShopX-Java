package cn.shopex.ecshopx.aftersales.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.TradeRefundFinishEventDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundOnlineRefundPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.TradeByIdReadPort;
import cn.shopex.ecshopx.common.port.promotions.BargainOrderActivityStatusPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Aligns offline-channel stubs with PHP {@code RefundJob} §售后:
 * {@code applyOfflineChannelHintFromOrder} ← order pay_type offline hint;
 * {@code updateRefundToSuccess} ← {@code updateOneBy(..., refund_status=SUCCESS)}.
 */
class DefaultAftersalesRefundJobSideEffectsOfflineStubsTest {

	private OrderNormalOrderHeaderReadPort orderHeaderReadPort;
	private AftersalesRefundMapper aftersalesRefundMapper;
	private DefaultAftersalesRefundJobSideEffects sideEffects;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesDetail.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
	}

	@BeforeEach
	void setUp() {
		orderHeaderReadPort = mock(OrderNormalOrderHeaderReadPort.class);
		aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		sideEffects =
				new DefaultAftersalesRefundJobSideEffects(
						mock(AftersalesRefundOnlineRefundPort.class),
						mock(TradeRefundFinishEventDispatchPublisher.class),
						mock(AftersalesMapper.class),
						mock(AftersalesDetailMapper.class),
						orderHeaderReadPort,
						aftersalesRefundMapper,
						mock(TradeByIdReadPort.class),
						mock(BargainOrderActivityStatusPort.class));
		when(aftersalesRefundMapper.update(isNull(), any())).thenReturn(1);
	}

	@Test
	@DisplayName("6-1：channel 非 offline 且订单 pay_type=offline → 内存改 channel=offline")
	void applyOfflineChannelHint_patchesWhenOrderPayTypeOffline() {
		AftersalesRefund refund = baseRefund();
		refund.setRefundChannel("original");
		Map<String, Object> header = new LinkedHashMap<>();
		header.put("pay_type", "offline");
		when(orderHeaderReadPort.getHeader(1L, 535L)).thenReturn(Optional.of(header));

		AftersalesRefund out = sideEffects.applyOfflineChannelHintFromOrder(refund);

		assertThat(out.getRefundChannel()).isEqualTo("offline");
		assertThat(out).isSameAs(refund);
		verify(aftersalesRefundMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("6-1：channel 已是 offline → 不查单、不改")
	void applyOfflineChannelHint_skipsWhenAlreadyOffline() {
		AftersalesRefund refund = baseRefund();
		refund.setRefundChannel("offline");

		AftersalesRefund out = sideEffects.applyOfflineChannelHintFromOrder(refund);

		assertThat(out.getRefundChannel()).isEqualTo("offline");
		verify(orderHeaderReadPort, never()).getHeader(anyLong(), anyLong());
	}

	@Test
	@DisplayName("6-1：订单 pay_type 非 offline → 保持原 channel")
	void applyOfflineChannelHint_keepsOriginalWhenOrderNotOffline() {
		AftersalesRefund refund = baseRefund();
		refund.setRefundChannel("original");
		Map<String, Object> header = new LinkedHashMap<>();
		header.put("pay_type", "pos");
		when(orderHeaderReadPort.getHeader(1L, 535L)).thenReturn(Optional.of(header));

		AftersalesRefund out = sideEffects.applyOfflineChannelHintFromOrder(refund);

		assertThat(out.getRefundChannel()).isEqualTo("original");
	}

	@Test
	@DisplayName("6-2：仅将退款单 refund_status 置 SUCCESS")
	void updateRefundToSuccess_setsRefundStatusSuccess() {
		AftersalesRefund refund = baseRefund();
		refund.setRefundStatus("AUDIT_SUCCESS");

		sideEffects.updateRefundToSuccess(refund);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<LambdaUpdateWrapper<AftersalesRefund>> cap =
				ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
		verify(aftersalesRefundMapper).update(isNull(), cap.capture());
		assertThat(cap.getValue().getSqlSet()).contains("refund_status");
		assertThat(cap.getValue().getParamNameValuePairs().values()).contains("SUCCESS");
		verify(orderHeaderReadPort, never()).getHeader(anyLong(), anyLong());
	}

	private static AftersalesRefund baseRefund() {
		AftersalesRefund refund = new AftersalesRefund();
		refund.setCompanyId(1L);
		refund.setOrderId(535L);
		refund.setRefundBn(2202608258337278667L);
		refund.setAftersalesBn(202608255427266L);
		return refund;
	}
}
