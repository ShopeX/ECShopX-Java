package cn.shopex.ecshopx.orders.service.normal.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.point.service.PointMemberMoneyToPointService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ECX-10614：积分抵扣行分摊不得留下 1 分现金尾差；行合计对得上整单。
 *
 * <p>样例来自生产单 5407893000230261（优惠后三行 16200/13000/22000 元，运费 2 元，积分抵 51201）。
 */
class NormalOrderPointDeductionItemShareEcX10614Test {

	private static final long COMPANY_ID = 1L;

	private PointMemberMoneyToPointService moneyToPointService;
	private NormalOrderPointDeductionApplyService service;

	@BeforeEach
	void setUp() {
		moneyToPointService = mock(PointMemberMoneyToPointService.class);
		// deduct_point=1：1 分钱 ↔ 1 积分（fen/100 后 *1 再 ceil）
		when(moneyToPointService.moneyToPoint(anyLong(), anyLong()))
				.thenAnswer(inv -> {
					long moneyFen = inv.getArgument(1);
					return moneyFen <= 0L ? 0L : (moneyFen + 99L) / 100L;
				});
		service = new NormalOrderPointDeductionApplyService(moneyToPointService);
	}

	@Test
	@DisplayName("商品全额积分抵扣：各行现金为 0，运费积分 1 + 现金 1")
	void fullGoodsPointDeductionLeavesZeroItemCashOnAllLines() {
		Map<String, Object> orderData = baseOrder(51201);
		service.apply(orderData, 51201, COMPANY_ID, 100_000L, openRuleWithFreight());

		List<Map<String, Object>> items = castItems(orderData);
		assertThat(items).extracting(it -> longVal(it.get("total_fee"))).containsOnly(0L);
		assertThat(longVal(orderData.get("freight_fee"))).isEqualTo(100L);
		assertThat(longVal(orderData.get("freight_point_fee"))).isEqualTo(100L);
		assertThat(intVal(orderData.get("point_fee"))).isEqualTo(5_120_100);
		assertThat(longVal(orderData.get("total_fee"))).isEqualTo(100L);
		assertOrderLineInvariants(orderData);
	}

	@Test
	@DisplayName("部分积分抵扣：行合计对得上整单，未覆盖运费时运费保持现金")
	void partialGoodsPointDeductionKeepsOrderLineInvariants() {
		Map<String, Object> orderData = baseOrder(25600);
		service.apply(orderData, 25600, COMPANY_ID, 100_000L, openRuleWithFreight());

		assertThat(intVal(orderData.get("point_fee"))).isEqualTo(2_560_000);
		long itemPointFeeSum =
				castItems(orderData).stream().mapToLong(it -> longVal(it.get("point_fee"))).sum();
		assertThat(itemPointFeeSum).isEqualTo(2_560_000L);
		assertThat(longVal(orderData.get("freight_fee"))).isEqualTo(200L);
		assertThat(longVal(orderData.get("freight_point_fee"))).isEqualTo(0L);
		assertThat(longVal(orderData.get("total_fee"))).isEqualTo(2_560_200L);
		assertOrderLineInvariants(orderData);
	}

	@Test
	@DisplayName("单商品行 + 运费混合抵扣：商品行现金为 0")
	void singleItemFullDeductionWithMixedFreightKeepsInvariants() {
		Map<String, Object> orderData = new LinkedHashMap<>();
		orderData.put("total_fee", 10200L);
		orderData.put("freight_fee", 200L);
		List<Map<String, Object>> items = new ArrayList<>();
		items.add(item(9001, "单SKU全额抵扣", 10000L, 1, 10000L));
		orderData.put("items", items);

		service.apply(orderData, 101, COMPANY_ID, 100_000L, openRuleWithFreight());

		assertThat(castItems(orderData)).hasSize(1);
		assertThat(longVal(castItems(orderData).get(0).get("total_fee"))).isEqualTo(0L);
		assertThat(longVal(castItems(orderData).get(0).get("point_fee"))).isEqualTo(10000L);
		assertThat(longVal(orderData.get("freight_fee"))).isEqualTo(100L);
		assertThat(longVal(orderData.get("freight_point_fee"))).isEqualTo(100L);
		assertThat(intVal(orderData.get("point_fee"))).isEqualTo(10100);
		assertThat(longVal(orderData.get("total_fee"))).isEqualTo(100L);
		assertOrderLineInvariants(orderData);
	}

	private Map<String, Object> baseOrder(int pointUse) {
		Map<String, Object> orderData = new LinkedHashMap<>();
		orderData.put("total_fee", 5_120_200L);
		orderData.put("freight_fee", 200L);
		List<Map<String, Object>> items = new ArrayList<>();
		items.add(item(6734, "冰川护盾户外滑雪服", 850000L, 2, 1_620_000L));
		items.add(item(6764, "Y硬壳冲锋衣", 980000L, 2, 1_300_000L));
		items.add(item(6749, "新款粉色羽绒服", 1_100_000L, 2, 2_200_000L));
		orderData.put("items", items);
		orderData.put("point_use", pointUse);
		return orderData;
	}

	private static Map<String, Object> item(int id, String name, long price, int num, long totalFee) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_id", id);
		m.put("item_name", name);
		m.put("price", price);
		m.put("num", num);
		m.put("total_fee", totalFee);
		return m;
	}

	private static Map<String, Object> openRuleWithFreight() {
		Map<String, Object> rule = new LinkedHashMap<>();
		rule.put("isOpenMemberPoint", "true");
		rule.put("isOpenDeductPoint", "true");
		rule.put("deduct_proportion_limit", 100);
		rule.put("deduct_point", "1");
		rule.put("can_deduct_freight", 1);
		rule.put("name", "积分");
		return rule;
	}

	private static void assertOrderLineInvariants(Map<String, Object> result) {
		long itemCash =
				castItems(result).stream().mapToLong(it -> longVal(it.get("total_fee"))).sum();
		long itemPointFee =
				castItems(result).stream().mapToLong(it -> longVal(it.get("point_fee"))).sum();
		assertThat(longVal(result.get("total_fee")))
				.as("Σ行现金 + 运费现金 应等于整单应付")
				.isEqualTo(itemCash + longVal(result.get("freight_fee")));
		assertThat(intVal(result.get("point_fee")))
				.as("Σ行积分抵扣 + 运费积分抵扣 应等于整单积分抵扣")
				.isEqualTo((int) (itemPointFee + longVal(result.get("freight_point_fee"))));
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> castItems(Map<String, Object> orderData) {
		return (List<Map<String, Object>>) orderData.get("items");
	}

	private static int intVal(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v));
	}

	private static long longVal(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v));
	}
}
