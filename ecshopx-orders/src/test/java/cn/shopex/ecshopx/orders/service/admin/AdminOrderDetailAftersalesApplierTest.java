package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.aftersales.service.AftersalesDetailAggregateService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminOrderDetailAftersalesApplierTest {

	@Test
	@DisplayName("未发货商品只给仅退款数量，不给退货退款数量")
	void unshippedItem_getsOnlyRefundQuota() {
		AdminOrderDetailAftersalesApplier applier = applier(0, 0, 0);
		Map<String, Object> orderInfo = orderInfo("PARTAIL", "PAYED");
		Map<String, Object> item = item(3, 0, 0);

		applier.apply(orderInfo, List.of(item));

		assertThat(item.get("left_aftersales_num")).isEqualTo(0);
		assertThat(item.get("left_refund_only_num")).isEqualTo(3);
		assertThat(orderInfo.get("can_apply_aftersales")).isEqualTo(1);
	}

	@Test
	@DisplayName("已有仅退款不扣减退货退款数量")
	void existingOnlyRefund_doesNotReduceReturnQuota() {
		AdminOrderDetailAftersalesApplier applier = applier(1, 0, 1);
		Map<String, Object> orderInfo = orderInfo("PARTAIL", "PAYED");
		Map<String, Object> item = item(3, 1, 0);

		applier.apply(orderInfo, List.of(item));

		assertThat(item.get("left_aftersales_num")).isEqualTo(1);
		assertThat(item.get("left_refund_only_num")).isEqualTo(1);
		assertThat(orderInfo.get("can_apply_aftersales")).isEqualTo(1);
	}

	@Test
	@DisplayName("已有退货退款与取消数量分别扣减对应额度")
	void existingReturnExchangeAndCancel_reduceRespectiveQuota() {
		AdminOrderDetailAftersalesApplier applier = applier(2, 1, 1);
		Map<String, Object> orderInfo = orderInfo("PARTAIL", "PAYED");
		Map<String, Object> item = item(3, 1, 1);

		applier.apply(orderInfo, List.of(item));

		assertThat(item.get("left_aftersales_num")).isEqualTo(0);
		assertThat(item.get("left_refund_only_num")).isEqualTo(0);
		assertThat(orderInfo.get("can_apply_aftersales")).isEqualTo(0);
	}

	@Test
	@DisplayName("整单待发货强制关闭售后")
	void pendingOrder_blocksAftersales() {
		AdminOrderDetailAftersalesApplier applier = applier(0, 0, 0);
		Map<String, Object> orderInfo = orderInfo("PENDING", "PAYED");
		Map<String, Object> item = item(3, 0, 0);

		applier.apply(orderInfo, List.of(item));

		assertThat(item.get("left_refund_only_num")).isEqualTo(3);
		assertThat(orderInfo.get("can_apply_aftersales")).isEqualTo(0);
	}

	@Test
	@DisplayName("已取消订单强制关闭售后")
	void canceledOrder_blocksAftersales() {
		AdminOrderDetailAftersalesApplier applier = applier(0, 0, 0);
		Map<String, Object> orderInfo = orderInfo("PARTAIL", "CANCEL");
		Map<String, Object> item = item(3, 0, 0);

		applier.apply(orderInfo, List.of(item));

		assertThat(orderInfo.get("can_apply_aftersales")).isEqualTo(0);
	}

	@Test
	@DisplayName("超过售后关闭时间不开放售后")
	void autoClosedItem_blocksAftersales() {
		AdminOrderDetailAftersalesApplier applier = applier(0, 0, 0);
		Map<String, Object> orderInfo = orderInfo("PARTAIL", "PAYED");
		Map<String, Object> item = item(3, 0, 0);
		item.put("auto_close_aftersales_time", 1);

		applier.apply(orderInfo, List.of(item));

		assertThat(orderInfo.get("can_apply_aftersales")).isEqualTo(0);
	}

	@Test
	@DisplayName("已有售后按明细扣减可退金额和积分")
	void existingAftersales_reducesRefundableFeeAndPoint() {
		AdminOrderDetailAftersalesApplier applier =
				applier(1, 0, 1, new AftersalesDetailAggregateService.AppliedRefundAmount(100, 10, true));
		Map<String, Object> orderInfo = orderInfo("PARTAIL", "PAYED");
		Map<String, Object> item = item(3, 1, 0);

		applier.apply(orderInfo, List.of(item));

		assertThat(item.get("after_sales_fee")).isEqualTo(Map.of("refund_fee", 100, "refund_point", 10));
		assertThat(item.get("remain_fee")).isEqualTo(194);
		assertThat(item.get("remain_point")).isEqualTo(10);
	}

	@Test
	@DisplayName("无售后时保留商品全额和积分")
	void noAftersales_keepsOriginalFeeAndPoint() {
		AdminOrderDetailAftersalesApplier applier = applier(0, 0, 0);
		Map<String, Object> orderInfo = orderInfo("PARTAIL", "PAYED");
		Map<String, Object> item = item(3, 1, 0);

		applier.apply(orderInfo, List.of(item));

		assertThat(item.get("after_sales_fee")).isEqualTo(0);
		assertThat(item.get("remain_fee")).isEqualTo(294);
		assertThat(item.get("remain_point")).isEqualTo(20);
	}

	private static AdminOrderDetailAftersalesApplier applier(
			int total, int returnExchange, int onlyRefund) {
		return new AdminOrderDetailAftersalesApplier(
				new StubAggregateService(total, returnExchange, onlyRefund));
	}

	private static AdminOrderDetailAftersalesApplier applier(
			int total,
			int returnExchange,
			int onlyRefund,
			AftersalesDetailAggregateService.AppliedRefundAmount refundAmount) {
		return new AdminOrderDetailAftersalesApplier(
				new StubAggregateService(total, returnExchange, onlyRefund, refundAmount));
	}

	private static Map<String, Object> orderInfo(String deliveryStatus, String orderStatus) {
		Map<String, Object> orderInfo = new HashMap<>();
		orderInfo.put("company_id", 141L);
		orderInfo.put("order_id", "5406561000261112");
		orderInfo.put("delivery_status", deliveryStatus);
		orderInfo.put("order_status", orderStatus);
		return orderInfo;
	}

	private static Map<String, Object> item(int num, int deliveryItemNum, int cancelItemNum) {
		Map<String, Object> item = new HashMap<>();
		item.put("id", 5996L);
		item.put("num", num);
		item.put("delivery_item_num", deliveryItemNum);
		item.put("cancel_item_num", cancelItemNum);
		item.put("total_fee", 294);
		item.put("point_fee", 20);
		return item;
	}

	private static class StubAggregateService extends AftersalesDetailAggregateService {

		private final int total;
		private final int returnExchange;
		private final int onlyRefund;
		private final AftersalesDetailAggregateService.AppliedRefundAmount refundAmount;

		StubAggregateService(int total, int returnExchange, int onlyRefund) {
			this(
					total,
					returnExchange,
					onlyRefund,
					new AftersalesDetailAggregateService.AppliedRefundAmount(0, 0, false));
		}

		StubAggregateService(
				int total,
				int returnExchange,
				int onlyRefund,
				AftersalesDetailAggregateService.AppliedRefundAmount refundAmount) {
			super(null);
			this.total = total;
			this.returnExchange = returnExchange;
			this.onlyRefund = onlyRefund;
			this.refundAmount = refundAmount;
		}

		@Override
		public int getAppliedNum(long companyId, String orderId, long subOrderId) {
			return total;
		}

		@Override
		public int getAppliedNumByType(
				long companyId, String orderId, long subOrderId, String... aftersalesTypes) {
			for (String type : aftersalesTypes) {
				if ("ONLY_REFUND".equals(type)) {
					return onlyRefund;
				}
			}
			return returnExchange;
		}

		@Override
		public AppliedRefundAmount getAppliedRefundAmount(
				long companyId, String orderId, long subOrderId) {
			return refundAmount;
		}
	}
}
