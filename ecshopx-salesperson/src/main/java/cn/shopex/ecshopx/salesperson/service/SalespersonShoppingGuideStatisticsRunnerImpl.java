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
import cn.shopex.ecshopx.common.cron.SalespersonShoppingGuideStatisticsRunner;
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
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 导购日统计；单线程顺序与 shopping_guide 查询结果一致。
 */
@Component
@RequiredArgsConstructor
public class SalespersonShoppingGuideStatisticsRunnerImpl implements SalespersonShoppingGuideStatisticsRunner {

	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	private final CompanysRecordStatisticsRedisPort recordStatsRedis;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SalespersonStatisticsMapper salespersonStatisticsMapper;
	private final SalespersonStatisticsCronLogPort cronLog;

	@Override
	public int runShoppingGuideBlock(int yesterdayYmd) {
		List<ShopSalesperson> list = shopSalespersonMapper.selectList(
				new QueryWrapper<ShopSalesperson>()
						.eq("salesperson_type", "shopping_guide")
						.select("company_id", "salesperson_id"));
		int rowCount = list.size();
		int inserts = 0;
		for (ShopSalesperson row : list) {
			long companyId = row.getCompanyId();
			long salespersonId = row.getSalespersonId();
			cronLog.debugStart(companyId, salespersonId);
			try {
				inserts += recordSalespersonStatistics(companyId, salespersonId, "normal", yesterdayYmd);
			} catch (Exception e) {
				cronLog.debugError(companyId, salespersonId, e);
			}
			cronLog.debugEnd(companyId, salespersonId);
		}
		return rowCount + inserts;
	}

	/**
	 * 包内单测可覆盖；date 为 null 时在调用方应传入非 null 的昨日 Ymd。
	 */
	int recordSalespersonStatistics(long companyId, long salespersonId, String statisticsType, Integer date) {
		int dateYmd = (date == null) ? yesterdayYmd() : date;
		long epochSec = System.currentTimeMillis() / 1000L;
		long expireAt = epochSec + 3L * 24 * 3600;
		int ds = dateYmd;
		int ins = 0;
		String memberKey = "Member:Salesperson:" + salespersonId + ":Company:" + companyId + ":" + ds;
		long newUserNum = recordStatsRedis.scard(memberKey);
		recordStatsRedis.expireAt(memberKey, expireAt);
		if (newUserNum != 0) {
			ins += insertSalespersonIfAbsent(
					companyId, dateYmd, "newAddMember", "member", (int) newUserNum, salespersonId);
		}

		String orderHash = orderPaySalespersonKey(statisticsType, companyId, salespersonId, dateYmd);
		Map<String, String> statisticsData = recordStatsRedis.hgetall(orderHash);
		recordStatsRedis.expireAt(orderHash, expireAt);
		int fee = parseHashInt(
				statisticsData, salespersonId + "_salesperson_orderPayFee");
		int num = parseHashInt(
				statisticsData, salespersonId + "_salesperson_orderPayNum");
		ins += insertSalespersonIfAbsent(companyId, dateYmd, "orderPayFee", statisticsType, fee, salespersonId);
		ins += insertSalespersonIfAbsent(companyId, dateYmd, "orderPayNum", statisticsType, num, salespersonId);
		return ins;
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

	private static String orderPaySalespersonKey(
			String statisticsType, long companyId, long salespersonId, int date) {
		return "OrderPaySalespersonStatistics:"
				+ statisticsType + ":"
				+ companyId + ":SalespersonId:"
				+ salespersonId + ":"
				+ date;
	}

	private static int parseHashInt(Map<String, String> data, String field) {
		return Optional.ofNullable(data.get(field))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(s -> {
					try {
						return Integer.parseInt(s);
					} catch (NumberFormatException e) {
						return 0;
					}
				})
				.orElse(0);
	}

	private static int yesterdayYmd() {
		return Integer.parseInt(
				ZonedDateTime.now(APP_ZONE).toLocalDate().minusDays(1).format(YMD));
	}
}
