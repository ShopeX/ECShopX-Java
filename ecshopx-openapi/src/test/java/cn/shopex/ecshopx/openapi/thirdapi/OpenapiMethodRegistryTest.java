package cn.shopex.ecshopx.openapi.thirdapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.shopex.ecshopx.common.openapi.OpenapiMethodDescriptor;
import org.junit.jupiter.api.Test;

/** CSV 解析工具测试；网关路由已改为 {@link OpenapiHandlerForwardRegistry}，不再依赖 CSV。 */
class OpenapiMethodRegistryTest {

	@Test
	void parseLineSanitizesDirtyMethodName() {
		String line =
				"POST /api/openapi/ecx.salesperson.updateStores,OpenapiBundle,OpenapiBundle\\Http\\ThirdApi\\V1\\Action,ShopSalesperson,updateSalespersonStores'],ecshopx-openapi,ShopSalespersonController,openapi,v1,updateSalespersonStores'],,,,";
		OpenapiMethodDescriptor d = OpenapiMethodRegistry.parseLine(line).orElseThrow();
		assertEquals("updateSalespersonStores", d.javaMethod());
	}

	@Test
	void normalizeVersion() {
		assertEquals("2.0", OpenapiMethodRegistry.normalizeVersion("v2"));
		assertEquals("2.0", OpenapiMethodRegistry.normalizeVersion("2"));
		assertEquals("2.0", OpenapiMethodRegistry.normalizeVersion("2.0"));
	}
}
