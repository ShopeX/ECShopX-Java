package cn.shopex.ecshopx.companys.service.posauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PosPermissionMatrixTest {

	@Test
	void storeManagerHasAllEight() {
		Map<String, Boolean> p = PosPermissionMatrix.permissionsForRole("store_manager");
		assertEquals(8, p.size());
		assertTrue(p.values().stream().allMatch(Boolean::booleanValue));
		assertEquals("store_all", PosPermissionMatrix.docPolicyForRole("store_manager").get("operate"));
	}

	@Test
	void supervisorLacksStoreSetting() {
		Map<String, Boolean> p = PosPermissionMatrix.permissionsForRole("supervisor");
		assertTrue(p.get("refund"));
		assertFalse(p.get("store_setting"));
	}

	@Test
	void clerkOnlyCashierMemberShift() {
		Map<String, Boolean> p = PosPermissionMatrix.permissionsForRole("clerk");
		assertTrue(p.get("cashier"));
		assertTrue(p.get("member_query"));
		assertTrue(p.get("shift_handover"));
		assertFalse(p.get("refund"));
		assertFalse(p.get("day_close"));
		assertEquals("own_only", PosPermissionMatrix.docPolicyForRole("clerk").get("operate"));
		assertEquals("store_all", PosPermissionMatrix.docPolicyForRole("clerk").get("view"));
	}
}
