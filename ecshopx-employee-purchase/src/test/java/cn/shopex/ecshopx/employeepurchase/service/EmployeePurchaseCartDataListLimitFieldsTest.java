package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.domain.MemberActivityItemsAggregate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmployeePurchaseCartDataListLimitFieldsTest {

	@Test
	void attachItemLimitFields_copiesLimitsAndAggregates() {
		ActivityItems act = new ActivityItems();
		act.setItemId(9L);
		act.setLimitNum(3);
		act.setLimitFee(5000);
		MemberActivityItemsAggregate agg = new MemberActivityItemsAggregate();
		agg.setItemId(9L);
		agg.setAggregateNum(1);
		agg.setAggregateFee(1200);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", 9L);
		row.put("item_name", "内购商品");
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row);

		EmployeePurchaseCartDataListService.attachItemLimitFields(
				rows, Map.of(9L, act), Map.of(9L, agg));

		assertEquals(3, row.get("limit_num"));
		assertEquals(5000, row.get("limit_fee"));
		assertEquals(1, row.get("aggregate_num"));
		assertEquals(1200, row.get("aggregate_fee"));
	}

	@Test
	void attachItemLimitFields_whenMissing_defaultsZero() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", 9L);
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row);

		EmployeePurchaseCartDataListService.attachItemLimitFields(rows, Map.of(), Map.of());

		assertEquals(0, row.get("limit_num"));
		assertEquals(0, row.get("limit_fee"));
		assertEquals(0, row.get("aggregate_num"));
		assertEquals(0, row.get("aggregate_fee"));
	}
}
