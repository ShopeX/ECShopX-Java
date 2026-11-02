package cn.shopex.ecshopx.distribution.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.merchant.service.MerchantDisabledDistributorIdsQueryService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HandleValidCartValidShopIdsServiceTest {

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private MerchantDisabledDistributorIdsQueryService merchantDisabledDistributorIdsQueryService;

	@InjectMocks
	private HandleValidCartValidShopIdsService service;

	@Test
	@DisplayName("可买 is_valid：启用/禁用（含历史 1/0），不含闭店/撤店")
	void purchasableIsValid_coversEnabledAndDisabledOnly() {
		assertTrue(HandleValidCartValidShopIdsService.PURCHASABLE_IS_VALID.contains("true"));
		assertTrue(HandleValidCartValidShopIdsService.PURCHASABLE_IS_VALID.contains("false"));
		assertTrue(HandleValidCartValidShopIdsService.PURCHASABLE_IS_VALID.contains("1"));
		assertTrue(HandleValidCartValidShopIdsService.PURCHASABLE_IS_VALID.contains("0"));
		assertFalse(HandleValidCartValidShopIdsService.PURCHASABLE_IS_VALID.contains("closed"));
		assertFalse(HandleValidCartValidShopIdsService.PURCHASABLE_IS_VALID.contains("delete"));
	}

	@Test
	@DisplayName("禁用门店经查询命中后仍视为可买店铺")
	void resolve_keepsDisabledShopReturnedByQuery() {
		Distributor disabled = new Distributor();
		disabled.setDistributorId(306L);
		disabled.setIsValid("false");
		when(distributorMapper.selectList(any())).thenReturn(List.of(disabled));
		when(merchantDisabledDistributorIdsQueryService.listDistributorIdsLinkedToDisabledMerchants(38L))
				.thenReturn(List.of());

		assertEquals(List.of(306L), service.resolveValidShopIdsAfterDistributorAndMerchantRules(38L, List.of(306L)));
	}

	@Test
	@DisplayName("关联已禁用商户的店铺仍剔除")
	void resolve_excludesMerchantDisabledShops() {
		Distributor enabled = new Distributor();
		enabled.setDistributorId(306L);
		enabled.setIsValid("true");
		when(distributorMapper.selectList(any())).thenReturn(List.of(enabled));
		when(merchantDisabledDistributorIdsQueryService.listDistributorIdsLinkedToDisabledMerchants(38L))
				.thenReturn(List.of(306L));

		assertEquals(List.of(), service.resolveValidShopIdsAfterDistributorAndMerchantRules(38L, List.of(306L)));
	}

	@Test
	@DisplayName("空店铺 id 直接返回空")
	void resolve_emptyInput() {
		assertEquals(List.of(), service.resolveValidShopIdsAfterDistributorAndMerchantRules(38L, List.of()));
		assertEquals(List.of(), service.resolveValidShopIdsAfterDistributorAndMerchantRules(38L, null));
	}
}
