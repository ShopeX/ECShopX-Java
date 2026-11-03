package cn.shopex.ecshopx.orders.service.pos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.auth.PosJwtRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class PosOrderBinderTest {

	@AfterEach
	void clear() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void withoutRequestReturnsZero() {
		assertEquals(0L, PosOrderBinder.currentPosStoreAccountId());
	}

	@Test
	void readsIdFromPosJwtAttribute() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(PosJwtRequestAttributes.POS_JWT_USER_DATA))
				.thenReturn(Map.of("pos_store_account_id", 42L));
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		assertEquals(42L, PosOrderBinder.currentPosStoreAccountId());
	}
}
