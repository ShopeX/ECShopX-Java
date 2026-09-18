package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierLinkedMainItemIdResolverTest {

	@Mock
	private ItemsRepository itemsRepository;

	@Test
	void resolve_platformPoolOnlyWhenApproved() {
		Items pending = pool(88, 5001L, "processing");
		Items approved = pool(88, 5002L, "approved");
		when(itemsRepository.listByCompanyIdAndSupplierItemIds(7001L, List.of(88L))).thenReturn(List.of(pending, approved));

		Map<Integer, Long> out = SupplierLinkedMainItemIdResolver.resolve(itemsRepository, 7001L, List.of(88L), 0L);
		assertEquals(5002L, out.get(88));
	}

	@Test
	void resolve_returnsEmptyWhenNotApproved() {
		when(itemsRepository.listByCompanyIdAndSupplierItemIds(7001L, List.of(88L)))
				.thenReturn(List.of(pool(88, 5001L, "processing")));

		Map<Integer, Long> out = SupplierLinkedMainItemIdResolver.resolve(itemsRepository, 7001L, List.of(88L), 0L);
		assertTrue(out.isEmpty());
	}

	@Test
	void resolve_shopScopeUsesMatchingDistributorRow() {
		Items pool = pool(99, 5001L, "approved");
		Items shop = shop(99, 6001L, 88, "approved");
		when(itemsRepository.listByCompanyIdAndSupplierItemIds(7001L, List.of(99L))).thenReturn(List.of(pool, shop));

		Map<Integer, Long> out = SupplierLinkedMainItemIdResolver.resolve(itemsRepository, 7001L, List.of(99L), 88L);
		assertEquals(6001L, out.get(99));
	}

	@Test
	void resolveScopeDistributorId_usesJwtDistributorIdForShopOperator() {
		Map<String, Object> jwt = Map.of(
				"operator_type", "distributor",
				"operator_id", 10001,
				"distributor_id", 285);
		assertEquals(285L, SupplierLinkedMainItemIdResolver.resolveScopeDistributorId(jwt, Map.of()));
	}

	@Test
	void resolveScopeDistributorId_ignoresOperatorIdWhenDistributorIdMissing() {
		Map<String, Object> jwt = Map.of("operator_type", "distributor", "operator_id", 10001);
		assertEquals(0L, SupplierLinkedMainItemIdResolver.resolveScopeDistributorId(jwt, Map.of()));
	}

	@Test
	void isApprovedLinkedItem() {
		assertTrue(SupplierLinkedMainItemIdResolver.isApprovedLinkedItem(pool(1, 1L, "approved")));
		assertFalse(SupplierLinkedMainItemIdResolver.isApprovedLinkedItem(pool(1, 1L, "processing")));
	}

	private static Items pool(int supplierItemId, long itemId, String auditStatus) {
		Items it = new Items();
		it.setSupplierItemId(supplierItemId);
		it.setItemId(itemId);
		it.setDistributorId(0);
		it.setAuditStatus(auditStatus);
		return it;
	}

	private static Items shop(int supplierItemId, long itemId, int distributorId, String auditStatus) {
		Items it = pool(supplierItemId, itemId, auditStatus);
		it.setDistributorId(distributorId);
		return it;
	}
}
