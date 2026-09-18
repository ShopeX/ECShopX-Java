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

import cn.shopex.ecshopx.common.cron.SalespersonGiveCouponsCountPort;
import cn.shopex.ecshopx.common.cron.SalespersonGiveCouponsRecordStatisticsRunner;
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
 * 导购送券：kaquan 表行数 COUNT + {@code companys_salesperson_statistics} 幂等写入（salespersonGiveCoupons/member），段二不读 Redis 汇总键。
 */
@Component
@RequiredArgsConstructor
public class SalespersonGiveCouponsRecordStatisticsRunnerImpl implements SalespersonGiveCouponsRecordStatisticsRunner {

	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SalespersonStatisticsMapper salespersonStatisticsMapper;
	private final SalespersonStatisticsCronLogPort cronLog;
	private final SalespersonGiveCouponsCountPort giveCouponsCountPort;

	@Override
	public int runGiveCouponsBlock(int yesterdayYmd) {
		List<ShopSalesperson> list = shopSalespersonMapper.selectList(
				new QueryWrapper<ShopSalesperson>()
						.eq("salesperson_type", "shopping_guide")
						.select("company_id", "salesperson_id"));
		if (list.isEmpty()) {
			// 与 PHP 一致：无 shopping_guide 行时 foreach 不进入，不输出导购赠券块级 / 行级 debug
			return 0;
		}
		cronLog.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS, 0, 0);
		int rowCount = list.size();
		int inserts = 0;
		for (ShopSalesperson row : list) {
			long companyId = row.getCompanyId();
			long salespersonId = row.getSalespersonId();
			cronLog.debugStart(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS, companyId, salespersonId);
			try {
				inserts += recordSalespersonGiveCouponsStatistics(companyId, salespersonId, yesterdayYmd);
			} catch (Exception e) {
				cronLog.debugError(
						SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS, companyId, salespersonId, e);
			}
			cronLog.debugEnd(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS, companyId, salespersonId);
		}
		cronLog.debugEnd(SalespersonStatisticsCronLogKind.SHOPPING_GUIDE_GIVE_COUPONS, 0, 0);
		return rowCount + inserts;
	}

	int recordSalespersonGiveCouponsStatistics(long companyId, long salespersonId, Integer date) {
		int dateYmd = date == null ? yesterdayYmd() : date;
		int sendCouponsNum = giveCouponsCountPort.countSuccessRowsForDate(companyId, salespersonId, dateYmd);
		return insertSalespersonIfAbsent(companyId, dateYmd, "salespersonGiveCoupons", "member", sendCouponsNum, salespersonId);
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

	private static int yesterdayYmd() {
		return Integer.parseInt(ZonedDateTime.now(APP_ZONE).toLocalDate().minusDays(1).format(YMD));
	}
}
