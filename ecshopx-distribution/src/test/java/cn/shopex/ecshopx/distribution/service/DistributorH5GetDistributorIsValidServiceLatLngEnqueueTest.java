package cn.shopex.ecshopx.distribution.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
class DistributorH5GetDistributorIsValidServiceLatLngEnqueueTest {

	private static final long COMPANY_ID = 1L;
	private static final long USER_ID = 42L;
	private static final long FILTER_COMPANY_ID = 10L;
	private static final long AUTH_COMPANY_ID = 20L;

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
		Map<String, Object> selfRow = new LinkedHashMap<>();
		selfRow.put("is_valid", "true");
		selfRow.put("is_delivery", Boolean.TRUE);
		selfRow.put("is_ziti", Boolean.FALSE);
		lenient()
				.when(distributorIsValidCoreQueryService.getDistributorSelf(anyLong(), anyBoolean(), any()))
				.thenReturn(selfRow);
		lenient()
				.when(distributorIsValidCoreQueryService.getInfoSimple(anyLong(), any(Integer.class), anyString()))
				.thenReturn(null);
		Map<String, Object> defaultInRule = new LinkedHashMap<>();
		defaultInRule.put("radio_type", Integer.valueOf(2));
		lenient()
				.when(distributionStoreEntryRuleRedisService.getInRule(anyLong()))
				.thenReturn(defaultInRule);
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

	private static DistributorIsValidQuery baselineQuery() {
		return DistributorIsValidQuery.of(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				Integer.valueOf(0),
				null,
				null,
				null,
				null,
				null,
				null);
	}

	@Test
	void enqueueIfNeeded_calledWhenDefaultAddressHasNonNumericLatLng() {
		Map<String, Object> addrMap = new LinkedHashMap<>();
		addrMap.put("address_id", Long.valueOf(99L));
		addrMap.put("lat", "-");
		addrMap.put("lng", "");
		when(memberAddressDefaultReadService.getDefaultAddress(FILTER_COMPANY_ID, AUTH_COMPANY_ID))
				.thenReturn(addrMap);

		service.getDistributorIsValid(
				COMPANY_ID, USER_ID, FILTER_COMPANY_ID, AUTH_COMPANY_ID, baselineQuery());

		verify(memberAddressLatLngNonNumericEnqueueService, times(1))
				.enqueueIfNeeded(eq(FILTER_COMPANY_ID), eq(AUTH_COMPANY_ID), same(addrMap));
		verify(mapGeocodeClient, never()).getLatAndLng(anyLong(), anyString(), anyString());
	}

	@Test
	void enqueueIfNeeded_calledWhenDefaultAddressPresent_latLngNumericStillInvokesService() {
		Map<String, Object> addrMap = new LinkedHashMap<>();
		addrMap.put("address_id", Long.valueOf(1L));
		addrMap.put("lat", "31.23");
		addrMap.put("lng", "121.47");
		when(memberAddressDefaultReadService.getDefaultAddress(FILTER_COMPANY_ID, AUTH_COMPANY_ID))
				.thenReturn(addrMap);

		service.getDistributorIsValid(
				COMPANY_ID, USER_ID, FILTER_COMPANY_ID, AUTH_COMPANY_ID, baselineQuery());

		verify(memberAddressLatLngNonNumericEnqueueService, times(1))
				.enqueueIfNeeded(eq(FILTER_COMPANY_ID), eq(AUTH_COMPANY_ID), same(addrMap));
	}

	@Test
	void enqueueIfNeeded_notCalledWhenNoDefaultAddress() {
		when(memberAddressDefaultReadService.getDefaultAddress(FILTER_COMPANY_ID, AUTH_COMPANY_ID))
				.thenReturn(null);

		service.getDistributorIsValid(
				COMPANY_ID, USER_ID, FILTER_COMPANY_ID, AUTH_COMPANY_ID, baselineQuery());

		verify(memberAddressLatLngNonNumericEnqueueService, never())
				.enqueueIfNeeded(anyLong(), anyLong(), any());
	}

	@Test
	void enqueueIfNeeded_notCalledWhenUserIdZero() {
		service.getDistributorIsValid(
				COMPANY_ID, 0L, FILTER_COMPANY_ID, AUTH_COMPANY_ID, baselineQuery());

		verify(memberAddressDefaultReadService, never()).getDefaultAddress(anyLong(), anyLong());
		verify(memberAddressLatLngNonNumericEnqueueService, never())
				.enqueueIfNeeded(anyLong(), anyLong(), any());
	}
}
