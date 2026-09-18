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

import cn.shopex.ecshopx.promotions.domain.PromotionActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionActivityMapper;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityMessage;
import cn.shopex.ecshopx.promotions.schedule.ScheduleFirePromotionActivityEnqueuePort;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionActivityMultiLangReadService;
import cn.shopex.ecshopx.promotions.service.schedule.MembershipSchedulePromotionActivitySupport;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 自动化营销活动：定时将到期活动置为无效、按计划触发可发放任务等与 HTTP 创建/编辑职责分离。
 */
@Service
@RequiredArgsConstructor
public class PromotionActivityService {

	private static final int SCHEDULE_LIST_PAGE = 50;
	private static final int FIRE_PAGE_SIZE = 100;

	private static final String[] ACTIVITY_TYPE_ORDER = {
		"member_birthday",
		"member_anniversary",
		"member_day",
		"member_upgrade",
		"member_vip_upgrade"
	};

	private final PromotionActivityMapper promotionActivityMapper;
	private final PromotionActivityCreateService promotionActivityCreateService;
	private final PromotionActivityMultiLangReadService promotionActivityMultiLangReadService;
	private final ScheduleFirePromotionActivityEnqueuePort scheduleFirePromotionActivityEnqueuePort;
	private final MembershipSchedulePromotionActivitySupport membershipSchedulePromotionActivitySupport;

	/**
	 * 将已结束且仍为「有效」状态的活动批量置为无效。
	 *
	 * <p>与 PHP {@code PromotionActivityRepository::updateBy} 行为对齐：匹配结果为空时（Doctrine 空 {@code ArrayCollection} 在
	 * {@code if (!$entityList)} 中为假）不抛 {@code ResourceException}，不执行 persist，返回 0。
	 *
	 * @return 本次 {@code UPDATE} 影响行数
	 */
	public int activityInvalid() {
		long now = Instant.now().getEpochSecond();
		QueryWrapper<PromotionActivity> q = new QueryWrapper<>();
		q.lt("end_time", now).eq("activity_status", "valid");
		long count = promotionActivityMapper.selectCount(q);
		if (count == 0L) {
			return 0;
		}
		UpdateWrapper<PromotionActivity> uw = new UpdateWrapper<>();
		uw.lt("end_time", now)
				.eq("activity_status", "valid")
				.set("activity_status", "invalid")
				.set("updated", (int) now);
		return promotionActivityMapper.update(null, uw);
	}

	/**
	 * 扫描有效的计划类活动并投递与 PHP {@code scheduleFire} 等价的异步分页任务。
	 *
	 * @return 全局投递条数（每页会员批一条）
	 */
	public long scheduleTrigger() {
		long triggerTime = Instant.now().getEpochSecond();
		long enqueued = 0L;
		for (String activityType : ACTIVITY_TYPE_ORDER) {
			if ("member_upgrade".equals(activityType) || "member_vip_upgrade".equals(activityType)) {
				continue;
			}
			QueryWrapper<PromotionActivity> w = new QueryWrapper<>();
			w.lt("begin_time", triggerTime)
					.ge("end_time", triggerTime)
					.eq("activity_status", "valid")
					.eq("activity_type", activityType)
					.orderByDesc("created");
			long totalCount = promotionActivityMapper.selectCount(w);
			if (totalCount == 0L) {
				continue;
			}
			long totalPage = (totalCount + (long) SCHEDULE_LIST_PAGE - 1L) / (long) SCHEDULE_LIST_PAGE;
			for (int i = 1; (long) i <= totalPage; i++) {
				Page<PromotionActivity> p = new Page<>(i, SCHEDULE_LIST_PAGE, false);
				Page<PromotionActivity> result = promotionActivityMapper.selectPage(p, w);
				List<Map<String, Object>> rows = new ArrayList<>();
				for (PromotionActivity e : result.getRecords()) {
					rows.add(promotionActivityCreateService.toActivityListRow(e));
				}
				promotionActivityMultiLangReadService.applyTitles(rows, "zh-CN");
				enqueued += scheduleTriggerFireToJob(activityType, rows, triggerTime);
			}
		}
		return enqueued;
	}

	private long scheduleTriggerFireToJob(String activityType, List<Map<String, Object>> list, long triggerTime) {
		long n = 0L;
		for (Map<String, Object> activityInfo : list) {
			if (!membershipSchedulePromotionActivitySupport.isTrigger(activityType, activityInfo)) {
				continue;
			}
			long totalMembers =
					membershipSchedulePromotionActivitySupport.countMembers(
							activityType, activityInfo, triggerTime);
			if (totalMembers == 0L) {
				continue;
			}
			long totalPage2 = (totalMembers + (long) FIRE_PAGE_SIZE - 1L) / (long) FIRE_PAGE_SIZE;
			for (int j = 1; (long) j <= totalPage2; j++) {
				ScheduleFirePromotionActivityMessage message =
						new ScheduleFirePromotionActivityMessage(
								activityType, snapshot(activityInfo), triggerTime, FIRE_PAGE_SIZE, j);
				n += scheduleFirePromotionActivityEnqueuePort.enqueue(message);
			}
		}
		return n;
	}

	private static Map<String, Object> snapshot(Map<String, Object> activityInfo) {
		return new LinkedHashMap<>(activityInfo);
	}
}
