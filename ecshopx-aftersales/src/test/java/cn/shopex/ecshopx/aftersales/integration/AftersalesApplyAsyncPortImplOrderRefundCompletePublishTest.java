package cn.shopex.ecshopx.aftersales.integration;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyPostCommitCommand;
import cn.shopex.ecshopx.common.dispatch.OrderRefundCompleteJobDispatchPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AftersalesApplyAsyncPortImplOrderRefundCompletePublishTest {

	@Mock private OrderRefundCompleteJobDispatchPublisher orderRefundCompleteJobDispatchPublisher;

	private AftersalesApplyAsyncPortImpl port;

	@BeforeEach
	void setUp() {
		port = new AftersalesApplyAsyncPortImpl(orderRefundCompleteJobDispatchPublisher);
	}

	@Test
	void publish_once_whenOnlyRefund() {
		long companyId = 10L;
		long orderId = 100L;
		var cmd = new AftersalesApplyPostCommitCommand(companyId, orderId, "ONLY_REFUND", false, 555L, 777L);

		port.dispatchPostCommitSideEffects(cmd);

		verify(orderRefundCompleteJobDispatchPublisher).publish(companyId, orderId);
	}

	@Test
	void publish_never_whenRefundGoodsAndNotGoodsReturned() {
		long companyId = 10L;
		long orderId = 100L;
		var cmd = new AftersalesApplyPostCommitCommand(companyId, orderId, "REFUND_GOODS", false, 555L, 777L);

		port.dispatchPostCommitSideEffects(cmd);

		verify(orderRefundCompleteJobDispatchPublisher, never()).publish(anyLong(), anyLong());
	}

	@Test
	void publish_once_whenRefundGoodsAndGoodsReturned() {
		long companyId = 10L;
		long orderId = 100L;
		var cmd = new AftersalesApplyPostCommitCommand(companyId, orderId, "REFUND_GOODS", true, 555L, 777L);

		port.dispatchPostCommitSideEffects(cmd);

		verify(orderRefundCompleteJobDispatchPublisher).publish(companyId, orderId);
	}

	@Test
	void publish_never_whenOtherTypeAndNotGoodsReturned() {
		long companyId = 10L;
		long orderId = 100L;
		var cmd = new AftersalesApplyPostCommitCommand(companyId, orderId, "EXCHANGING_GOODS", false, 555L, 777L);

		port.dispatchPostCommitSideEffects(cmd);

		verify(orderRefundCompleteJobDispatchPublisher, never()).publish(anyLong(), anyLong());
	}

	@Test
	void publish_once_whenOtherTypeButGoodsReturned() {
		long companyId = 10L;
		long orderId = 100L;
		var cmd = new AftersalesApplyPostCommitCommand(companyId, orderId, "EXCHANGING_GOODS", true, 555L, 777L);

		port.dispatchPostCommitSideEffects(cmd);

		verify(orderRefundCompleteJobDispatchPublisher).publish(companyId, orderId);
	}
}
