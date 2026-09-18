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

import cn.shopex.ecshopx.companys.service.OperatorRoleMenuAliasService;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CompanysDashboardStatisticsService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD_BASIC = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private final OrderPayStatisticsRedisReader orderReader;
	private final CompanysDepositDayRechargeRedisReader depositReader;
	private final OperatorRoleMenuAliasService operatorRoleMenuAliasService;
	private final ShopMenuService shopMenuService;
	private final CompanysNoticeStatisticsService companysNoticeStatisticsService;
	private final CompanysMemberStatisticsService memberStatisticsService;

	public CompanysDashboardStatisticsService(
			OrderPayStatisticsRedisReader orderReader,
			CompanysDepositDayRechargeRedisReader depositReader,
			OperatorRoleMenuAliasService operatorRoleMenuAliasService,
			ShopMenuService shopMenuService,
			CompanysNoticeStatisticsService companysNoticeStatisticsService,
			CompanysMemberStatisticsService memberStatisticsService) {
		this.orderReader = orderReader;
		this.depositReader = depositReader;
		this.operatorRoleMenuAliasService = operatorRoleMenuAliasService;
		this.shopMenuService = shopMenuService;
		this.companysNoticeStatisticsService = companysNoticeStatisticsService;
		this.memberStatisticsService = memberStatisticsService;
	}

	public LinkedHashMap<String, Object> getDataList(
			long companyId,
			boolean isApp,
			String shopIdRaw,
			String operatorType,
			long merchantId,
			long operatorId) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		if (isApp) {
			String todayYmd = LocalDate.now(SHANGHAI).format(YMD_BASIC);
			LinkedHashMap<String, Object> todayData =
					longCountsToObjectMap(orderReader.getStatistics(companyId, todayYmd, shopIdRaw));
			todayData.put(
					"real_deposit",
					depositReader.getRechargeTotal(companyId, ymdBasicIso(todayYmd)));
			result.put("today_data", todayData);

			List<String> aliases = operatorRoleMenuAliasService.listShopMenuAliases(companyId, operatorId);
			int menuVersion = "distributor".equals(operatorType) ? 3 : 1;
			List<String> apisList =
					shopMenuService.collectApisForStatisticsApp(menuVersion, aliases == null ? List.of() : aliases);

			LinkedHashMap<String, Object> apis = new LinkedHashMap<>();
			apis.put("order", 0);
			apis.put("aftersales", 0);
			apis.put("order_ziti", 0);
			apis.put("items", 0);
			apis.put("users", 0);
			if (apisList.contains("order.list.get")) {
				apis.put("order", 1);
				apis.put("order_ziti", 1);
			}
			if (apisList.contains("aftersales.list")) {
				apis.put("aftersales", 1);
			}
			if (apisList.contains("goods.items.lists")) {
				apis.put("items", 1);
			}
			if (apisList.contains("member.list")) {
				apis.put("users", 1);
			}
			result.put("apis", apis);
			return result;
		}
		if ("merchant".equals(operatorType)) {
			LocalDate today = LocalDate.now(SHANGHAI);
			String todayYmd = today.format(YMD_BASIC);
			String yesterdayYmd = today.minusDays(1).format(YMD_BASIC);
			result.put(
					"today_data",
					longCountsToObjectMap(orderReader.getMerchantStatistics(companyId, merchantId, todayYmd)));
			result.put(
					"yesterday_data",
					longCountsToObjectMap(orderReader.getMerchantStatistics(companyId, merchantId, yesterdayYmd)));
			result.put(
					"notice_data",
					companysNoticeStatisticsService.buildMerchantNoticeData(companyId, merchantId));
			return result;
		}
		LocalDate today = LocalDate.now(SHANGHAI);
		String todayYmd = today.format(YMD_BASIC);
		String yesterdayYmd = today.minusDays(1).format(YMD_BASIC);
		LinkedHashMap<String, Object> todayData =
				longCountsToObjectMap(orderReader.getStatistics(companyId, todayYmd, null));
		todayData.put("real_deposit", depositReader.getRechargeTotal(companyId, ymdBasicIso(todayYmd)));
		result.put("today_data", todayData);

		LinkedHashMap<String, Object> yesterdayData =
				longCountsToObjectMap(orderReader.getStatistics(companyId, yesterdayYmd, null));
		yesterdayData.put("real_deposit", depositReader.getRechargeTotal(companyId, ymdBasicIso(yesterdayYmd)));
		result.put("yesterday_data", yesterdayData);

		result.put("notice_data", companysNoticeStatisticsService.buildAdminNoticeData(companyId));
		result.put("member_data", memberStatisticsService.getMemberStatistics(companyId, todayYmd));
		return result;
	}

	private static String ymdBasicIso(String todayYmd) {
		return LocalDate.parse(todayYmd, YMD_BASIC).format(ISO_DATE);
	}

	private static LinkedHashMap<String, Object> longCountsToObjectMap(LinkedHashMap<String, Long> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Long> e : src.entrySet()) {
			out.put(e.getKey(), e.getValue());
		}
		return out;
	}
}
