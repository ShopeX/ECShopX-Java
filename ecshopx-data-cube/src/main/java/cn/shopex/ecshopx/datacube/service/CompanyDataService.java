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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.datacube.domain.CompanyData;
import cn.shopex.ecshopx.datacube.mapper.CompanyDataMapper;
import cn.shopex.ecshopx.datacube.mapper.CompanyDataStatisticsMapper;
import cn.shopex.ecshopx.datacube.service.companydata.CompanyDataStatisticAsyncExecutor;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class CompanyDataService {

	private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

	private final CompanysMapper companysMapper;
	private final CompanyDataMapper companyDataMapper;
	private final CompanyDataStatisticsMapper companyDataStatisticsMapper;
	private final CompanyDataStatisticAsyncExecutor companyDataStatisticAsyncExecutor;
	private final ActivitiesMapper activitiesMapper;
	private final Clock clock;

	public CompanyDataService(
			CompanysMapper companysMapper,
			CompanyDataMapper companyDataMapper,
			CompanyDataStatisticsMapper companyDataStatisticsMapper,
			CompanyDataStatisticAsyncExecutor companyDataStatisticAsyncExecutor,
			ActivitiesMapper activitiesMapper,
			@Qualifier("companyDataStatisticsClock") Clock clock) {
		this.companysMapper = companysMapper;
		this.companyDataMapper = companyDataMapper;
		this.companyDataStatisticsMapper = companyDataStatisticsMapper;
		this.companyDataStatisticAsyncExecutor = companyDataStatisticAsyncExecutor;
		this.activitiesMapper = activitiesMapper;
		this.clock = clock;
	}

	public void scheduleInitStatistic() {
		scheduleInitStatistic(null, null, null);
	}

	/** 内购日统计：枚举昨日（或 override）在窗活动，对每一活动走与全站 init 同构的 company 维初始化与异步 runStatistics。 */
	public void scheduleInitEmployeePurchaseStatistic() {
		scheduleInitEmployeePurchaseStatistic(null);
	}

	/** 手工补跑时覆盖统计日；为 null 时与无参版一致（昨日，与时钟一致）。 */
	public void scheduleInitEmployeePurchaseStatistic(LocalDate countDateOverride) {
		LocalDate countDate = countDateOverride != null ? countDateOverride : LocalDate.now(clock).minusDays(1);
		long start = countDate.atStartOfDay(SH).toEpochSecond();
		List<Long> actIds = activitiesMapper.selectActiveIdsForCubeStatistic(start);
		if (actIds == null || actIds.isEmpty()) {
			return;
		}
		for (Long actId : actIds) {
			if (actId == null) {
				continue;
			}
			scheduleInitStatistic(countDate, "employee_purchase", actId);
		}
	}

	/**
	 * 与 DataCube 默认调度对齐：可指定统计日、order 维度与活动；Kernel 无参时等价于昨日、全站维度、act=0。
	 */
	public void scheduleInitStatistic(LocalDate countDate, String orderClass, Long actId) {
		LocalDate targetDate = countDate != null ? countDate : LocalDate.now(clock).minusDays(1);
		long act = actId != null ? actId : 0L;
		String oc = StringUtils.hasText(orderClass) ? orderClass : null;
		log.info("执行统计商城数据初始化脚本");
		var all = companysMapper.selectList(new QueryWrapper<Companys>().select("company_id"));
		for (Companys c : all) {
			long companyId = c.getCompanyId() != null ? c.getCompanyId() : 0L;
			QueryWrapper<CompanyData> w = new QueryWrapper<CompanyData>()
					.eq("count_date", targetDate)
					.eq("company_id", companyId);
			if (oc != null) {
				w.eq("order_class", oc).eq("act_id", act);
			} else {
				w.isNull("order_class").eq("act_id", 0L);
			}
			long n = companyDataMapper.selectCount(w);
			if (n == 0) {
				CompanyData row = new CompanyData();
				row.setCompanyId(companyId);
				row.setCountDate(targetDate);
				row.setActId(act);
				row.setMemberCount(0);
				row.setAftersalesCount(0);
				row.setRefundedCount(0);
				row.setAmountPayedCount(0L);
				row.setOrderCount(0);
				row.setOrderPayedCount(0);
				row.setGmvCount(0L);
				if (oc != null) {
					row.setOrderClass(oc);
				}
				companyDataMapper.insert(row);
			}
			companyDataStatisticAsyncExecutor.runStatisticsAsync(companyId, targetDate, oc, act);
		}
	}

	public void runStatistics(long companyId, LocalDate countDate, String orderClass, long actId) {
		log.info("统计商城数据开始,参数{company_id:" + companyId + ",count_date:" + countDate + "}");
		if (companyId == 0L) {
			throw new IllegalArgumentException("必须指定company_id才能统计数据");
		}
		if (countDate == null) {
			throw new IllegalArgumentException("必须填写日期，且格式为为\"Y-m-d\"");
		}
		long start = countDate.atStartOfDay(SH).toEpochSecond();
		long end = countDate.atTime(23, 59, 59).atZone(SH).toEpochSecond();
		String dimOrderClass = StringUtils.hasText(orderClass) ? orderClass : null;
		int member = 0;
		if (dimOrderClass == null) {
			Long m = companyDataStatisticsMapper.countNewMembers(companyId, start, end);
			member = m == null ? 0 : m.intValue();
		}
		long after = nz(companyDataStatisticsMapper.countAftersales(companyId, start, end, dimOrderClass, actId));
		int aftersales = (int) Math.min(after, Integer.MAX_VALUE);
		long refSum = nz(companyDataStatisticsMapper.sumRefundedFee(companyId, start, end, dimOrderClass, actId));
		int refunded = (int) Math.min(refSum, Integer.MAX_VALUE);
		long amount = nz(companyDataStatisticsMapper.sumAmountPayed(companyId, start, end, dimOrderClass, actId));
		long orderCnt = nz(companyDataStatisticsMapper.countOrders(companyId, start, end, dimOrderClass, actId));
		int orderCount = (int) Math.min(orderCnt, Integer.MAX_VALUE);
		long payedCnt = nz(companyDataStatisticsMapper.countTradesSuccessWindow(companyId, start, end, dimOrderClass, actId));
		int orderPayed = (int) Math.min(payedCnt, Integer.MAX_VALUE);
		long gmv = nz(companyDataStatisticsMapper.sumGmv(companyId, start, end, dimOrderClass, actId));
		var uw = new UpdateWrapper<CompanyData>();
		uw.set("member_count", member)
				.set("aftersales_count", aftersales)
				.set("refunded_count", refunded)
				.set("amount_payed_count", amount)
				.set("order_count", orderCount)
				.set("order_payed_count", orderPayed)
				.set("gmv_count", gmv)
				.eq("count_date", countDate)
				.eq("company_id", companyId);
		if (dimOrderClass != null) {
			uw.eq("order_class", dimOrderClass).eq("act_id", actId);
		} else {
			uw.isNull("order_class").eq("act_id", 0L);
		}
		companyDataMapper.update(null, uw);
		log.info("统计商城数据结束");
	}

	private static long nz(Long v) {
		return v == null ? 0L : v;
	}
}
