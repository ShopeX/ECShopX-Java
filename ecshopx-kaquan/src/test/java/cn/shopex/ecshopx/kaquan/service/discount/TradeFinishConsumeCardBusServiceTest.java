package cn.shopex.ecshopx.kaquan.service.discount;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TradeFinishConsumeCardBusServiceTest {

	@Mock
	private UserDiscountConsumCardService userDiscountConsumCardService;

	private TradeFinishConsumeCardBusService underTest;

	@BeforeEach
	void setUp() {
		underTest = new TradeFinishConsumeCardBusService(userDiscountConsumCardService, new ObjectMapper());
	}

	@Test
	void onTradeFinishTradeRow_point_earlyReturn_noConsume() {
		Map<String, Object> row = baseRow();
		row.put("pay_type", "point");
		row.put("discount_info", "[{\"coupon_code\":\"C1\"}]");

		underTest.onTradeFinishTradeRow(row);

		verify(userDiscountConsumCardService, never()).consumeCouponForTradeFinishPayBill(
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void onTradeFinishTradeRow_scalarCoupon_invokesConsumeOnce() {
		Map<String, Object> row = baseRow();
		row.put("pay_type", "wxpay");
		row.put("discount_info", "[{\"coupon_code\":\"ONLY\"}]");

		underTest.onTradeFinishTradeRow(row);

		verify(userDiscountConsumCardService, times(1))
				.consumeCouponForTradeFinishPayBill(11L, 22L, "ONLY", "T-9", "500", "33");
	}

	@Test
	void onTradeFinishTradeRow_listCoupon_invokesConsumePerCode() {
		Map<String, Object> row = baseRow();
		row.put("pay_type", "wxpay");
		row.put("discount_info", "[{\"coupon_code\":[\" A \",\"B\"]}]");

		underTest.onTradeFinishTradeRow(row);

		verify(userDiscountConsumCardService, times(1)).consumeCouponForTradeFinishPayBill(eq(11L), eq(22L), eq("A"), eq("T-9"), eq("500"), eq("33"));
		verify(userDiscountConsumCardService, times(1)).consumeCouponForTradeFinishPayBill(eq(11L), eq(22L), eq("B"), eq("T-9"), eq("500"), eq("33"));
	}

	@Test
	void onTradeFinishTradeRow_firstCouponConsumeThrows_secondCouponStillConsumed() {
		doThrow(new RuntimeException("first fails"))
				.doNothing()
				.when(userDiscountConsumCardService)
				.consumeCouponForTradeFinishPayBill(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString());

		Map<String, Object> row = baseRow();
		row.put("pay_type", "wxpay");
		row.put("discount_info", "[{\"coupon_code\":[\"FIRST\",\"SECOND\"]}]");

		underTest.onTradeFinishTradeRow(row);

		verify(userDiscountConsumCardService, times(1))
				.consumeCouponForTradeFinishPayBill(11L, 22L, "FIRST", "T-9", "500", "33");
		verify(userDiscountConsumCardService, times(1))
				.consumeCouponForTradeFinishPayBill(11L, 22L, "SECOND", "T-9", "500", "33");
	}

	private static Map<String, Object> baseRow() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 11L);
		row.put("user_id", 22);
		row.put("order_id", "T-9");
		row.put("pay_fee", 500);
		row.put("shop_id", "33");
		return row;
	}
}
