package cn.shopex.ecshopx.distribution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.integration.MapGeocodeClient;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidAppendFieldsService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidCoreQueryService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidNearShopService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidNostoresBranchService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidSettingService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.DistributorIsValidWhiteListBranchService;
import cn.shopex.ecshopx.distribution.service.distributorvalid.dto.DistributorIsValidQuery;
import cn.shopex.ecshopx.members.service.address.MemberAddressDefaultReadService;
import cn.shopex.ecshopx.members.service.address.MemberAddressLatLngNonNumericEnqueueService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributorH5GetDistributorIsValidServicePreferVirtualStoreTest {

	private static final long COMPANY_ID = 38L;
	private static final long USER_ID = 100L;

	@Mock
	private DistributorIsValidSettingService distributorIsValidSettingService;

	@Mock
	private DistributorIsValidWhiteListBranchService distributorIsValidWhiteListBranchService;

	@Mock
	private MemberAddressDefaultReadService memberAddressDefaultReadService;

	@Mock
	private MemberAddressLatLngNonNumericEnqueueService memberAddressLatLngNonNumericEnqueueService;

	@Mock
	private MapGeocodeClient mapGeocodeClient;

	@Mock
	private DistributorIsValidCoreQueryService distributorIsValidCoreQueryService;

	@Mock
	private DistributorIsValidNostoresBranchService distributorIsValidNostoresBranchService;

	@Mock
	private DistributorIsValidNearShopService distributorIsValidNearShopService;

	@Mock
	private DistributorIsValidAppendFieldsService distributorIsValidAppendFieldsService;

	@Mock
	private DistributionStoreEntryRuleRedisService distributionStoreEntryRuleRedisService;

	private DistributorH5GetDistributorIsValidService service;

	@BeforeEach
	void setUp() {
		lenient()
				.when(distributorIsValidSettingService.getOpenDividedSetting(anyLong()))
				.thenReturn(new LinkedHashMap<>());
		service =
				new DistributorH5GetDistributorIsValidService(
						distributorIsValidSettingService,
						distributorIsValidWhiteListBranchService,
						memberAddressDefaultReadService,
						memberAddressLatLngNonNumericEnqueueService,
						mapGeocodeClient,
						distributorIsValidCoreQueryService,
						distributorIsValidNostoresBranchService,
						distributorIsValidNearShopService,
						distributorIsValidAppendFieldsService,
						distributionStoreEntryRuleRedisService);
	}

	@Test
	void radioType1_withoutInputs_returnsVirtualStore_andSkipsAddressFill() {
		Map<String, Object> inRule = new LinkedHashMap<>();
		inRule.put("radio_type", Integer.valueOf(1));
		when(distributionStoreEntryRuleRedisService.getInRule(COMPANY_ID)).thenReturn(inRule);

		Map<String, Object> selfRow = new LinkedHashMap<>();
		selfRow.put("distributor_id", Long.valueOf(0L));
		selfRow.put("name", "线上总店");
		when(distributorIsValidCoreQueryService.getDistributorSelf(eq(COMPANY_ID), eq(true), any()))
				.thenReturn(selfRow);

		DistributorIsValidQuery query =
				DistributorIsValidQuery.of(
						null, null, null, null, null, null, null, Integer.valueOf(0), null, null, null, null, null, null);

		Map<String, Object> result =
				service.getDistributorIsValid(COMPANY_ID, USER_ID, COMPANY_ID, COMPANY_ID, query);

		assertThat(result.get("is_valid")).isEqualTo("true");
		assertThat(result.get("name")).isEqualTo("线上总店");
		verify(memberAddressDefaultReadService, never()).getDefaultAddress(anyLong(), anyLong());
		verify(distributorIsValidCoreQueryService, never())
				.getInfoSimple(anyLong(), any(Integer.class), anyString());
	}

	@Test
	void radioType2_withoutInputs_fallsBackToDefaultStore() {
		Map<String, Object> inRule = new LinkedHashMap<>();
		inRule.put("radio_type", Integer.valueOf(2));
		when(distributionStoreEntryRuleRedisService.getInRule(COMPANY_ID)).thenReturn(inRule);

		Map<String, Object> defaultRow = new LinkedHashMap<>();
		defaultRow.put("distributor_id", Long.valueOf(293L));
		defaultRow.put("is_valid", "true");
		when(distributorIsValidCoreQueryService.getInfoSimple(eq(COMPANY_ID), eq(1), eq("true")))
				.thenReturn(defaultRow);

		DistributorIsValidQuery query =
				DistributorIsValidQuery.of(
						null, null, null, null, null, null, null, Integer.valueOf(0), null, null, null, null, null, null);

		Map<String, Object> result =
				service.getDistributorIsValid(COMPANY_ID, USER_ID, COMPANY_ID, COMPANY_ID, query);

		assertThat(result.get("distributor_id")).isEqualTo(Long.valueOf(293L));
		verify(distributorIsValidCoreQueryService, never())
				.getDistributorSelf(anyLong(), anyBoolean(), any());
		verify(distributorIsValidCoreQueryService).getInfoSimple(eq(COMPANY_ID), eq(1), eq("true"));
	}
}
