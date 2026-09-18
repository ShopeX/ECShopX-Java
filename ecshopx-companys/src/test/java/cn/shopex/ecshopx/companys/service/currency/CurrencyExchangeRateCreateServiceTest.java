package cn.shopex.ecshopx.companys.service.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.mapper.CurrencyExchangeRateMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrencyExchangeRateCreateServiceTest {

	private static final long COMPANY_ID = 1001L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), CurrencyExchangeRate.class);
	}

	@Mock
	private CurrencyExchangeRateMapper currencyExchangeRateMapper;

	private CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private CurrencyExchangeRateCreateService service;

	@BeforeEach
	void setUp() {
		companyDefaultCurrencyService = new CompanyDefaultCurrencyService(currencyExchangeRateMapper);
		service = new CurrencyExchangeRateCreateService(currencyExchangeRateMapper, companyDefaultCurrencyService);
	}

	@Test
	@DisplayName("合法 HKD 创建成功")
	void createAllowedCurrency() {
		when(currencyExchangeRateMapper.selectOne(any())).thenReturn(null);
		when(currencyExchangeRateMapper.insert(any(CurrencyExchangeRate.class)))
				.thenAnswer(inv -> {
					CurrencyExchangeRate e = inv.getArgument(0);
					e.setId(42L);
					return 1;
				});

		Map<String, Object> out =
				service.create(
						Map.of(
								"currency", "HKD",
								"symbol", "HK$",
								"rate", 0.92,
								"title", "港币"),
						COMPANY_ID);

		assertThat(out.get("currency")).isEqualTo("HKD");
		assertThat(out.get("symbol")).isEqualTo("HK$");
		assertThat(out.get("company_id")).isEqualTo(COMPANY_ID);

		ArgumentCaptor<CurrencyExchangeRate> captor = ArgumentCaptor.forClass(CurrencyExchangeRate.class);
		verify(currencyExchangeRateMapper).insert(captor.capture());
		assertThat(captor.getValue().getCurrency()).isEqualTo("HKD");
	}

	@Test
	@DisplayName("非法币种码拒绝")
	void rejectUnsupportedCurrency() {
		assertThatThrownBy(
						() ->
								service.create(
										Map.of("currency", "RMB", "symbol", "￥", "rate", 1),
										COMPANY_ID))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("不支持的货币类型");

		verify(currencyExchangeRateMapper, never()).insert(any(CurrencyExchangeRate.class));
	}

	@Test
	@DisplayName("同公司重复 currency 拒绝")
	void rejectDuplicateCurrency() {
		CurrencyExchangeRate existing = new CurrencyExchangeRate();
		existing.setId(1L);
		existing.setCompanyId(COMPANY_ID);
		existing.setCurrency("USD");
		when(currencyExchangeRateMapper.selectOne(any())).thenReturn(existing);

		assertThatThrownBy(
						() ->
								service.create(
										Map.of("currency", "USD", "symbol", "$", "rate", 7),
										COMPANY_ID))
				.isInstanceOf(ResourceException.class)
				.hasMessage("该币种已存在");

		verify(currencyExchangeRateMapper, never()).insert(any(CurrencyExchangeRate.class));
	}

	@Test
	@DisplayName("六码均属允许集")
	void allAllowedCodesPassWhitelist() {
		when(currencyExchangeRateMapper.selectOne(any())).thenReturn(null);
		when(currencyExchangeRateMapper.insert(any(CurrencyExchangeRate.class)))
				.thenAnswer(inv -> {
					CurrencyExchangeRate e = inv.getArgument(0);
					e.setId(1L);
					return 1;
				});

		for (String code : CurrencyCreateValidator.ALLOWED_CURRENCY_CODES) {
			Map<String, Object> out =
					service.create(Map.of("currency", code, "symbol", "X", "rate", 1), COMPANY_ID);
			assertThat(out.get("currency")).isEqualTo(code);
		}
	}
}
