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
import cn.shopex.ecshopx.datacube.domain.DistributorData;
import cn.shopex.ecshopx.datacube.mapper.DistributorDataMapper;
import cn.shopex.ecshopx.datacube.mapper.DistributorDataStatisticsMapper;
import cn.shopex.ecshopx.datacube.service.distributordata.DistributorDataStatisticAsyncExecutor;
import cn.shopex.ecshopx.datacube.service.distributordata.ScheduleDistributorDailyInitResult;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.members.domain.ShopRelMember;
import cn.shopex.ecshopx.members.mapper.ShopRelMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DistributorDataService {

	private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

	private final CompanysMapper companysMapper;
	private final DistributorMapper distributorMapper;
	private final DistributorDataMapper distributorDataMapper;
	private final DistributorDataStatisticsMapper distributorDataStatisticsMapper;
	private final ShopRelMemberMapper shopRelMemberMapper;
	private final DistributorDataStatisticAsyncExecutor distributorDataStatisticAsyncExecutor;
	private final Clock clock;

	public DistributorDataService(
			CompanysMapper companysMapper,
			DistributorMapper distributorMapper,
			DistributorDataMapper distributorDataMapper,
			DistributorDataStatisticsMapper distributorDataStatisticsMapper,
			ShopRelMemberMapper shopRelMemberMapper,
			DistributorDataStatisticAsyncExecutor distributorDataStatisticAsyncExecutor,
			@Qualifier("companyDataStatisticsClock") Clock clock) {
		this.companysMapper = companysMapper;
		this.distributorMapper = distributorMapper;
		this.distributorDataMapper = distributorDataMapper;
		this.distributorDataStatisticsMapper = distributorDataStatisticsMapper;
		this.shopRelMemberMapper = shopRelMemberMapper;
		this.distributorDataStatisticAsyncExecutor = distributorDataStatisticAsyncExecutor;
		this.clock = clock;
	}

	public ScheduleDistributorDailyInitResult scheduleInitStatistic() {
		log.info("执行统计商城门店数据初始化脚本");
		LocalDate countDate = LocalDate.now(clock).minusDays(1);
		List<Companys> companies = companysMapper.selectList(new QueryWrapper<Companys>().select("company_id"));
		if (companies == null || companies.isEmpty()) {
			return new ScheduleDistributorDailyInitResult(0);
		}
		int jobsEnqueued = 0;
		for (Companys c : companies) {
			long companyId = c.getCompanyId() != null ? c.getCompanyId() : 0L;
			List<Distributor> rows = distributorMapper.selectList(
					new QueryWrapper<Distributor>().eq("company_id", companyId).select("distributor_id", "company_id", "merchant_id"));
			List<Distributor> work = new ArrayList<>();
			Distributor head = new Distributor();
			head.setDistributorId(0L);
			head.setCompanyId(companyId);
			head.setMerchantId(0L);
			work.add(head);
			if (rows != null) {
				work.addAll(rows);
			}
			for (Distributor vv : work) {
				long distId = vv.getDistributorId() != null ? vv.getDistributorId() : 0L;
				long merId = vv.getMerchantId() != null ? vv.getMerchantId() : 0L;
				QueryWrapper<DistributorData> w = new QueryWrapper<DistributorData>()
						.eq("count_date", countDate)
						.eq("company_id", companyId)
						.eq("distributor_id", distId);
				long n = distributorDataMapper.selectCount(w);
				if (n == 0) {
					DistributorData row = new DistributorData();
					row.setCompanyId(companyId);
					row.setDistributorId(distId);
					row.setCountDate(countDate);
					row.setMerchantId(merId);
					distributorDataMapper.insert(row);
				}
				distributorDataStatisticAsyncExecutor.runStatisticsAsync(companyId, distId, merId, countDate);
				jobsEnqueued++;
			}
		}
		return new ScheduleDistributorDailyInitResult(jobsEnqueued);
	}

	public void runStatistics(long companyId, long distributorId, long merchantId, LocalDate countDate) {
		log.info(
				"统计商城加经销商数据开始,参数{company_id:"
						+ companyId
						+ ",distributor_id:"
						+ distributorId
						+ ",count_date:"
						+ countDate
						+ "}");
		if (companyId == 0L) {
			throw new IllegalArgumentException("必须指定company_id才能统计数据");
		}
		if (countDate == null) {
			throw new IllegalArgumentException("必须填写日期，且格式为为\"Y-m-d\"");
		}
		long start = countDate.atStartOfDay(SH).toEpochSecond();
		long end = countDate.atTime(23, 59, 59).atZone(SH).toEpochSecond();

		int member;
		if (distributorId == 0L) {
			member = 0;
		} else {
			long m = shopRelMemberMapper.selectCount(
					new QueryWrapper<ShopRelMember>()
							.eq("company_id", companyId)
							.eq("shop_id", distributorId)
							.ge("created", start)
							.le("created", end));
			member = (int) Math.min(m, Integer.MAX_VALUE);
		}
		long after = nz(distributorDataStatisticsMapper.countAftersales(companyId, distributorId, start, end));
		int aftersales = (int) Math.min(after, Integer.MAX_VALUE);
		long refSum = nz(distributorDataStatisticsMapper.sumRefundedFee(companyId, distributorId, start, end));
		int refunded = (int) Math.min(refSum, Integer.MAX_VALUE);
		long amount = nz(distributorDataStatisticsMapper.sumAmountPayed(companyId, distributorId, start, end));
		long amountPoint = nz(distributorDataStatisticsMapper.sumAmountPointPayed(companyId, distributorId, start, end));
		long orderCnt = nz(distributorDataStatisticsMapper.countOrders(companyId, distributorId, start, end));
		int orderCount = (int) Math.min(orderCnt, Integer.MAX_VALUE);
		long orderPointCnt = nz(distributorDataStatisticsMapper.countOrderPoint(companyId, distributorId, start, end));
		int orderPoint = (int) Math.min(orderPointCnt, Integer.MAX_VALUE);
		long payedCnt = nz(distributorDataStatisticsMapper.countTradesSuccessWindow(companyId, distributorId, start, end));
		int orderPayed = (int) Math.min(payedCnt, Integer.MAX_VALUE);
		long pointPayedCnt = nz(
				distributorDataStatisticsMapper.countTradesPointPayedWindow(companyId, distributorId, start, end));
		int orderPointPayed = (int) Math.min(pointPayedCnt, Integer.MAX_VALUE);
		long gmv = nz(distributorDataStatisticsMapper.sumGmv(companyId, distributorId, start, end));
		long gmvPoint = nz(distributorDataStatisticsMapper.sumGmvPoint(companyId, distributorId, start, end));

		var uw = new UpdateWrapper<DistributorData>();
		uw.set("member_count", member)
				.set("aftersales_count", aftersales)
				.set("refunded_count", refunded)
				.set("amount_payed_count", amount)
				.set("amount_point_payed_count", amountPoint)
				.set("order_count", orderCount)
				.set("order_point_count", orderPoint)
				.set("order_payed_count", orderPayed)
				.set("order_point_payed_count", orderPointPayed)
				.set("gmv_count", gmv)
				.set("gmv_point_count", gmvPoint)
				.eq("count_date", countDate)
				.eq("company_id", companyId)
				.eq("distributor_id", distributorId)
				.eq("merchant_id", merchantId);
		distributorDataMapper.update(null, uw);
		log.info("统计商城加经销商数据结束");
	}

	private static long nz(Long v) {
		return v == null ? 0L : v;
	}
}
