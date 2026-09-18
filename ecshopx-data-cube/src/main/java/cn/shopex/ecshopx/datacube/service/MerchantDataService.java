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

import cn.shopex.ecshopx.datacube.domain.MerchantData;
import cn.shopex.ecshopx.datacube.mapper.MerchantDataMapper;
import cn.shopex.ecshopx.datacube.mapper.MerchantDataStatisticsMapper;
import cn.shopex.ecshopx.datacube.service.merchantdata.MerchantDataStatisticAsyncExecutor;
import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MerchantDataService {

	private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

	private final MerchantMapper merchantMapper;
	private final MerchantDataMapper merchantDataMapper;
	private final MerchantDataStatisticsMapper merchantDataStatisticsMapper;
	private final MerchantDataStatisticAsyncExecutor merchantDataStatisticAsyncExecutor;
	private final Clock clock;

	public MerchantDataService(
			MerchantMapper merchantMapper,
			MerchantDataMapper merchantDataMapper,
			MerchantDataStatisticsMapper merchantDataStatisticsMapper,
			MerchantDataStatisticAsyncExecutor merchantDataStatisticAsyncExecutor,
			@Qualifier("companyDataStatisticsClock") Clock clock) {
		this.merchantMapper = merchantMapper;
		this.merchantDataMapper = merchantDataMapper;
		this.merchantDataStatisticsMapper = merchantDataStatisticsMapper;
		this.merchantDataStatisticAsyncExecutor = merchantDataStatisticAsyncExecutor;
		this.clock = clock;
	}

	public void scheduleInitStatistic() {
		log.info("执行统计商城数据初始化脚本");
		LocalDate countDate = LocalDate.now(clock).minusDays(1);
		List<Merchant> all =
				merchantMapper.selectList(new QueryWrapper<Merchant>().select("id", "company_id"));
		if (all == null) {
			return;
		}
		for (Merchant m : all) {
			long mid = m.getId() != null ? m.getId() : 0L;
			long cid = m.getCompanyId() != null ? m.getCompanyId() : 0L;
			QueryWrapper<MerchantData> w = new QueryWrapper<MerchantData>()
					.eq("count_date", countDate)
					.eq("merchant_id", mid)
					.eq("company_id", cid);
			long n = merchantDataMapper.selectCount(w);
			if (n == 0) {
				MerchantData row = new MerchantData();
				row.setMerchantId(mid);
				row.setCompanyId(cid);
				row.setCountDate(countDate);
				merchantDataMapper.insert(row);
			}
			merchantDataStatisticAsyncExecutor.runStatisticsAsync(cid, mid, countDate);
		}
	}

	public void runStatistics(long companyId, long merchantId, LocalDate countDate) {
		log.info("统计商城数据开始,参数{company_id:" + companyId + ",count_date:" + countDate + "}");
		if (companyId == 0L) {
			throw new IllegalArgumentException("必须指定company_id才能统计数据");
		}
		if (merchantId == 0L) {
			throw new IllegalArgumentException("必须指定merchant_id才能统计数据");
		}
		if (countDate == null) {
			throw new IllegalArgumentException("必须填写日期，且格式为为\"Y-m-d\"");
		}
		long start = countDate.atStartOfDay(SH).toEpochSecond();
		long end = countDate.atTime(23, 59, 59).atZone(SH).toEpochSecond();
		long after = nz(merchantDataStatisticsMapper.countAftersales(companyId, merchantId, start, end));
		int aftersales = (int) Math.min(after, Integer.MAX_VALUE);
		long refSum = nz(merchantDataStatisticsMapper.sumRefundedFee(companyId, merchantId, start, end));
		int refunded = (int) Math.min(refSum, Integer.MAX_VALUE);
		long amount = nz(merchantDataStatisticsMapper.sumAmountPayed(companyId, merchantId, start, end));
		long amountPoint = nz(merchantDataStatisticsMapper.sumAmountPointPayed(companyId, merchantId, start, end));
		long orderCnt = nz(merchantDataStatisticsMapper.countOrders(companyId, merchantId, start, end));
		int orderCount = (int) Math.min(orderCnt, Integer.MAX_VALUE);
		long orderPointCnt = nz(merchantDataStatisticsMapper.countOrderPoint(companyId, merchantId, start, end));
		int orderPoint = (int) Math.min(orderPointCnt, Integer.MAX_VALUE);
		long payedCnt = nz(merchantDataStatisticsMapper.countTradesSuccessWindow(companyId, merchantId, start, end));
		int orderPayed = (int) Math.min(payedCnt, Integer.MAX_VALUE);
		long pointPayedCnt = nz(merchantDataStatisticsMapper.countTradesPointPayedWindow(companyId, merchantId, start, end));
		int orderPointPayed = (int) Math.min(pointPayedCnt, Integer.MAX_VALUE);
		long gmv = nz(merchantDataStatisticsMapper.sumGmv(companyId, merchantId, start, end));
		long gmvPoint = nz(merchantDataStatisticsMapper.sumGmvPoint(companyId, merchantId, start, end));
		var uw = new UpdateWrapper<MerchantData>();
		uw.set("aftersales_count", aftersales)
				.set("refunded_count", refunded)
				.set("amount_payed_count", amount)
				.set("amount_point_payed_count", amountPoint)
				.set("order_count", orderCount)
				.set("order_point_count", orderPoint)
				.set("order_payed_count", orderPayed)
				.set("order_point_payed_count", orderPointPayed)
				.set("gmv_count", gmv)
				.set("gmv_point_count", gmvPoint)
				.set("merchant_id", merchantId)
				.eq("count_date", countDate)
				.eq("company_id", companyId)
				.eq("merchant_id", merchantId);
		merchantDataMapper.update(null, uw);
		log.info("统计商城数据结束");
	}

	private static long nz(Long v) {
		return v == null ? 0L : v;
	}
}
