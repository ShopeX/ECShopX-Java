package cn.shopex.ecshopx.orders.service.workwechat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SendDeliveryWaitDeliveryNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendDeliveryWaitZiTiNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeFinishWorkWechatNotifyServiceTest {

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private SendDeliveryWaitDeliveryNoticeJobDispatchPublisher sendDeliveryWaitDeliveryNoticeJobDispatchPublisher;

	@Mock
	private SendDeliveryWaitZiTiNoticeJobDispatchPublisher sendDeliveryWaitZiTiNoticeJobDispatchPublisher;

	@InjectMocks
	private TradeFinishWorkWechatNotifyService tradeFinishWorkWechatNotifyService;

	@Test
	void dispatchTradeFinishWorkWechatDeliveryWaitJobs_whenReceiptTypeLogistics_invokesDeliveryNoticeJobPublisherWithCompanyAndOrderId() {
		String expectedCompanyId = "9";
		String expectedOrderId = "501";
		NormalOrders row = new NormalOrders();
		row.setCompanyId(9L);
		row.setOrderId(501L);
		row.setReceiptType("logistics");
		when(normalOrdersMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", expectedCompanyId);
		payload.put("order_id", expectedOrderId);

		tradeFinishWorkWechatNotifyService.dispatchTradeFinishWorkWechatDeliveryWaitJobs(payload);

		verify(sendDeliveryWaitDeliveryNoticeJobDispatchPublisher)
				.publish(eq(expectedCompanyId), eq(expectedOrderId));
		verifyNoInteractions(sendDeliveryWaitZiTiNoticeJobDispatchPublisher);
	}

	@Test
	void dispatchTradeFinishWorkWechatDeliveryWaitJobs_whenReceiptTypeZiti_invokesZiTiNoticeJobPublisherWithCompanyAndOrderId() {
		String expectedCompanyId = "9";
		String expectedOrderId = "501";
		NormalOrders row = new NormalOrders();
		row.setCompanyId(9L);
		row.setOrderId(501L);
		row.setReceiptType("ziti");
		when(normalOrdersMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", expectedCompanyId);
		payload.put("order_id", expectedOrderId);

		tradeFinishWorkWechatNotifyService.dispatchTradeFinishWorkWechatDeliveryWaitJobs(payload);

		verify(sendDeliveryWaitZiTiNoticeJobDispatchPublisher)
				.publish(eq(expectedCompanyId), eq(expectedOrderId));
		verifyNoInteractions(sendDeliveryWaitDeliveryNoticeJobDispatchPublisher);
	}

	@Test
	void dispatchTradeFinishWorkWechatDeliveryWaitJobs_whenReceiptTypeMissing_doesNotInvokeEitherPublisher() {
		NormalOrders row = new NormalOrders();
		row.setCompanyId(9L);
		row.setOrderId(501L);
		row.setReceiptType("");
		when(normalOrdersMapper.selectOne(any())).thenReturn(row);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", "9");
		payload.put("order_id", "501");

		tradeFinishWorkWechatNotifyService.dispatchTradeFinishWorkWechatDeliveryWaitJobs(payload);

		verifyNoInteractions(sendDeliveryWaitDeliveryNoticeJobDispatchPublisher);
		verifyNoInteractions(sendDeliveryWaitZiTiNoticeJobDispatchPublisher);
	}
}
