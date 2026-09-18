package cn.shopex.ecshopx.goods.service.ome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.api.admin.v1.SyncFromOmeController;
import cn.shopex.ecshopx.goods.dispatch.GetItemsSpecFromOmeJobDispatchPublisher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class OmeItemSpecSyncFacadeInitialDispatchTest {

	@Test
	void enqueueInitialSync_enqueuesFirstPage_andEndUnixNearNow() {
		GetItemsSpecFromOmeJobDispatchPublisher publisher = mock(GetItemsSpecFromOmeJobDispatchPublisher.class);
		OmeItemSpecSyncFacade sut = new OmeItemSpecSyncFacade(publisher);
		long companyId = 42L;

		sut.enqueueInitialSync(companyId);

		verify(publisher)
				.enqueueGetItemsSpecFromOme(
						eq(companyId),
						eq(1),
						longThat(
								ts ->
										Math.abs(ts - System.currentTimeMillis() / 1000L) <= 2L));
	}

	@Test
	void syncItemSpec_controller_delegatesToOmeItemSpecSyncFacadeWithCompanyIdFromJwtMap() {
		OmeBrandSyncFacade omeBrandSyncFacade = mock(OmeBrandSyncFacade.class);
		OmeItemCategorySyncFacade omeItemCategorySyncFacade = mock(OmeItemCategorySyncFacade.class);
		OmeItemSpecSyncFacade omeItemSpecSyncFacade =
				mock(OmeItemSpecSyncFacade.class, withSettings().strictness(Strictness.STRICT_STUBS));
		OmeItemsSyncFacade omeItemsSyncFacade = mock(OmeItemsSyncFacade.class);

		SyncFromOmeController controller =
				new SyncFromOmeController(
						omeBrandSyncFacade, omeItemCategorySyncFacade, omeItemSpecSyncFacade, omeItemsSyncFacade);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA))
				.thenReturn(Map.of("company_id", 99L));

		ResponseEntity<ApiResult<Map<String, Object>>> response = controller.syncItemSpec(request);

		verify(omeItemSpecSyncFacade).enqueueInitialSync(99L);
		assertEquals(200, response.getStatusCode().value());
		assertEquals(200, response.getBody().getCode());
		Map<String, Object> data = response.getBody().getData();
		assertTrue(Boolean.TRUE.equals(data.get("status")));
	}
}
