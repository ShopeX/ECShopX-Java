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

import cn.shopex.ecshopx.datacube.domain.GoodsData;
import cn.shopex.ecshopx.datacube.mapper.GoodsDailyStatisticsQueryMapper;
import cn.shopex.ecshopx.datacube.mapper.GoodsDataMapper;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDailyStatLineKey;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDataStatisticAsyncExecutor;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsStatisticsBatch;
import cn.shopex.ecshopx.datacube.service.goodsdata.ScheduleGoodsDailyInitResult;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GoodsDataService {

	private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

	private static final List<String> TRADE_STATES =
			List.of("REFUND_PROCESS", "REFUND_SUCCESS", "SUCCESS");

	private static final int PAGE = 100;

	private final GoodsDailyStatisticsQueryMapper goodsDailyStatisticsQueryMapper;
	private final GoodsDataStatisticAsyncExecutor goodsDataStatisticAsyncExecutor;
	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final GoodsDataMapper goodsDataMapper;
	private final DistributorMapper distributorMapper;
	private final ActivitiesMapper activitiesMapper;
	private final Clock clock;

	public GoodsDataService(
			GoodsDailyStatisticsQueryMapper goodsDailyStatisticsQueryMapper,
			GoodsDataStatisticAsyncExecutor goodsDataStatisticAsyncExecutor,
			NormalOrdersItemsMapper normalOrdersItemsMapper,
			GoodsDataMapper goodsDataMapper,
			DistributorMapper distributorMapper,
			ActivitiesMapper activitiesMapper,
			@Qualifier("companyDataStatisticsClock") Clock clock) {
		this.goodsDailyStatisticsQueryMapper = goodsDailyStatisticsQueryMapper;
		this.goodsDataStatisticAsyncExecutor = goodsDataStatisticAsyncExecutor;
		this.normalOrdersItemsMapper = normalOrdersItemsMapper;
		this.goodsDataMapper = goodsDataMapper;
		this.distributorMapper = distributorMapper;
		this.activitiesMapper = activitiesMapper;
		this.clock = clock;
	}

	public ScheduleGoodsDailyInitResult scheduleInitStatistic() {
		log.info("执行统计商品数据初始化脚本");
		LocalDate countDate = LocalDate.now(clock).minusDays(1);
		long start = countDate.atStartOfDay(SH).toEpochSecond();
		long end = countDate.atTime(23, 59, 59).atZone(SH).toEpochSecond();
		long count = goodsDailyStatisticsQueryMapper.countKernelJoinWindow(start, end, TRADE_STATES);
		int pageSize = (int) Math.ceil(count / (double) PAGE);
		int jobsEnqueued = 0;
		for (int i = 0; i < pageSize; i++) {
			List<GoodsDailyStatLineKey> page =
					goodsDailyStatisticsQueryMapper.selectKernelJoinPage(
							start, end, TRADE_STATES, i * PAGE, PAGE);
			if (page != null && !page.isEmpty()) {
				goodsDataStatisticAsyncExecutor.runBatchAsync(new GoodsStatisticsBatch(page), countDate);
				jobsEnqueued++;
			}
		}
		return new ScheduleGoodsDailyInitResult(jobsEnqueued);
	}

	public ScheduleGoodsDailyInitResult scheduleInitEmployeePurchaseStatistic() {
		return scheduleInitEmployeePurchaseStatistic(null);
	}

	public ScheduleGoodsDailyInitResult scheduleInitEmployeePurchaseStatistic(LocalDate countDateOverride) {
		LocalDate countDate =
				countDateOverride != null ? countDateOverride : LocalDate.now(clock).minusDays(1);
		long activityWindowStart = countDate.atStartOfDay(SH).toEpochSecond();
		List<Long> actIds = activitiesMapper.selectActiveIdsForCubeStatistic(activityWindowStart);
		if (actIds == null || actIds.isEmpty()) {
			return new ScheduleGoodsDailyInitResult(0);
		}
		long tradeStart = activityWindowStart;
		long tradeEnd = countDate.atTime(23, 59, 59).atZone(SH).toEpochSecond();
		int jobsEnqueued = 0;
		for (Long actIdRaw : actIds) {
			if (actIdRaw == null) {
				continue;
			}
			long actId = actIdRaw;
			log.info("执行统计商品数据初始化脚本");
			long count =
					goodsDailyStatisticsQueryMapper.countEmployeePurchaseJoinWindow(
							tradeStart, tradeEnd, TRADE_STATES, actId);
			int pageSize = (int) Math.ceil(count / (double) PAGE);
			for (int i = 0; i < pageSize; i++) {
				List<GoodsDailyStatLineKey> page =
						goodsDailyStatisticsQueryMapper.selectEmployeePurchaseJoinPage(
								tradeStart, tradeEnd, TRADE_STATES, actId, i * PAGE, PAGE);
				if (page != null && !page.isEmpty()) {
					goodsDataStatisticAsyncExecutor.runEmployeePurchaseBatchAsync(
							new GoodsStatisticsBatch(page), countDate, actId);
					jobsEnqueued++;
				}
			}
		}
		return new ScheduleGoodsDailyInitResult(jobsEnqueued);
	}

	public void runStatistics(GoodsStatisticsBatch batch, LocalDate countDate) {
		runStatistics(batch, countDate, false, null, 0L);
	}

	public void runStatistics(GoodsStatisticsBatch batch, LocalDate countDate, String orderClass, long actId) {
		runStatistics(batch, countDate, true, orderClass, actId);
	}

	private void runStatistics(
			GoodsStatisticsBatch batch,
			LocalDate countDate,
			boolean employeeDimension,
			String orderClass,
			long actId) {
		List<GoodsDailyStatLineKey> lines = batch.lines();
		log.info("统计商品数据开始,参数{order_ids:{},count_date:{}}", lines, countDate);
		if (lines == null || lines.isEmpty()) {
			throw new IllegalArgumentException("必须指定order_id才能统计数据");
		}
		if (countDate == null) {
			throw new IllegalArgumentException("必须填写日期，且格式为为\"Y-m-d\"");
		}
		for (GoodsDailyStatLineKey key : lines) {
			long oid = key.getOrderId() != null ? key.getOrderId() : 0L;
			long lid = key.getLineId() != null ? key.getLineId() : 0L;
			log.info("订单{order_ids:{}开始", oid);
			NormalOrdersItems order =
					normalOrdersItemsMapper.selectOne(
							new QueryWrapper<NormalOrdersItems>()
									.eq("order_id", oid)
									.eq("id", lid));
			if (order == null) {
				log.warn("orders_normal_orders_items missing for order_id={}, id={}", oid, lid);
				continue;
			}
			long merchantId = 0L;
			Long distId = order.getDistributorId();
			if (distId != null && distId != 0L) {
				Map<String, Object> q = new HashMap<>();
				q.put("distributor_id", distId);
				q.put("company_id", order.getCompanyId());
				Map<String, Object> distRow = distributorMapper.selectDistributorRowDynamic(q);
				merchantId = longFromMap(distRow, "merchant_id");
			}
			long companyIdVal = nzL(order.getCompanyId());
			long itemIdVal = nzL(order.getItemId());
			QueryWrapper<GoodsData> existW =
					new QueryWrapper<GoodsData>()
							.eq("count_date", countDate)
							.eq("company_id", companyIdVal)
							.eq("item_id", itemIdVal);
			if (employeeDimension) {
				existW.eq("order_class", orderClass).eq("act_id", actId);
			} else {
				existW.isNull("order_class").eq("act_id", 0L);
			}
			GoodsData existing = goodsDataMapper.selectOne(existW);
			int num = order.getNum() != null ? order.getNum() : 0;
			long itemFee = intFeeToLong(order.getItemFee());
			long totalFee = intFeeToLong(order.getTotalFee());
			if (existing == null) {
				GoodsData row = new GoodsData();
				row.setCompanyId(order.getCompanyId() != null ? order.getCompanyId() : 0L);
				row.setCountDate(countDate);
				row.setItemId(order.getItemId() != null ? order.getItemId() : 0L);
				row.setSalesCount(num);
				row.setFixedAmountCount(itemFee);
				row.setSettleAmountCount(totalFee);
				row.setMerchantId(merchantId);
				if (employeeDimension) {
					row.setOrderClass(orderClass);
					row.setActId(actId);
				} else {
					row.setActId(0L);
					row.setOrderClass(null);
				}
				goodsDataMapper.insert(row);
			} else {
				UpdateWrapper<GoodsData> uw =
						new UpdateWrapper<GoodsData>()
								.set(
										"sales_count",
										nz(existing.getSalesCount()) + num)
								.set(
										"fixed_amount_count",
										nzLong(existing.getFixedAmountCount()) + itemFee)
								.set(
										"settle_amount_count",
										nzLong(existing.getSettleAmountCount()) + totalFee)
								.eq("count_date", countDate)
								.eq("company_id", companyIdVal)
								.eq("item_id", itemIdVal);
				if (employeeDimension) {
					uw.eq("order_class", orderClass).eq("act_id", actId);
				} else {
					uw.isNull("order_class").eq("act_id", 0L);
				}
				goodsDataMapper.update(null, uw);
			}
			log.info("订单{order_ids:{}结束", oid);
		}
		log.info("统计商品数据结束");
	}

	private static int nz(Integer v) {
		return v == null ? 0 : v;
	}

	private static long nzL(Long v) {
		return v == null ? 0L : v;
	}

	private static long nzLong(Long v) {
		return v == null ? 0L : v;
	}

	private static long intFeeToLong(Integer v) {
		return v == null ? 0L : v.longValue();
	}

	private static long longFromMap(Map<String, Object> row, String key) {
		if (row == null) {
			return 0L;
		}
		Object v = row.get(key);
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
