package cn.shopex.ecshopx.companys.service.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.mapper.CompanysNoticeStatisticsReadMapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
class CompanysNoticeStatisticsServiceTest {

	private static final long COMPANY_ID = 38L;

	@Mock
	private CompanysNoticeStatisticsReadMapper companysNoticeStatisticsReadMapper;

	@Mock
	private CompanysItemWarningStoreReader companysItemWarningStoreReader;

	@Mock
	private MessageSource messageSource;

	private CompanysNoticeStatisticsService service;

	@BeforeEach
	void setUp() {
		service =
				new CompanysNoticeStatisticsService(
						companysNoticeStatisticsReadMapper, companysItemWarningStoreReader, messageSource);
		when(companysNoticeStatisticsReadMapper.countWaitDeliveryOrders(COMPANY_ID)).thenReturn(0L);
		when(companysNoticeStatisticsReadMapper.countAftersalesPending(COMPANY_ID)).thenReturn(0L);
		when(companysNoticeStatisticsReadMapper.countRefundErrorLogsNotResubmit(COMPANY_ID)).thenReturn(0L);
		when(companysNoticeStatisticsReadMapper.countActiveSeckill(eq(COMPANY_ID), any(Integer.class)))
				.thenReturn(0L);
		when(companysNoticeStatisticsReadMapper.countActiveGroups(eq(COMPANY_ID), any(Integer.class)))
				.thenReturn(0L);
		when(companysItemWarningStoreReader.readPlatformWarningStore(COMPANY_ID)).thenReturn(10);
		when(companysNoticeStatisticsReadMapper.countLowStockItemsAll(COMPANY_ID, 10)).thenReturn(17L);
		when(companysNoticeStatisticsReadMapper.countLowStockItemsPlat(COMPANY_ID, 10)).thenReturn(12L);
	}

	@AfterEach
	void tearDown() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	@DisplayName("warning_goods_count 随 MessageSource 与当前 Locale 返回")
	void warningGoodsCountUsesMessageSourceForLocale() {
		LocaleContextHolder.setLocale(Locale.forLanguageTag("en-CN"));
		when(messageSource.getMessage(
						eq("companys.notice.warning_goods_count"),
						eq(new Object[] {12, 17}),
						eq("12 (自营) / 17 (全部)  "),
						eq(Locale.forLanguageTag("en-US"))))
				.thenReturn("12 (self-operated) / 17 (all)");

		LinkedHashMap<String, Object> data = service.buildAdminNoticeData(COMPANY_ID);

		assertThat(data.get("warning_goods_count")).isEqualTo("12 (self-operated) / 17 (all)");
		assertThat(data.get("warning_goods_count_plat")).isEqualTo(12);
	}
}
