package cn.shopex.ecshopx.goods.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.dispatch.ItemTagEditEventDispatchPublisher;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.promotions.service.ItemsTagActivityCheckService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsTagsRelServiceRelateTagsItemTagEditPublishTest {

	@Mock
	private ItemsQueryRepository itemsQueryRepository;

	@Mock
	private ItemsRelTagsRepository itemsRelTagsRepository;

	@Mock
	private ItemsTagActivityCheckService itemsTagActivityCheckService;

	@Mock
	private ItemTagEditEventDispatchPublisher itemTagEditEventDispatchPublisher;

	private ItemsTagsRelService service;

	@BeforeEach
	void setUp() {
		service = new ItemsTagsRelService(
				itemsQueryRepository,
				itemsRelTagsRepository,
				itemsTagActivityCheckService,
				itemTagEditEventDispatchPublisher);
	}

	@Test
	void relateTags_afterSuccessfulRel_publishesItemTagEditOnceWithCompanyIdInEntities() {
		long companyId = 42L;
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("item_ids", List.of(1L, 2L));
		merged.put("tag_ids", List.of());

		when(itemsRelTagsRepository.getLists(eq(companyId), anyList())).thenReturn(List.of());

		service.relateTags(companyId, merged);

		verify(itemTagEditEventDispatchPublisher, times(1))
				.publish(
						ArgumentMatchers.argThat(
								m ->
										m != null
												&& m.containsKey("company_id")
												&& companyId
														== ((Number) m.get("company_id")).longValue()));
	}

	@Test
	void relateTags_whenActivityConflict_doesNotPublish() {
		long companyId = 7L;
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("item_ids", List.of(100L));
		merged.put("tag_ids", List.of(200L));

		Items row = new Items();
		row.setItemId(100L);
		row.setItemCategory("10");
		row.setBrandId(1);
		when(itemsQueryRepository.listByCompanyIdAndItemIds(eq(companyId), eq(List.of(100L))))
				.thenReturn(List.of(row));
		when(itemsTagActivityCheckService.checkActivity(
						any(),
						eq(List.of(200L)),
						eq(companyId),
						ArgumentMatchers.<AtomicReference<String>>any()))
				.thenReturn(false);

		assertThrows(ResourceException.class, () -> service.relateTags(companyId, merged));

		verify(itemTagEditEventDispatchPublisher, never()).publish(any());
	}
}
