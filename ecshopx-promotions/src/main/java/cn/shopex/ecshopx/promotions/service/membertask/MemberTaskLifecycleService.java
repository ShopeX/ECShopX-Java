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

package cn.shopex.ecshopx.promotions.service.membertask;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskDisplayStatus;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskOperationPolicy;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会员任务发布 / 下架（ECX-10420）。 */
@Service
public class MemberTaskLifecycleService {

	private final MemberTaskMapper memberTaskMapper;
	private final MessageSource messageSource;

	public MemberTaskLifecycleService(MemberTaskMapper memberTaskMapper, MessageSource messageSource) {
		this.memberTaskMapper = memberTaskMapper;
		this.messageSource = messageSource;
	}

	@Transactional
	public Map<String, Object> publish(long companyId, long taskId, long nowEpochSec, Locale locale) {
		MemberTask row = load(companyId, taskId, locale);
		String display = resolveDisplay(row, nowEpochSec);
		int end = row.getEndTime() != null ? row.getEndTime() : 0;
		if (!MemberTaskDisplayStatus.DRAFT.equals(display)) {
			if (MemberTaskDisplayStatus.EXPIRED.equals(display)) {
				throw new BadRequestException(
						msg("promotions.member_task.publish_expired", "任务已过期，无法发布", locale));
			}
			throw new BadRequestException(
					msg("promotions.member_task.publish_not_draft", "仅待发布状态可发布", locale));
		}
		if (!MemberTaskOperationPolicy.canPublish(display, end, nowEpochSec)) {
			throw new BadRequestException(
					msg("promotions.member_task.publish_expired", "任务已过期，无法发布", locale));
		}
		int nowSec = (int) Math.min(nowEpochSec, Integer.MAX_VALUE);
		int rows =
				memberTaskMapper.update(
						null,
						new UpdateWrapper<MemberTask>()
								.eq("task_id", taskId)
								.eq("company_id", companyId)
								.eq("published", 0)
								.eq("off_shelf", 0)
								.set("published", 1)
								.set("updated", nowSec));
		if (rows == 0) {
			throw new BadRequestException(
					msg("promotions.member_task.publish_not_draft", "仅待发布状态可发布", locale));
		}
		row.setPublished(1);
		row.setUpdated(nowSec);
		return snapshot(row, nowEpochSec);
	}

	@Transactional
	public Map<String, Object> offShelf(long companyId, long taskId, long nowEpochSec, Locale locale) {
		MemberTask row = load(companyId, taskId, locale);
		String display = resolveDisplay(row, nowEpochSec);
		if (!MemberTaskOperationPolicy.canOffShelf(display)) {
			throw new BadRequestException(
					msg("promotions.member_task.off_shelf_not_allowed", "当前状态不可下架", locale));
		}
		int nowSec = (int) Math.min(nowEpochSec, Integer.MAX_VALUE);
		int rows =
				memberTaskMapper.update(
						null,
						new UpdateWrapper<MemberTask>()
								.eq("task_id", taskId)
								.eq("company_id", companyId)
								.eq("published", 1)
								.eq("off_shelf", 0)
								.set("off_shelf", 1)
								.set("updated", nowSec));
		if (rows == 0) {
			throw new BadRequestException(
					msg("promotions.member_task.off_shelf_not_allowed", "当前状态不可下架", locale));
		}
		row.setOffShelf(1);
		row.setUpdated(nowSec);
		return snapshot(row, nowEpochSec);
	}

	private MemberTask load(long companyId, long taskId, Locale locale) {
		MemberTask row =
				memberTaskMapper.selectOne(
						new QueryWrapper<MemberTask>()
								.eq("task_id", taskId)
								.eq("company_id", companyId));
		if (row == null) {
			throw new ResourceException(
					msg("promotions.member_task.task_not_found", "任务不存在", locale), 404);
		}
		return row;
	}

	private static String resolveDisplay(MemberTask row, long nowEpochSec) {
		int published = row.getPublished() != null ? row.getPublished() : 0;
		int offShelf = row.getOffShelf() != null ? row.getOffShelf() : 0;
		int begin = row.getBeginTime() != null ? row.getBeginTime() : 0;
		int end = row.getEndTime() != null ? row.getEndTime() : 0;
		return MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, nowEpochSec);
	}

	private static Map<String, Object> snapshot(MemberTask row, long nowEpochSec) {
		int published = row.getPublished() != null ? row.getPublished() : 0;
		int offShelf = row.getOffShelf() != null ? row.getOffShelf() : 0;
		int begin = row.getBeginTime() != null ? row.getBeginTime() : 0;
		int end = row.getEndTime() != null ? row.getEndTime() : 0;
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("task_id", row.getTaskId());
		out.put("display_status", MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, nowEpochSec));
		out.put("published", published);
		out.put("off_shelf", offShelf);
		out.put("begin_time", begin > 0 ? begin : null);
		out.put("end_time", end > 0 ? end : null);
		return out;
	}

	private String msg(String key, String defaultZh, Locale locale) {
		return messageSource.getMessage(key, null, defaultZh, locale);
	}
}
