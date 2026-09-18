package cn.shopex.ecshopx.employeepurchase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmployeePurchaseActivityItemDetailLimitFieldsTest {

	@Test
	void putActivityItemLimitFields_copiesFromActivityItem() {
		ActivityItems row = new ActivityItems();
		row.setLimitNum(3);
		row.setLimitFee(5000);
		Map<String, Object> target = new LinkedHashMap<>();

		EmployeePurchaseActivityItemDetailService.putActivityItemLimitFields(target, row);

		assertEquals(3, target.get("limit_num"));
		assertEquals(5000, target.get("limit_fee"));
	}

	@Test
	void putActivityItemLimitFields_whenMissing_defaultsZero() {
		Map<String, Object> target = new LinkedHashMap<>();

		EmployeePurchaseActivityItemDetailService.putActivityItemLimitFields(target, null);

		assertEquals(0, target.get("limit_num"));
		assertEquals(0, target.get("limit_fee"));
	}

	@Test
	void putActivityItemLimitFields_nullColumns_defaultsZero() {
		ActivityItems row = new ActivityItems();
		row.setLimitNum(null);
		row.setLimitFee(null);
		Map<String, Object> target = new LinkedHashMap<>();

		EmployeePurchaseActivityItemDetailService.putActivityItemLimitFields(target, row);

		assertEquals(0, target.get("limit_num"));
		assertEquals(0, target.get("limit_fee"));
	}

	@Test
	void isOnShelf_matchesPhpNullAsOnShelf() {
		assertFalse(EmployeePurchaseActivityItemDetailService.isOnShelf(null));

		ActivityItems on = new ActivityItems();
		on.setShelfStatus(1);
		assertTrue(EmployeePurchaseActivityItemDetailService.isOnShelf(on));

		ActivityItems implicit = new ActivityItems();
		implicit.setShelfStatus(null);
		assertTrue(EmployeePurchaseActivityItemDetailService.isOnShelf(implicit));

		ActivityItems off = new ActivityItems();
		off.setShelfStatus(0);
		assertFalse(EmployeePurchaseActivityItemDetailService.isOnShelf(off));
	}

	@Test
	void overlay_offShelfSku_zerosStoreLimitsAndInstock() {
		ActivityItems off = new ActivityItems();
		off.setShelfStatus(0);
		off.setActivityPrice(2100);
		off.setActivityStore(8);
		off.setLimitNum(3);
		off.setLimitFee(5000);
		Map<String, Object> target = new LinkedHashMap<>();
		target.put("activity_price", 9900);

		EmployeePurchaseActivityItemDetailService.overlayActivityItemSaleFields(target, off, false);

		assertEquals(9900, target.get("activity_price"));
		assertEquals(0, target.get("store"));
		assertEquals("instock", target.get("approve_status"));
		assertEquals(0, target.get("limit_num"));
		assertEquals(0, target.get("limit_fee"));
	}

	@Test
	void overlay_onShelfSku_copiesPriceStoreAndLimits() {
		ActivityItems on = new ActivityItems();
		on.setShelfStatus(1);
		on.setActivityPrice(2100);
		on.setActivityStore(8);
		on.setLimitNum(3);
		on.setLimitFee(5000);
		Map<String, Object> target = new LinkedHashMap<>();

		EmployeePurchaseActivityItemDetailService.overlayActivityItemSaleFields(target, on, false);

		assertEquals(2100, target.get("activity_price"));
		assertEquals(8, target.get("store"));
		assertEquals(3, target.get("limit_num"));
		assertEquals(5000, target.get("limit_fee"));
		assertFalse(target.containsKey("approve_status"));
	}
}
