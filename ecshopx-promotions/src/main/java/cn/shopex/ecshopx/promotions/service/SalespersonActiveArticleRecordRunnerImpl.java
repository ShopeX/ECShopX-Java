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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.cron.CompanysRecordStatisticsRedisPort;
import cn.shopex.ecshopx.common.cron.SalespersonActiveArticleRecordRunner;
import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogKind;
import cn.shopex.ecshopx.common.cron.SalespersonStatisticsCronLogPort;
import cn.shopex.ecshopx.promotions.domain.SalespersonActiveArticleStatistics;
import cn.shopex.ecshopx.promotions.mapper.SalespersonActiveArticleStatisticsMapper;
import cn.shopex.ecshopx.salesperson.domain.ShopSalesperson;
import cn.shopex.ecshopx.salesperson.mapper.ShopSalespersonMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 活动转发 Redis Hash 读和与 {@code salesperson_active_article_statistics} 幂等写入。
 */
@Component
@RequiredArgsConstructor
public class SalespersonActiveArticleRecordRunnerImpl implements SalespersonActiveArticleRecordRunner {

	private final CompanysRecordStatisticsRedisPort recordStatsRedis;
	private final ShopSalespersonMapper shopSalespersonMapper;
	private final SalespersonActiveArticleStatisticsMapper activeArticleStatisticsMapper;
	private final SalespersonStatisticsCronLogPort cronLog;

	@Override
	public int runActiveArticleBlock(int yesterdayYmd) {
		List<ShopSalesperson> list = shopSalespersonMapper.selectList(
				new QueryWrapper<ShopSalesperson>()
						.eq("salesperson_type", "shopping_guide")
						.select("company_id", "salesperson_id"));
		int rowCount = list.size();
		int inserts = 0;
		for (ShopSalesperson row : list) {
			long companyId = row.getCompanyId();
			long salespersonId = row.getSalespersonId();
			cronLog.debugStart(SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD, companyId, salespersonId);
			try {
				inserts += recordActiveArticleStatistics(companyId, salespersonId, yesterdayYmd);
			} catch (Exception e) {
				cronLog.debugError(
						SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD, companyId, salespersonId, e);
			}
			cronLog.debugEnd(SalespersonStatisticsCronLogKind.ACTIVE_ARTICLE_FORWARD, companyId, salespersonId);
		}
		return rowCount + inserts;
	}

	int recordActiveArticleStatistics(long companyId, long salespersonId, int dateYmd) {
		String articleKey = activeArticleRedisKey(companyId, salespersonId);
		Map<String, String> fields = recordStatsRedis.hgetall(articleKey);
		long activeArticleSum = sumHashValues(fields);
		return insertActiveArticleIfAbsent(companyId, salespersonId, dateYmd, (int) activeArticleSum);
	}

	private static String activeArticleRedisKey(long companyId, long salespersonId) {
		return "ActiveArticleForwardTimes:Company:"
				+ companyId
				+ ":Salesperson:"
				+ salespersonId;
	}

	private static long sumHashValues(Map<String, String> fields) {
		if (fields == null || fields.isEmpty()) {
			return 0L;
		}
		long sum = 0L;
		for (String v : fields.values()) {
			sum += parseLongLoose(v);
		}
		return sum;
	}

	private static long parseLongLoose(String s) {
		if (s == null || s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private int insertActiveArticleIfAbsent(long companyId, long salespersonId, int addDate, int dataValue) {
		SalespersonActiveArticleStatistics found = activeArticleStatisticsMapper.selectOne(
				new LambdaQueryWrapper<SalespersonActiveArticleStatistics>()
						.eq(SalespersonActiveArticleStatistics::getCompanyId, companyId)
						.eq(SalespersonActiveArticleStatistics::getSalespersonId, salespersonId)
						.eq(SalespersonActiveArticleStatistics::getAddDate, addDate));
		if (found != null) {
			return 0;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		SalespersonActiveArticleStatistics row = new SalespersonActiveArticleStatistics();
		row.setCompanyId(companyId);
		row.setSalespersonId(salespersonId);
		row.setAddDate(addDate);
		row.setDataValue(dataValue);
		row.setCreated(now);
		row.setUpdated(now);
		activeArticleStatisticsMapper.insert(row);
		return 1;
	}
}
