package cn.shopex.ecshopx.orders.service.pos;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PosOrderDocAccessGuardTest {

	@Test
	void managerCanOperateAnyStoreOrder() {
		assertDoesNotThrow(
				() ->
						PosOrderDocAccessGuard.assertStoreAndClerkWrite(
								Map.of("distributor_id", 88L, "id", 1L, "role", "store_manager"), 88L, 0L));
	}

	@Test
	void crossStoreRejected() {
		assertThrows(
				ForbiddenException.class,
				() ->
						PosOrderDocAccessGuard.assertStoreAndClerkWrite(
								Map.of("distributor_id", 88L, "id", 1L, "role", "store_manager"), 99L, 1L));
	}

	@Test
	void clerkOwnOrderOk() {
		assertDoesNotThrow(
				() ->
						PosOrderDocAccessGuard.assertStoreAndClerkWrite(
								Map.of("distributor_id", 88L, "id", 9L, "role", "clerk"), 88L, 9L));
	}

	@Test
	void clerkZeroIdRejected() {
		assertThrows(
				ForbiddenException.class,
				() ->
						PosOrderDocAccessGuard.assertStoreAndClerkWrite(
								Map.of("distributor_id", 88L, "id", 9L, "role", "clerk"), 88L, 0L));
	}

	@Test
	void clerkOthersOrderRejected() {
		assertThrows(
				ForbiddenException.class,
				() ->
						PosOrderDocAccessGuard.assertStoreAndClerkWrite(
								Map.of("distributor_id", 88L, "id", 9L, "role", "clerk"), 88L, 10L));
	}
}
