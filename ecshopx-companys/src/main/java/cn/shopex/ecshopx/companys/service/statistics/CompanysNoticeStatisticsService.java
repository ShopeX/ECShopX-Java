/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.companys.service.statistics;

import cn.shopex.ecshopx.common.web.locale.RequestMessageLocale;
import cn.shopex.ecshopx.companys.mapper.CompanysNoticeStatisticsReadMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CompanysNoticeStatisticsService {

	private static final String WARNING_GOODS_COUNT_KEY = "companys.notice.warning_goods_count";

	private final CompanysNoticeStatisticsReadMapper companysNoticeStatisticsReadMapper;
	private final CompanysItemWarningStoreReader companysItemWarningStoreReader;
	private final MessageSource messageSource;

	public CompanysNoticeStatisticsService(
			CompanysNoticeStatisticsReadMapper companysNoticeStatisticsReadMapper,
			CompanysItemWarningStoreReader companysItemWarningStoreReader,
			MessageSource messageSource) {
		this.companysNoticeStatisticsReadMapper = companysNoticeStatisticsReadMapper;
		this.companysItemWarningStoreReader = companysItemWarningStoreReader;
		this.messageSource = messageSource;
	}

	public LinkedHashMap<String, Object> getOrderStatusCount(long companyId) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		fillAdminNoticeCounts(result, companyId);
		return result;
	}

	public LinkedHashMap<String, Object> buildAdminNoticeData(long companyId) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		fillAdminNoticeCounts(m, companyId);
		return m;
	}

	public LinkedHashMap<String, Object> buildMerchantNoticeData(long companyId, long merchantId) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put(
				"wait_delivery_count",
				clampToInt(companysNoticeStatisticsReadMapper.countWaitDeliveryOrdersForMerchant(companyId, merchantId)));
		m.put(
				"aftersales_count",
				clampToInt(companysNoticeStatisticsReadMapper.countAftersalesPendingForMerchant(companyId, merchantId)));
		m.put(
				"refund_errorlogs_count",
				clampToInt(
						companysNoticeStatisticsReadMapper.countRefundErrorLogsNotResubmitForMerchant(
								companyId, merchantId)));
		return m;
	}

	private void fillAdminNoticeCounts(LinkedHashMap<String, Object> out, long companyId) {
		long waitLong = companysNoticeStatisticsReadMapper.countWaitDeliveryOrders(companyId);
		int waitDeliveryCount = clampToInt(waitLong);

		int warningStore = companysItemWarningStoreReader.readPlatformWarningStore(companyId);
		long allLong = companysNoticeStatisticsReadMapper.countLowStockItemsAll(companyId, warningStore);
		long platLong = companysNoticeStatisticsReadMapper.countLowStockItemsPlat(companyId, warningStore);
		int all = clampToInt(allLong);
		int plat = clampToInt(platLong);

		int now = (int) Instant.now().getEpochSecond();
		int startedSeckillCount = clampToInt(companysNoticeStatisticsReadMapper.countActiveSeckill(companyId, now));
		int startedGtoupsCount = clampToInt(companysNoticeStatisticsReadMapper.countActiveGroups(companyId, now));

		long afterLong = companysNoticeStatisticsReadMapper.countAftersalesPending(companyId);
		int aftersalesCount = clampToInt(afterLong);

		long refundLong = companysNoticeStatisticsReadMapper.countRefundErrorLogsNotResubmit(companyId);
		int refundErrorlogsCount = clampToInt(refundLong);

		Locale locale = messageLocaleFromContext();
		String warningGoodsCount =
				messageSource.getMessage(
						WARNING_GOODS_COUNT_KEY,
						new Object[] {plat, all},
						defaultWarningGoodsCount(plat, all),
						locale);

		out.put("wait_delivery_count", waitDeliveryCount);
		out.put("warning_goods_count", warningGoodsCount);
		out.put("warning_goods_count_plat", plat);
		out.put("started_seckill_count", startedSeckillCount);
		out.put("started_gtoups_count", startedGtoupsCount);
		out.put("aftersales_count", aftersalesCount);
		out.put("refund_errorlogs_count", refundErrorlogsCount);
	}

	private static Locale messageLocaleFromContext() {
		Locale holder = LocaleContextHolder.getLocale();
		if (holder == null) {
			return Locale.SIMPLIFIED_CHINESE;
		}
		return RequestMessageLocale.toMessageLocale(holder.toLanguageTag());
	}

	private static String defaultWarningGoodsCount(int plat, int all) {
		return plat + " (自营) / " + all + " (全部)  ";
	}

	private static int clampToInt(long v) {
		if (v > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) v;
	}
}
