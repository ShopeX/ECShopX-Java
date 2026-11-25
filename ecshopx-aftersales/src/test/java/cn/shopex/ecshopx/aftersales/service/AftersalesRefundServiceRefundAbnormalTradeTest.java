package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.AftersalesRefundJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesRefundAbnormalChannelPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class AftersalesRefundServiceRefundAbnormalTradeTest {

	private static final long COMPANY_ID = 9_001L;
	private static final long REFUND_BN = 211_506_110_001_001L;

	@Mock
	private AftersalesRefundMapper aftersalesRefundMapper;

	@Mock
	private AftersalesRefundJobDispatchPublisher aftersalesRefundJobDispatchPublisher;

	@Mock
	private AftersalesRefundAbnormalChannelPort aftersalesRefundAbnormalChannelPort;

	@Mock
	private ObjectProvider<OrderNormalOrderHeaderReadPort> orderNormalOrderHeaderReadPort;

	private AftersalesRefundService service;

	@BeforeEach
	void init() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(
				new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		service =
				new AftersalesRefundService(
						aftersalesRefundMapper,
						aftersalesRefundJobDispatchPublisher,
						aftersalesRefundAbnormalChannelPort,
						orderNormalOrderHeaderReadPort);
	}

	@Test
	@DisplayName("§4.3 成功：CAS→渠道 SUCCESS→退款单 SUCCESS")
	void refundAbnormalTrade_channelSuccess_marksSuccess() {
		AftersalesRefund pending = abnormalPending(AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL);
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(pending);
		when(aftersalesRefundMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> channelOk = new LinkedHashMap<>();
		channelOk.put("status", "SUCCESS");
		channelOk.put("refund_id", "RF-100");
		when(aftersalesRefundAbnormalChannelPort.dispatchAbnormalRefundChannel(COMPANY_ID, REFUND_BN))
				.thenReturn(channelOk);

		AftersalesRefund afterSuccess = successRefund(pending);
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(pending, afterSuccess);

		AftersalesRefund result = service.refundAbnormalTrade(COMPANY_ID, REFUND_BN);

		assertThat(result.getRefundStatus()).isEqualTo("SUCCESS");
		assertThat(result.getRefundId()).isEqualTo("RF-100");
		verify(aftersalesRefundAbnormalChannelPort, times(1))
				.dispatchAbnormalRefundChannel(COMPANY_ID, REFUND_BN);
		verify(aftersalesRefundMapper, times(2)).update(any(), any());
	}

	@Test
	@DisplayName("§4.3 失败：渠道 FAIL 恢复原异常状态")
	void refundAbnormalTrade_channelFail_restoresPriorStatus() {
		AftersalesRefund pending = abnormalPending(AftersalesRefundService.REFUND_STATUS_DUP_PAY);
		AftersalesRefund restored = abnormalPending(AftersalesRefundService.REFUND_STATUS_DUP_PAY);
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(pending, restored);

		when(aftersalesRefundMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> channelFail = new LinkedHashMap<>();
		channelFail.put("status", "FAIL");
		channelFail.put("error_desc", "channel declined");
		when(aftersalesRefundAbnormalChannelPort.dispatchAbnormalRefundChannel(COMPANY_ID, REFUND_BN))
				.thenReturn(channelFail);

		assertThatThrownBy(() -> service.refundAbnormalTrade(COMPANY_ID, REFUND_BN))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("channel declined");

		verify(aftersalesRefundMapper, times(2)).update(any(), any());
	}

	@Test
	@DisplayName("§4.3 SUCCESS 幂等：不调渠道")
	void refundAbnormalTrade_alreadySuccess_idempotent() {
		AftersalesRefund done = successRefund(abnormalPending(AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL));
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(done);

		AftersalesRefund result = service.refundAbnormalTrade(COMPANY_ID, REFUND_BN);

		assertThat(result.getRefundStatus()).isEqualTo("SUCCESS");
		verify(aftersalesRefundAbnormalChannelPort, never())
				.dispatchAbnormalRefundChannel(anyLong(), anyLong());
		verify(aftersalesRefundMapper, never()).update(any(), any());
	}

	@Test
	@DisplayName("§4.3 PROCESSING 拒绝并发")
	void refundAbnormalTrade_processing_rejects() {
		AftersalesRefund processing = abnormalPending(AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL);
		processing.setRefundStatus("PROCESSING");
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(processing);

		assertThatThrownBy(() -> service.refundAbnormalTrade(COMPANY_ID, REFUND_BN))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("处理中");

		verify(aftersalesRefundAbnormalChannelPort, never())
				.dispatchAbnormalRefundChannel(anyLong(), anyLong());
	}

	private static AftersalesRefund abnormalPending(String status) {
		AftersalesRefund r = new AftersalesRefund();
		r.setCompanyId(COMPANY_ID);
		r.setRefundBn(REFUND_BN);
		r.setOrderId(880_001L);
		r.setTradeId("T-ABN-1");
		r.setRefundStatus(status);
		r.setRefundFee(5_000);
		r.setRefundPoint(0);
		return r;
	}

	private static AftersalesRefund successRefund(AftersalesRefund base) {
		AftersalesRefund r = new AftersalesRefund();
		r.setCompanyId(base.getCompanyId());
		r.setRefundBn(base.getRefundBn());
		r.setOrderId(base.getOrderId());
		r.setTradeId(base.getTradeId());
		r.setRefundStatus("SUCCESS");
		r.setRefundId("RF-100");
		r.setRefundedFee(base.getRefundFee());
		r.setRefundedPoint(0);
		return r;
	}
}