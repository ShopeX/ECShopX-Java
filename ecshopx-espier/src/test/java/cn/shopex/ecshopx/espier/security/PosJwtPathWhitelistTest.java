package cn.shopex.ecshopx.espier.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PosJwtPathWhitelistTest {

	@Test
	void allowsAuthAndStorefrontPrefixes() {
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/pos/auth/login"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/pos/auth/logout"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/pos/auth/me"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/order/create"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/order/payment"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/aftersales/apply"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/checkout"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/operator/cartdataadd"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/operator/cartdatalist"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/operator/cartdataupdate"));
		assertTrue(PosJwtPathWhitelist.isAllowed("DELETE", "/api/v1/operator/cartdatadel"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/operator/scancodeAddcart"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/operator/pending/add"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/operator/pending/list"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/orders"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/goods/items/onsale"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/goods/items"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/distributors"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/member/image/code"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/member/sms/code"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/member"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/member"));
		assertTrue(PosJwtPathWhitelist.isAllowed("PATCH", "/api/v1/member"));
		assertTrue(PosJwtPathWhitelist.isAllowed("PUT", "/api/v1/member"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/trade/logistics/list"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/members"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/account/management"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/account/management/1"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/company/logistics/list"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/company/logistics/create"));
		assertTrue(PosJwtPathWhitelist.isAllowed("DELETE", "/api/v1/company/logistics/1"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/member/tag"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/member/tag"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/member/tagsearch"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/member/tagcategory"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/member/tag-group"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/member/reltag"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/member/reltagdel"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/getUserCardList"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/espier/address"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/delivery"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/qr_writeoff"));
		assertTrue(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/writeoff/100"));
		assertTrue(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/writeoff/100"));
	}

	@Test
	void deniesAdminPosAccounts() {
		assertFalse(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/store/pos-accounts"));
		assertTrue(PosJwtPathWhitelist.isExplicitlyDeniedAdmin("/api/v1/store/pos-accounts/1"));
		assertFalse(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/operator/employees"));
		assertFalse(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/goods/items"));
		assertFalse(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/distributors/info"));
		assertFalse(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/members"));
		assertFalse(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/members/register/setting"));
		assertFalse(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/member/image/code"));
		assertFalse(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/account/management"));
		assertFalse(PosJwtPathWhitelist.isAllowed("PATCH", "/api/v1/account/management/1"));
		assertFalse(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/trade/logistics/list"));
		assertFalse(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/member/grade"));
		assertFalse(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/delivery"));
		assertFalse(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/delivery/lists"));
		assertFalse(PosJwtPathWhitelist.isAllowed("GET", "/api/v1/qr_writeoff"));
		assertFalse(PosJwtPathWhitelist.isAllowed("POST", "/api/v1/writeoff"));
	}
}
