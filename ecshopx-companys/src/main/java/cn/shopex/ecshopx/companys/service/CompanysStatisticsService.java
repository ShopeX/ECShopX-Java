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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.common.cron.CompanysRecordStatisticsRedisPort;
import cn.shopex.ecshopx.common.cron.SalespersonActiveArticleRecordRunner;
import cn.shopex.ecshopx.common.cron.SalespersonCommissionRecordStatisticsRunner;
import cn.shopex.ecshopx.common.cron.SalespersonGiveCouponsRecordStatisticsRunner;
import cn.shopex.ecshopx.common.cron.SalespersonPopularizeRecordStatisticsRunner;
import cn.shopex.ecshopx.common.cron.SalespersonShoppingGuideStatisticsRunner;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.domain.Statistics;
import cn.shopex.ecshopx.companys.domain.StoreStatistics;
import cn.shopex.ecshopx.companys.dto.ScheduleRecordActiveArticleStatisticsResult;
import cn.shopex.ecshopx.companys.dto.ScheduleRecordCommissionStatisticsResult;
import cn.shopex.ecshopx.companys.dto.ScheduleRecordGiveCouponsStatisticsResult;
import cn.shopex.ecshopx.companys.dto.ScheduleRecordPopularizeStatisticsResult;
import cn.shopex.ecshopx.companys.dto.ScheduleRecordStatisticsResult;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.companys.mapper.StatisticsMapper;
import cn.shopex.ecshopx.companys.mapper.StoreStatisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** 商城日统计定时写库；全公司与导购分段组合入口。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanysStatisticsService {

	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	private final CompanysRecordStatisticsRedisPort recordStatsRedis;
	private final CompanysMapper companysMapper;
	private final StatisticsMapper statisticsMapper;
	private final StoreStatisticsMapper storeStatisticsMapper;
	private final ObjectProvider<SalespersonShoppingGuideStatisticsRunner> salespersonRunnerProvider;
	private final ObjectProvider<SalespersonActiveArticleRecordRunner> activeArticleRunnerProvider;
	private final ObjectProvider<SalespersonCommissionRecordStatisticsRunner> commissionRunnerProvider;
	private final ObjectProvider<SalespersonPopularizeRecordStatisticsRunner> popularizeRunnerProvider;
	private final ObjectProvider<SalespersonGiveCouponsRecordStatisticsRunner> giveCouponsRunnerProvider;

	public ScheduleRecordStatisticsResult scheduleRecordStatistics() {
		ScheduleRecordStatisticsResult company = runCompanyRecordStatisticsBlockForYesterday();
		int runnerPart = runShoppingGuideBlockForYesterday();
		return new ScheduleRecordStatisticsResult(company.processed() + runnerPart);
	}

	/**
	 * 全公司日统计写入（service + normal），不含导购块；供队列 handler 与组合入口复用。
	 */
	public ScheduleRecordStatisticsResult runCompanyRecordStatisticsBlockForYesterday() {
		int yesterdayYmd = yesterdayYmd();
		int companyRows = 0;
		int insertTotal = 0;
		List<Companys> companys = companysMapper.selectList(new QueryWrapper<Companys>().select("company_id"));
		companyRows = companys.size();
		for (Companys c : companys) {
			long companyId = c.getCompanyId();
			insertTotal += recordStatistics(companyId, "service", yesterdayYmd);
			insertTotal += recordStatistics(companyId, "normal", yesterdayYmd);
		}
		return new ScheduleRecordStatisticsResult(companyRows + insertTotal);
	}

	/**
	 * 导购日统计块（昨日 Ymd），与全公司块在同一次调度内顺序执行时使用。
	 */
	public int runShoppingGuideBlockForYesterday() {
		int yesterdayYmd = yesterdayYmd();
		SalespersonShoppingGuideStatisticsRunner runner = salespersonRunnerProvider.getIfAvailable();
		if (runner == null) {
			return 0;
		}
		return runner.runShoppingGuideBlock(yesterdayYmd);
	}

	/**
	 * 活动转发统计块（昨日 Ymd）；与全公司块组合用于非调度器直连场景。
	 */
	public int runActiveArticleBlockForYesterday() {
		int yesterdayYmd = yesterdayYmd();
		SalespersonActiveArticleRecordRunner activeRunner = activeArticleRunnerProvider.getIfAvailable();
		if (activeRunner == null) {
			return 0;
		}
		return activeRunner.runActiveArticleBlock(yesterdayYmd);
	}

	public ScheduleRecordActiveArticleStatisticsResult scheduleRecordActiveArticleStatistics() {
		ScheduleRecordStatisticsResult company = runCompanyRecordStatisticsBlockForYesterday();
		int activePart = runActiveArticleBlockForYesterday();
		return new ScheduleRecordActiveArticleStatisticsResult(company.processed() + activePart);
	}

	/**
	 * 导购分润统计块（昨日 Ymd）；与全公司队列块组合用于非调度器直连场景。
	 */
	public int runCommissionBlockForYesterday() {
		int yesterdayYmd = yesterdayYmd();
		SalespersonCommissionRecordStatisticsRunner commissionRunner = commissionRunnerProvider.getIfAvailable();
		if (commissionRunner == null) {
			return 0;
		}
		return commissionRunner.runCommissionBlock(yesterdayYmd);
	}

	public ScheduleRecordCommissionStatisticsResult scheduleRecordCommissionStatistics() {
		ScheduleRecordStatisticsResult company = runCompanyRecordStatisticsBlockForYesterday();
		int commissionPart = runCommissionBlockForYesterday();
		return new ScheduleRecordCommissionStatisticsResult(company.processed() + commissionPart);
	}

	/**
	 * 推广统计导购块（昨日 Ymd）；与全公司队列块组合用于非调度器直连场景。
	 */
	public int runPopularizeBlockForYesterday() {
		int yesterdayYmd = yesterdayYmd();
		SalespersonPopularizeRecordStatisticsRunner popularizeRunner = popularizeRunnerProvider.getIfAvailable();
		if (popularizeRunner == null) {
			return 0;
		}
		return popularizeRunner.runPopularizeBlock(yesterdayYmd);
	}

	public ScheduleRecordPopularizeStatisticsResult scheduleRecordPopularizeStatistics() {
		ScheduleRecordStatisticsResult company = runCompanyRecordStatisticsBlockForYesterday();
		int popularizePart = runPopularizeBlockForYesterday();
		return new ScheduleRecordPopularizeStatisticsResult(company.processed() + popularizePart);
	}

	/**
	 * 送券统计导购块（昨日 Ymd）；与全公司队列块组合用于非调度器直连场景。
	 */
	public int runGiveCouponsBlockForYesterday() {
		int yesterdayYmd = yesterdayYmd();
		SalespersonGiveCouponsRecordStatisticsRunner runner = giveCouponsRunnerProvider.getIfAvailable();
		if (runner == null) {
			return 0;
		}
		return runner.runGiveCouponsBlock(yesterdayYmd);
	}

	public ScheduleRecordGiveCouponsStatisticsResult scheduleRecordGiveCouponsStatistics() {
		ScheduleRecordStatisticsResult company = runCompanyRecordStatisticsBlockForYesterday();
		int giveCouponsPart = runGiveCouponsBlockForYesterday();
		return new ScheduleRecordGiveCouponsStatisticsResult(company.processed() + giveCouponsPart);
	}

	/**
	 * 单测可见；{@code date == null} 时使用昨日 Ymd（Asia/Shanghai）。
	 */
	public int recordStatistics(long companyId, String statisticsType, Integer date) {
		int dateYmd = date == null ? yesterdayYmd() : date;
		long expireAt = System.currentTimeMillis() / 1000L + 3L * 24 * 3600;
		int inserts = 0;
		String ds = Integer.toString(dateYmd);

		String memberKey = "Member:" + companyId + ":" + ds;
		long newUserNum = recordStatsRedis.scard(memberKey);
		recordStatsRedis.expireAt(memberKey, expireAt);
		if (newUserNum != 0) {
			inserts += createCompanyLevelRow(companyId, dateYmd, "newAddMember", "member", (int) newUserNum, null);
		}

		String vipKey = "MemberCard:" + companyId + ":vip:" + ds;
		long vipNum = recordStatsRedis.scard(vipKey);
		recordStatsRedis.expireAt(vipKey, expireAt);
		if (vipNum != 0) {
			inserts += createCompanyLevelRow(companyId, dateYmd, "vipMember", "member", (int) vipNum, null);
		}

		String svipKey = "MemberCard:" + companyId + ":svip:" + ds;
		long svipNum = recordStatsRedis.scard(svipKey);
		recordStatsRedis.expireAt(svipKey, expireAt);
		if (svipNum != 0) {
			inserts += createCompanyLevelRow(companyId, dateYmd, "svipMember", "member", (int) svipNum, null);
		}

		String payUserKey = orderPayKey(statisticsType, companyId, ds + "_orderPayUser");
		long payUsers = recordStatsRedis.scard(payUserKey);
		recordStatsRedis.expireAt(payUserKey, expireAt);
		if (payUsers != 0) {
			inserts += createCompanyLevelRow(companyId, dateYmd, "orderPayUser", statisticsType, (int) payUsers, null);
		}

		String orderHashKey = orderPayKey(statisticsType, companyId, ds);
		Map<String, String> statisticsData = recordStatsRedis.hgetall(orderHashKey);
		recordStatsRedis.expireAt(orderHashKey, expireAt);
		for (Map.Entry<String, String> e : statisticsData.entrySet()) {
			String key = e.getKey();
			String value = e.getValue();
			List<String> segs = splitKeySegments(key);
			if (segs.size() == 1) {
				String title = segs.get(0);
				int v = parseIntLoose(value);
				inserts += createCompanyLevelRow(companyId, dateYmd, title, statisticsType, v, null);
			} else if (segs.size() == 2) {
				long shopId = parseLongLoose(segs.get(0));
				String title = segs.get(1);
				int v = parseIntLoose(value);
				if (isTruthyShop(shopId)) {
					inserts += createStoreRow(companyId, dateYmd, title, statisticsType, v, shopId);
					String shopPayKey = orderPayKey(statisticsType, companyId, ds + "_" + shopId + "_orderPayUser");
					long shopPayUsers = recordStatsRedis.scard(shopPayKey);
					recordStatsRedis.expireAt(shopPayKey, expireAt);
					if (shopPayUsers != 0) {
						inserts += createStoreRow(
								companyId, dateYmd, "orderPayUser", statisticsType, (int) shopPayUsers, shopId);
					}
				}
			} else {
				log.warn("skip statistics hash field with unexpected segments: keyField={}", key);
			}
		}
		return inserts;
	}

	private int createCompanyLevelRow(
			long companyId, int addDate, String title, String type, int value, Long shopId) {
		if (shopId != null && isTruthyShop(shopId)) {
			return createStoreRow(companyId, addDate, title, type, value, shopId);
		}
		return insertStatisticsIfAbsent(companyId, addDate, title, type, value);
	}

	private int insertStatisticsIfAbsent(long companyId, int addDate, String title, String type, int value) {
		Statistics found = statisticsMapper.selectOne(
				new LambdaQueryWrapper<Statistics>()
						.eq(Statistics::getCompanyId, companyId)
						.eq(Statistics::getAddDate, addDate)
						.eq(Statistics::getStatisticTitle, title)
						.eq(Statistics::getStatisticType, type));
		if (found != null) {
			return 0;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		Statistics row = new Statistics();
		row.setCompanyId(companyId);
		row.setAddDate(addDate);
		row.setStatisticTitle(title);
		row.setStatisticType(type);
		row.setDataValue(value);
		row.setCreated(now);
		row.setUpdated(now);
		statisticsMapper.insert(row);
		return 1;
	}

	private int createStoreRow(
			long companyId, int addDate, String title, String type, int value, long shopId) {
		StoreStatistics found = storeStatisticsMapper.selectOne(
				new LambdaQueryWrapper<StoreStatistics>()
						.eq(StoreStatistics::getCompanyId, companyId)
						.eq(StoreStatistics::getAddDate, addDate)
						.eq(StoreStatistics::getStatisticTitle, title)
						.eq(StoreStatistics::getStatisticType, type)
						.eq(StoreStatistics::getShopId, shopId));
		if (found != null) {
			return 0;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		StoreStatistics row = new StoreStatistics();
		row.setCompanyId(companyId);
		row.setAddDate(addDate);
		row.setStatisticTitle(title);
		row.setStatisticType(type);
		row.setDataValue(value);
		row.setShopId(shopId);
		row.setCreated(now);
		row.setUpdated(now);
		storeStatisticsMapper.insert(row);
		return 1;
	}

	private static String orderPayKey(String statisticsType, long companyId, String datePart) {
		return "OrderPayStatistics:" + statisticsType + ":" + companyId + ":" + datePart;
	}

	private static List<String> splitKeySegments(String key) {
		String[] parts = key.split("_", -1);
		List<String> segs = new ArrayList<>();
		for (String p : parts) {
			if (p != null && !p.isEmpty()) {
				segs.add(p);
			}
		}
		return segs;
	}

	private static int parseIntLoose(String s) {
		if (s == null || s.isEmpty()) {
			return 0;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
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

	private static boolean isTruthyShop(long shopId) {
		return shopId != 0L;
	}

	private static int yesterdayYmd() {
		return Integer.parseInt(
				ZonedDateTime.now(APP_ZONE).toLocalDate().minusDays(1).format(YMD));
	}
}
