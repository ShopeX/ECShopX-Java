package cn.shopex.ecshopx.theme.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PagesTemplateFrontDetailResolveSupportTest {

	@Test
	@DisplayName("index_type=1 始终走平台池")
	void usePlatformTemplatePool_headquartersIndex() {
		assertTrue(PagesTemplateFrontDetailResolveSupport.usePlatformTemplatePool(1, false));
		assertTrue(PagesTemplateFrontDetailResolveSupport.usePlatformTemplatePool(1, true));
	}

	@Test
	@DisplayName("index_type=2 且 distributor_self 虚拟店走平台池")
	void usePlatformTemplatePool_shopIndexVirtual() {
		assertTrue(PagesTemplateFrontDetailResolveSupport.usePlatformTemplatePool(2, true));
		assertFalse(PagesTemplateFrontDetailResolveSupport.usePlatformTemplatePool(2, false));
	}

	@Test
	@DisplayName("distributor_self 真值判定")
	void isVirtualDistributorRow() {
		assertTrue(PagesTemplateFrontDetailResolveSupport.isVirtualDistributorRow(Map.of("distributor_self", 1)));
		assertTrue(PagesTemplateFrontDetailResolveSupport.isVirtualDistributorRow(Map.of("distributor_self", "1")));
		assertFalse(PagesTemplateFrontDetailResolveSupport.isVirtualDistributorRow(Map.of("distributor_self", 0)));
		assertFalse(PagesTemplateFrontDetailResolveSupport.isVirtualDistributorRow(Map.of()));
	}

	@Test
	@DisplayName("店铺首页 index 映射 distributor_index")
	void weappPagesForShopTemplateDb() {
		assertEquals("distributor_index", PagesTemplateFrontDetailResolveSupport.weappPagesForShopTemplateDb("index"));
		assertEquals("distributor_index", PagesTemplateFrontDetailResolveSupport.weappPagesForShopTemplateDb(null));
		assertEquals("cart", PagesTemplateFrontDetailResolveSupport.weappPagesForShopTemplateDb("cart"));
	}
}