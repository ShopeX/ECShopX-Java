package cn.shopex.ecshopx.common.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DualJwtSupportTest {

	@Test
	void prefersPosClaimsAndActingId() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(PosJwtRequestAttributes.POS_JWT_USER_DATA))
				.thenReturn(Map.of(
						"company_id",
						38L,
						"pos_store_account_id",
						99L,
						"distributor_id",
						3361L));
		assertTrue(DualJwtSupport.isPosSession(request));
		assertEquals(38L, DualJwtSupport.requireCompanyId(request));
		assertEquals(99L, DualJwtSupport.requireActingOperatorId(request));
	}

	@Test
	void fallsBackToOperatorClaims() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(PosJwtRequestAttributes.POS_JWT_USER_DATA)).thenReturn(null);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA))
				.thenReturn(Map.of("company_id", 1L, "operator_id", 7L));
		assertEquals(1L, DualJwtSupport.requireCompanyId(request));
		assertEquals(7L, DualJwtSupport.requireActingOperatorId(request));
	}
}
