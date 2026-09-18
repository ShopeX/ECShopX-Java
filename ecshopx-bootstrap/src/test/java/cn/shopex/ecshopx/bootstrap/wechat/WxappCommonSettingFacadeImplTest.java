package cn.shopex.ecshopx.bootstrap.wechat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.companys.service.setting.CategoryPageSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.NostoresSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.ShareParametersSettingRedisService;
import cn.shopex.ecshopx.companys.service.setting.WhitelistSettingRedisService;
import cn.shopex.ecshopx.espier.storage.StorageProperties;
import cn.shopex.ecshopx.im.service.EChatConfigService;
import cn.shopex.ecshopx.im.service.MeiqiaConfigService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappCommonSettingFacadeImplTest {

	private static final long COMPANY_ID = 1001L;

	@Mock
	private MeiqiaConfigService meiqiaConfigService;
	@Mock
	private EChatConfigService eChatConfigService;
	@Mock
	private NostoresSettingRedisService nostoresSettingRedisService;
	@Mock
	private WhitelistSettingRedisService whitelistSettingRedisService;
	@Mock
	private ShareParametersSettingRedisService shareParametersSettingRedisService;
	@Mock
	private PointMemberRuleReadService pointMemberRuleReadService;
	@Mock
	private CategoryPageSettingRedisService categoryPageSettingRedisService;
	@Mock
	private CompanyDefaultCurrencyService companyDefaultCurrencyService;
	@Mock
	private DmCrmSettingReadPort dmCrmSettingReadPort;

	private WxappCommonSettingFacadeImpl facade;

	@BeforeEach
	void setUp() {
		StorageProperties storageProperties = new StorageProperties();
		storageProperties.setDriver("local");
		facade =
				new WxappCommonSettingFacadeImpl(
						meiqiaConfigService,
						eChatConfigService,
						nostoresSettingRedisService,
						whitelistSettingRedisService,
						shareParametersSettingRedisService,
						pointMemberRuleReadService,
						categoryPageSettingRedisService,
						companyDefaultCurrencyService,
						dmCrmSettingReadPort,
						storageProperties,
						"");

		when(meiqiaConfigService.getInfo(COMPANY_ID)).thenReturn(Map.of());
		when(eChatConfigService.getInfo(COMPANY_ID)).thenReturn(Map.of());
		when(nostoresSettingRedisService.getNostoresStatus(COMPANY_ID))
				.thenReturn(Map.of("nostores_status", false));
		when(whitelistSettingRedisService.getMergedConfig(eq(COMPANY_ID), any()))
				.thenReturn(Map.of("whitelist_status", false));
		when(shareParametersSettingRedisService.read(COMPANY_ID)).thenReturn(null);
		when(pointMemberRuleReadService.getPointRule(COMPANY_ID, "CN"))
				.thenReturn(Map.of("name", "积分"));
		when(categoryPageSettingRedisService.getCategoryPageSetting(COMPANY_ID)).thenReturn(null);
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(false);
	}

	@Test
	@DisplayName("common.setting 响应含 currency 对象且位于末尾")
	void includesCurrencyObjectAtEnd() {
		CurrencyExchangeRate row = new CurrencyExchangeRate();
		row.setId(9L);
		row.setCompanyId(COMPANY_ID);
		row.setCurrency("CNY");
		row.setTitle("中国人民币");
		row.setSymbol("￥");
		row.setRate(1.0);
		row.setIsDefault(true);
		row.setUsePlatform("normal");

		LinkedHashMap<String, Object> currencyMap = new LinkedHashMap<>();
		currencyMap.put("id", 9L);
		currencyMap.put("company_id", COMPANY_ID);
		currencyMap.put("currency", "CNY");
		currencyMap.put("title", "中国人民币");
		currencyMap.put("symbol", "￥");
		currencyMap.put("rate", 1);
		currencyMap.put("is_default", true);
		currencyMap.put("use_platform", "normal");

		when(companyDefaultCurrencyService.getCur(COMPANY_ID)).thenReturn(row);
		when(companyDefaultCurrencyService.toCurResponseMap(row)).thenReturn(currencyMap);

		Map<String, Object> data = facade.getCommonSetting(COMPANY_ID, "CN");

		assertThat(data).containsKey("currency");
		@SuppressWarnings("unchecked")
		Map<String, Object> currency = (Map<String, Object>) data.get("currency");
		assertThat(currency.get("currency")).isEqualTo("CNY");
		assertThat(currency.get("symbol")).isEqualTo("￥");
		assertThat(currency.get("rate")).isEqualTo(1);
		assertThat(currency.get("is_default")).isEqualTo(true);
		assertThat(data.keySet().stream().reduce((a, b) -> b).orElse("")).isEqualTo("currency");
	}

	@Test
	@DisplayName("currency 来自 getCur 默认可触发懒建路径")
	void currencyUsesGetCur() {
		CurrencyExchangeRate lazyRow = new CurrencyExchangeRate();
		lazyRow.setId(1L);
		lazyRow.setCompanyId(COMPANY_ID);
		lazyRow.setCurrency("CNY");
		lazyRow.setSymbol("￥");
		lazyRow.setRate(1.0);
		lazyRow.setIsDefault(true);

		when(companyDefaultCurrencyService.getCur(anyLong())).thenReturn(lazyRow);
		when(companyDefaultCurrencyService.toCurResponseMap(lazyRow))
				.thenReturn(Map.of("currency", "CNY", "rate", 1));

		Map<String, Object> data = facade.getCommonSetting(COMPANY_ID, "CN");

		assertThat(data.get("currency")).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> currency = (Map<String, Object>) data.get("currency");
		assertThat(currency.get("currency")).isEqualTo("CNY");
	}
}
