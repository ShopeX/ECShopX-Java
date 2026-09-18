package cn.shopex.ecshopx.goods.service.items;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.goods.dispatch.ItemDeleteEventDispatchPublisher;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTypeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.promotions.repository.MemberPriceBulkDeleteRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@ExtendWith(MockitoExtension.class)
class PlatformItemsDeleteServiceItemDeleteEventPublishProbeTest {

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private ItemStoreService itemStoreService;

	@Mock
	private ItemRelAttributesRepository itemRelAttributesRepository;

	@Mock
	private ItemsRelCatsRepository itemsRelCatsRepository;

	@Mock
	private ItemsRelTypeRepository itemsRelTypeRepository;

	@Mock
	private DistributorItemsRepository distributorItemsRepository;

	@Mock
	private MemberPriceBulkDeleteRepository memberPriceBulkDeleteRepository;

	@Mock
	private ItemsBarcodeRepository itemsBarcodeRepository;

	@Mock
	private ItemDeleteEventDispatchPublisher itemDeleteEventDispatchPublisher;

	@InjectMocks
	private PlatformItemsDeleteService platformItemsDeleteService;

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void deletePlatformItems_afterCommit_invokesPublishOnce_withItemDeletePayloadShape() {
		long itemId = 4401L;
		long companyId = 88L;
		Items row = new Items();
		row.setItemId(itemId);
		row.setCompanyId(companyId);
		row.setNospec("true");
		row.setDistributorId(0);
		row.setItemBn("probe-bn");
		row.setItemType("services");
		row.setDefaultItemId(itemId);

		when(itemsRepository.getByItemIdAndCompany(itemId, companyId)).thenReturn(row);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
				inv -> {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization synchronization :
								TransactionSynchronizationManager.getSynchronizations()) {
							synchronization.afterCommit();
						}
						TransactionSynchronizationManager.clear();
					}
					return null;
				})
				.when(txMgr)
				.commit(any(TransactionStatus.class));

		TransactionTemplate tt = new TransactionTemplate(txMgr);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);

		tt.executeWithoutResult(st -> platformItemsDeleteService.deletePlatformItems(merged, itemId, 0L));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(itemDeleteEventDispatchPublisher).publishAfterCommit(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertThat(((Number) payload.get("item_id")).longValue()).isEqualTo(itemId);
		assertThat(((Number) payload.get("company_id")).longValue()).isEqualTo(companyId);
		assertThat(payload.get("del_ids")).isEqualTo(List.of(itemId));
		@SuppressWarnings("unchecked")
		Map<String, Object> itemInfo = (Map<String, Object>) payload.get("item_info");
		assertThat(itemInfo).isNotNull();
		assertThat(itemInfo.get("item_bn")).isEqualTo("probe-bn");
		assertThat(((Number) itemInfo.get("item_id")).longValue()).isEqualTo(itemId);
		assertThat(((Number) itemInfo.get("company_id")).longValue()).isEqualTo(companyId);
	}

	/**
	 * Mirrors {@code ItemsController#deleteItemsResponseData} query parsing: non-blank
	 * {@code distributor_id} uses {@link LeadingNumberParser#parseAsLong(String)}; merged map is only
	 * {@code company_id}. Asserts the same publisher invocation path as the default-distributor case.
	 */
	@Test
	void deletePlatformItems_afterCommit_nonZeroDistributorQuery_publishMatchesRouteDerivedParams() {
		long itemId = 4402L;
		long companyId = 89L;
		String distributorIdParam = "12trailing";
		final long distributorIdQuery =
				StringUtils.hasText(distributorIdParam.trim())
						? LeadingNumberParser.parseAsLong(distributorIdParam.trim())
						: 0L;

		Items row = new Items();
		row.setItemId(itemId);
		row.setCompanyId(companyId);
		row.setNospec("true");
		row.setDistributorId(12);
		row.setItemBn("probe-bn-12");
		row.setItemType("services");
		row.setDefaultItemId(itemId);

		when(itemsRepository.getByItemIdAndCompany(itemId, companyId)).thenReturn(row);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
				inv -> {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization synchronization :
								TransactionSynchronizationManager.getSynchronizations()) {
							synchronization.afterCommit();
						}
						TransactionSynchronizationManager.clear();
					}
					return null;
				})
				.when(txMgr)
				.commit(any(TransactionStatus.class));

		TransactionTemplate tt = new TransactionTemplate(txMgr);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);

		tt.executeWithoutResult(st -> platformItemsDeleteService.deletePlatformItems(merged, itemId, distributorIdQuery));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(itemDeleteEventDispatchPublisher).publishAfterCommit(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertThat(((Number) payload.get("item_id")).longValue()).isEqualTo(itemId);
		assertThat(((Number) payload.get("company_id")).longValue()).isEqualTo(companyId);
		assertThat(payload.get("del_ids")).isEqualTo(List.of(itemId));
		@SuppressWarnings("unchecked")
		Map<String, Object> itemInfo = (Map<String, Object>) payload.get("item_info");
		assertThat(itemInfo).isNotNull();
		assertThat(itemInfo.get("item_bn")).isEqualTo("probe-bn-12");
	}
}
