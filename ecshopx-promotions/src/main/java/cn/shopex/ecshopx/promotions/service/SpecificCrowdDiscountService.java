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

import cn.shopex.ecshopx.promotions.domain.SpecificCrowdDiscount;
import cn.shopex.ecshopx.promotions.mapper.SpecificCrowdDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 特定人群促销的定时任务编排：过期扫描等与 HTTP 创建/编辑职责分离。
 */
@Service
@RequiredArgsConstructor
public class SpecificCrowdDiscountService {

	private static final int PAGE_SIZE = 20;
	private static final long STATUS_PUBLISHED = 2L;
	private static final long STATUS_EXPIRED = 4L;
	private static final int CYCLE_TYPE_FIXED_RANGE = 2;
	private static final int CYCLE_TYPE_NATURAL_MONTH = 1;

	private final SpecificCrowdDiscountMapper specificCrowdDiscountMapper;

	/**
	 * 将「指定时段、已发布、结束时间不晚于当日零点」的活动批量置为已过期，分页更新。
	 *
	 * @return 本轮 {@code UPDATE} 影响行数之和
	 */
	public int scheduleExpiredPromotion() {
		ZoneId zoneId = ZoneId.systemDefault();
		long dayStartEpoch = LocalDate.now(zoneId).atStartOfDay(zoneId).toEpochSecond();

		QueryWrapper<SpecificCrowdDiscount> base = new QueryWrapper<>();
		base.eq("cycle_type", CYCLE_TYPE_FIXED_RANGE)
				.eq("status", STATUS_PUBLISHED)
				.le("end_time", dayStartEpoch);

		long totalCount = specificCrowdDiscountMapper.selectCount(base);
		if (totalCount == 0L) {
			return 0;
		}
		int totalPage = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
		int updatedTotal = 0;
		for (int pageNum = 1; pageNum <= totalPage; pageNum++) {
			Page<SpecificCrowdDiscount> page = new Page<>(pageNum, PAGE_SIZE, false);
			Page<SpecificCrowdDiscount> result = specificCrowdDiscountMapper.selectPage(page, base);
			List<Long> ids =
					result.getRecords().stream()
							.map(SpecificCrowdDiscount::getId)
							.filter(id -> id != null)
							.toList();
			if (ids.isEmpty()) {
				continue;
			}
			UpdateWrapper<SpecificCrowdDiscount> uw = new UpdateWrapper<>();
			uw.in("id", ids).set("status", STATUS_EXPIRED);
			updatedTotal += specificCrowdDiscountMapper.update(null, uw);
		}
		return updatedTotal;
	}

	/**
	 * 自然月周期、已发布且结束时间不晚于当日零点的活动：若行内 {@code end_time} 早于本月初，则将
	 * {@code start_time}/{@code end_time} 刷新为当前自然月 1 日 0 点～当月末 23:59:59；否则跳过该行。分页
	 * 20 条/页，逐行单条 {@code UPDATE}，无整段方法级事务。
	 *
	 * @return 上述 {@code UPDATE} 影响行数累加
	 */
	public int scheduleExpiredPromotionMonth() {
		ZoneId zoneId = ZoneId.systemDefault();
		long dayStartEpoch = LocalDate.now(zoneId).atStartOfDay(zoneId).toEpochSecond();
		YearMonth ym = YearMonth.now(zoneId);
		long monthStartEpoch = ym.atDay(1).atStartOfDay(zoneId).toEpochSecond();
		long monthEndEpoch = ym.atEndOfMonth().atTime(23, 59, 59).atZone(zoneId).toEpochSecond();

		QueryWrapper<SpecificCrowdDiscount> countWrapper = new QueryWrapper<>();
		applyScheduleExpiredMonthFilter(countWrapper, dayStartEpoch);
		long totalCount = specificCrowdDiscountMapper.selectCount(countWrapper);
		if (totalCount == 0L) {
			return 0;
		}
		int totalPage = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
		int updatedTotal = 0;
		for (int pageNum = 1; pageNum <= totalPage; pageNum++) {
			QueryWrapper<SpecificCrowdDiscount> pageWrapper = new QueryWrapper<>();
			applyScheduleExpiredMonthFilter(pageWrapper, dayStartEpoch);
			pageWrapper.select("id", "status", "end_time");
			Page<SpecificCrowdDiscount> page = new Page<>(pageNum, PAGE_SIZE, false);
			Page<SpecificCrowdDiscount> result = specificCrowdDiscountMapper.selectPage(page, pageWrapper);
			for (SpecificCrowdDiscount row : result.getRecords()) {
				Long end = row.getEndTime();
				if (end == null) {
					continue;
				}
				if (end < monthStartEpoch) {
					UpdateWrapper<SpecificCrowdDiscount> uw = new UpdateWrapper<>();
					uw.eq("id", row.getId())
							.set("start_time", monthStartEpoch)
							.set("end_time", monthEndEpoch);
					updatedTotal += specificCrowdDiscountMapper.update(null, uw);
				}
			}
		}
		return updatedTotal;
	}

	private void applyScheduleExpiredMonthFilter(
			QueryWrapper<SpecificCrowdDiscount> w, long dayStartEpoch) {
		w.eq("cycle_type", CYCLE_TYPE_NATURAL_MONTH)
				.eq("status", STATUS_PUBLISHED)
				.le("end_time", dayStartEpoch);
	}
}
