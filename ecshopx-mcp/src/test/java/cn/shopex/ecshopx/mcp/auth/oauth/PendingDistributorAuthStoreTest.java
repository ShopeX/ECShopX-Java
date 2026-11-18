package cn.shopex.ecshopx.mcp.auth.oauth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingDistributorAuthStoreTest {

	@Test
	void putPeekConsumeLifecycle() {
		PendingDistributorAuthStore store = new PendingDistributorAuthStore();
		PendingDistributorAuthStore.PendingAuth auth = PendingDistributorAuthStore.PendingAuth.create(
				"client",
				"http://localhost/cb",
				"st",
				"challenge",
				"S256",
				"jwt",
				List.of(new PendingDistributorAuthStore.ShopOption(1L, "A")));

		String id = store.put(auth);
		assertTrue(store.peek(id).isPresent());
		assertEquals("jwt", store.peek(id).orElseThrow().businessJwt());
		assertTrue(store.consume(id).isPresent());
		assertTrue(store.peek(id).isEmpty());
	}

	@Test
	void expiredPendingIsRemovedOnPeek() throws InterruptedException {
		PendingDistributorAuthStore store = new PendingDistributorAuthStore(1L);
		String id = store.put(PendingDistributorAuthStore.PendingAuth.create(
				"client",
				"http://localhost/cb",
				null,
				"challenge",
				"S256",
				"jwt",
				List.of(new PendingDistributorAuthStore.ShopOption(1L, "A"))));
		Thread.sleep(1100L);
		assertTrue(store.peek(id).isEmpty());
	}
}
