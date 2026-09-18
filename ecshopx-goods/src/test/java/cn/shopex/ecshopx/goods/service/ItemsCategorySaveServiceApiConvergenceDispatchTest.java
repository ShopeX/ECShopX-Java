package cn.shopex.ecshopx.goods.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import cn.shopex.ecshopx.common.dispatch.GoodsItemCategoryAddDispatchPublisher;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformCategorySyncPort;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.dto.CategoryTreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Locks the single convergence point for admin and legacy service-style category creation:
 * {@link ItemsCategorySaveService#saveItemsCategory} registers dispatch only after transaction commit.
 */
class ItemsCategorySaveServiceApiConvergenceDispatchTest {

	private EmbeddedDatabase embeddedDatabase;
	private TransactionTemplate transactionTemplate;
	private GoodsItemCategoryAddDispatchPublisher dispatchPublisher;
	private ItemsCategoryRepository itemsCategoryRepository;
	private ItemsCategoryDistributorIdResolver distributorIdResolver;
	private ItemsCategorySaveService itemsCategorySaveService;

	@BeforeEach
	void setUp() {
		embeddedDatabase =
				new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
		DataSourceTransactionManager txManager = new DataSourceTransactionManager(embeddedDatabase);
		transactionTemplate = new TransactionTemplate(txManager);

		dispatchPublisher = Mockito.mock(GoodsItemCategoryAddDispatchPublisher.class);
		itemsCategoryRepository = Mockito.mock(ItemsCategoryRepository.class);
		distributorIdResolver = Mockito.mock(ItemsCategoryDistributorIdResolver.class);

		doAnswer(invocation -> {
					ItemsCategory entity = invocation.getArgument(0);
					entity.setCategoryId(42_001L);
					return null;
				})
				.when(itemsCategoryRepository)
				.insert(any(ItemsCategory.class));
		Mockito.when(itemsCategoryRepository.getByCategoryCode(anyString(), anyLong()))
				.thenReturn(Optional.empty());
		Mockito.doNothing().when(itemsCategoryRepository).updatePath(anyLong(), anyLong(), anyString());

		Mockito.when(distributorIdResolver.resolveForCategoryNode(any(CategoryTreeNode.class), anyLong(), anyLong()))
				.thenAnswer(invocation -> {
					CategoryTreeNode node = invocation.getArgument(0);
					if (Boolean.TRUE.equals(node.getIsMainCategory())) {
						return 0L;
					}
					return node.getPendingDistributorIdBeforeResolve();
				});

		itemsCategorySaveService =
				new ItemsCategorySaveService(
						dispatchPublisher,
						itemsCategoryRepository,
						distributorIdResolver,
						new ObjectMapper(),
						Mockito.mock(ShuyunOpenPlatformCategorySyncPort.class));
	}

	@AfterEach
	void tearDown() {
		if (embeddedDatabase != null) {
			embeddedDatabase.shutdown();
		}
	}

	@Test
	void saveItemsCategory_afterCommit_publishesItemCategoryAdd_forServiceApiEntryParity() {
		long companyId = 9_001L;
		CategoryTreeNode root = new CategoryTreeNode();
		root.setCategoryName("聚合测试类目");
		root.setIsMainCategory(true);

		assertDoesNotThrow(() -> transactionTemplate.executeWithoutResult(status -> {
			itemsCategorySaveService.saveItemsCategory(companyId, 0L, List.of(root));
			verify(dispatchPublisher, never()).publish(anyLong());
		}));

		verify(dispatchPublisher).publish(companyId);
		verifyNoMoreInteractions(dispatchPublisher);
	}
}
