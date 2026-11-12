package cn.shopex.ecshopx.promotions.service.membertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.discount.DiscountCardItemScopeGateway;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTaskRuleConfigDisplayServiceTest {

	@Mock
	private DiscountCardItemScopeGateway discountCardItemScopeGateway;

	@Mock
	private DistributorListQueryService distributorListQueryService;

	private MemberTaskRuleConfigDisplayService service;

	@BeforeEach
	void setUp() {
		service =
				new MemberTaskRuleConfigDisplayService(
						discountCardItemScopeGateway, distributorListQueryService);
	}

	@Test
	void enrich_addsCategoryAndDistributorListsInIdOrder() {
		when(discountCardItemScopeGateway.categoryNamesByIds(eq(1L), eq(List.of(100L, 200L))))
				.thenReturn(Map.of(100L, "鞋靴", 200L, "服装"));
		Distributor d = new Distributor();
		d.setDistributorId(10L);
		d.setName("旗舰店");
		when(distributorListQueryService.listByIdsAndCompany(1L, List.of(10L, 99L))).thenReturn(List.of(d));

		Map<String, Object> rule = new LinkedHashMap<>();
		rule.put("goods_scope", "category");
		rule.put("category_ids", List.of(100L, 200L));
		rule.put("distributor_ids", List.of(10L, 99L));

		Map<String, Object> out = service.enrichForAdminDetail(1L, rule);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> categories = (List<Map<String, Object>>) out.get("category_list");
		assertThat(categories).hasSize(2);
		assertThat(categories.get(0)).containsEntry("category_id", 100L).containsEntry("category_name", "鞋靴");
		assertThat(categories.get(1)).containsEntry("category_id", 200L).containsEntry("category_name", "服装");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> distributors = (List<Map<String, Object>>) out.get("distributor_list");
		assertThat(distributors).hasSize(2);
		assertThat(distributors.get(0)).containsEntry("distributor_id", 10L).containsEntry("name", "旗舰店");
		assertThat(distributors.get(1)).containsEntry("distributor_id", 99L).containsEntry("name", "");
	}

	@Test
	void enrich_emptyIds_yieldsEmptyLists() {
		Map<String, Object> out = service.enrichForAdminDetail(1L, Map.of("goods_scope", "unlimited"));

		assertThat(out.get("category_list")).isEqualTo(List.of());
		assertThat(out.get("distributor_list")).isEqualTo(List.of());
	}
}
