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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.cron.CompanysRecordStatisticsRedisPort;
import cn.shopex.ecshopx.common.cron.SalespersonPopularizeRecordStatisticsRunner;
import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogKind;
import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogPort;
import cn.shopex.ecshopx.salesperson.domain.SalespersonStatistics;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonStatisticsMapper;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 导购推广：Redis 推广分润键 + {@code companys_salesperson_statistics} 幂等写入（salesCommission/member）。
 */
@Component
@RequiredArgsConstructor
public class SalespersonPopularizeRecordStatisticsRunnerImpl implements SalespersonPopularizeRecordStatisticsRunner {

	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	private final CompanysRecordStatisticsRedisPort recordStatsRedis;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SalespersonStatisticsMapper salespersonStatisticsMapper;
	private final SalespersonStatisticsCronLogPort cronLog;

	@Override
	public int runPopularizeBlock(int yesterdayYmd) {
		List<ShopSalesperson> list = shopSalespersonMapper.selectList(
				new QueryWrapper<ShopSalesperson>()
						.eq("salesperson_type", "shopping_guide")
						.select("company_id", "salesperson_id"));
		cronLog.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE, 0, 0);
		int rowCount = list.size();
		int inserts = 0;
		for (ShopSalesperson row : list) {
			long companyId = row.getCompanyId();
			long salespersonId = row.getSalespersonId();
			cronLog.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE, companyId, salespersonId);
			try {
				inserts += recordSalespersonPopularizeStatistics(companyId, salespersonId, yesterdayYmd);
			} catch (Exception e) {
				cronLog.debugError(
						SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE, companyId, salespersonId, e);
			}
		}
		cronLog.debugEnd(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_POPULARIZE, 0, 0);
		return rowCount + inserts;
	}

	int recordSalespersonPopularizeStatistics(long companyId, long salespersonId, Integer date) {
		int dateYmd = date == null ? yesterdayYmd() : date;
		String dateStr = Integer.toString(dateYmd);
		String popularizeKey = salespersonPopularizeKey(companyId, salespersonId, dateStr);
		String raw = recordStatsRedis.get(popularizeKey);
		int salesCommission = parseIntOrZero(raw);
		return insertSalespersonIfAbsent(companyId, dateYmd, "salesCommission", "member", salesCommission, salespersonId);
	}

	private static String salespersonPopularizeKey(long companyId, long salespersonId, String date) {
		return "Member:Salesperson:Popularize:" + salespersonId + ":Company:" + companyId + ":" + date;
	}

	private int insertSalespersonIfAbsent(
			long companyId, int addDate, String title, String type, int value, long salespersonId) {
		SalespersonStatistics found = salespersonStatisticsMapper.selectOne(
				new LambdaQueryWrapper<SalespersonStatistics>()
						.eq(SalespersonStatistics::getCompanyId, companyId)
						.eq(SalespersonStatistics::getAddDate, addDate)
						.eq(SalespersonStatistics::getStatisticTitle, title)
						.eq(SalespersonStatistics::getStatisticType, type)
						.eq(SalespersonStatistics::getSalespersonId, salespersonId));
		if (found != null) {
			return 0;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		SalespersonStatistics row = new SalespersonStatistics();
		row.setCompanyId(companyId);
		row.setAddDate(addDate);
		row.setStatisticTitle(title);
		row.setStatisticType(type);
		row.setDataValue(value);
		row.setSalespersonId(salespersonId);
		row.setCreated(now);
		row.setUpdated(now);
		salespersonStatisticsMapper.insert(row);
		return 1;
	}

	private static int parseIntOrZero(String s) {
		if (s == null || s.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int yesterdayYmd() {
		return Integer.parseInt(ZonedDateTime.now(APP_ZONE).toLocalDate().minusDays(1).format(YMD));
	}
}
