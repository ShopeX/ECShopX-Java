package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.aftersales.service.AftersalesDetailAggregateService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderDetailAftersalesApplier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WxappOrderDetailOrderRecombineSupportTest {

	@Test
	@DisplayName("自提订单按商品数量计算可售后数量")
	void zitiOrder_usesItemNumAsDeliveredNum() {
		WxappOrderDetailOrderRecombineSupport support = support();
		Map<String, Object> orderInfo = orderInfo("ziti", "DONE");
		Map<String, Object> item = item(3, 0);

		support.applyIfPresent("normal", result(orderInfo, item));

		assertThat(item.get("delivery_item_num")).isEqualTo(3);
		assertThat(item.get("left_aftersales_num")).isEqualTo(3);
		assertThat(item.get("left_refund_only_num")).isEqualTo(0);
		assertThat(orderInfo.get("can_apply_aftersales")).isEqualTo(1);
	}

	@Test
	@DisplayName("非自提订单保持已发货数量")
	void normalOrder_keepsDeliveredNum() {
		WxappOrderDetailOrderRecombineSupport support = support();
		Map<String, Object> orderInfo = orderInfo("logistics", "PENDING");
		Map<String, Object> item = item(3, 1);

		support.applyIfPresent("normal", result(orderInfo, item));

		assertThat(item.get("delivery_item_num")).isEqualTo(1);
		assertThat(item.get("left_aftersales_num")).isEqualTo(1);
		assertThat(item.get("left_refund_only_num")).isEqualTo(2);
	}

	private static WxappOrderDetailOrderRecombineSupport support() {
		return new WxappOrderDetailOrderRecombineSupport(
				new AdminOrderDetailAftersalesApplier(new StubAggregateService()));
	}

	private static Map<String, Object> result(Object orderInfo, Object item) {
		Map<String, Object> order = new HashMap<>();
		order.put("orderInfo", orderInfo);
		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfoMap = (Map<String, Object>) orderInfo;
		orderInfoMap.put("items", List.of(item));
		return order;
	}

	private static Map<String, Object> orderInfo(String receiptType, String zitiStatus) {
		Map<String, Object> orderInfo = new HashMap<>();
		orderInfo.put("company_id", 141L);
		orderInfo.put("order_id", "5406561000261112");
		orderInfo.put("receipt_type", receiptType);
		orderInfo.put("ziti_status", zitiStatus);
		orderInfo.put("delivery_status", "PARTAIL");
		orderInfo.put("order_status", "PAYED");
		return orderInfo;
	}

	private static Map<String, Object> item(int num, int deliveryItemNum) {
		Map<String, Object> item = new HashMap<>();
		item.put("id", 5996L);
		item.put("num", num);
		item.put("delivery_item_num", deliveryItemNum);
		item.put("cancel_item_num", 0);
		return item;
	}

	private static class StubAggregateService extends AftersalesDetailAggregateService {

		StubAggregateService() {
			super(null);
		}

		@Override
		public int getAppliedNum(long companyId, String orderId, long subOrderId) {
			return 0;
		}

		@Override
		public int getAppliedNumByType(
				long companyId, String orderId, long subOrderId, String... aftersalesTypes) {
			return 0;
		}

		@Override
		public AppliedRefundAmount getAppliedRefundAmount(
				long companyId, String orderId, long subOrderId) {
			return new AppliedRefundAmount(0, 0, false);
		}
	}
}
