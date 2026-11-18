package cn.shopex.ecshopx.mcp.auth.oauth;

import cn.shopex.ecshopx.mcp.loopback.ApiLoopbackClient;
import cn.shopex.ecshopx.mcp.loopback.LoopbackRequest;
import cn.shopex.ecshopx.mcp.loopback.LoopbackResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DistributorShopSelectionServiceTest {

	private ApiLoopbackClient loopbackClient;

	private DistributorShopSelectionService service;

	@BeforeEach
	void setUp() {
		loopbackClient = mock(ApiLoopbackClient.class);
		service = new DistributorShopSelectionService(loopbackClient, new ObjectMapper());
	}

	@Test
	void listShopsParsesDistributorRows() {
		when(loopbackClient.execute(any())).thenReturn(new LoopbackResponse(200, """
				{"code":200,"data":{"list":[{"distributor_id":11,"name":"店铺A"},{"shop_id":22,"shop_name":"店铺B"}]}}
				"""));

		DistributorShopSelectionService.ListResult result = service.listShops("raw.jwt");

		assertTrue(result.success());
		assertEquals(2, result.shops().size());
		assertEquals(11L, result.shops().get(0).distributorId());
		assertEquals("店铺A", result.shops().get(0).name());
		assertEquals(22L, result.shops().get(1).distributorId());
		assertEquals("店铺B", result.shops().get(1).name());

		ArgumentCaptor<LoopbackRequest> captor = ArgumentCaptor.forClass(LoopbackRequest.class);
		verify(loopbackClient).execute(captor.capture());
		LoopbackRequest request = captor.getValue();
		assertEquals("GET", request.method());
		assertEquals(DistributorShopSelectionService.LIST_PATH, request.path());
		assertEquals("Bearer raw.jwt", request.headers().get("Authorization"));
		assertEquals(1, request.query().get("page"));
		assertEquals(100, request.query().get("pageSize"));
		assertEquals(true, request.query().get("is_all"));
	}

	@Test
	void listShopsFailsOnBizCode() {
		when(loopbackClient.execute(any())).thenReturn(new LoopbackResponse(200, """
				{"code":422,"msg":"未选择店铺"}
				"""));

		DistributorShopSelectionService.ListResult result = service.listShops("Bearer x");
		assertFalse(result.success());
		assertEquals("未选择店铺", result.errorMessage());
	}

	@Test
	void selectShopPostsSetDistributorId() {
		when(loopbackClient.execute(any())).thenReturn(new LoopbackResponse(200, """
				{"code":200,"data":{"status":true}}
				"""));

		DistributorShopSelectionService.SelectResult result = service.selectShop("Bearer tok", 99L);
		assertTrue(result.success());
		assertEquals(null, result.errorMessage());

		ArgumentCaptor<LoopbackRequest> captor = ArgumentCaptor.forClass(LoopbackRequest.class);
		verify(loopbackClient).execute(captor.capture());
		LoopbackRequest request = captor.getValue();
		assertEquals("POST", request.method());
		assertEquals(DistributorShopSelectionService.SELECT_PATH, request.path());
		assertTrue(request.body().contains("\"set_distributor_id\":99"));
	}

	@Test
	void selectShopRejectsNonPositiveId() {
		DistributorShopSelectionService.SelectResult result = service.selectShop("tok", 0L);
		assertFalse(result.success());
		assertEquals("set_distributor_id is required", result.errorMessage());
	}
}
