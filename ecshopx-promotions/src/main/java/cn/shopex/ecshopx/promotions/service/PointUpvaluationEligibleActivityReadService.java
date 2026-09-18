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

import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.PointUpvaluation;
import cn.shopex.ecshopx.promotions.mapper.PointUpvaluationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointUpvaluationEligibleActivityReadService {

	private final PointUpvaluationMapper pointUpvaluationMapper;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final MemberAccountService memberAccountService;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public PointUpvaluationEligibleActivityReadService(
			PointUpvaluationMapper pointUpvaluationMapper,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			MemberAccountService memberAccountService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.pointUpvaluationMapper = pointUpvaluationMapper;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.memberAccountService = memberAccountService;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Optional<Map<String, Object>> getEligibleActivity(long companyId, long userId, String usedScene) {
		if (userId <= 0L) {
			return Optional.empty();
		}
		long nowSec = System.currentTimeMillis() / 1000L;
		List<PointUpvaluation> candidates =
				pointUpvaluationMapper.selectList(
						new LambdaQueryWrapper<PointUpvaluation>()
								.eq(PointUpvaluation::getCompanyId, companyId)
								.le(PointUpvaluation::getBeginTime, nowSec)
								.ge(PointUpvaluation::getEndTime, nowSec)
								.orderByDesc(PointUpvaluation::getUpvaluation)
								.orderByDesc(PointUpvaluation::getCreated));
		if (candidates.isEmpty()) {
			return Optional.empty();
		}
		MemberGradeTokens grade = resolveMemberGrade(userId, companyId);
		List<Long> eligibleIds = new ArrayList<>();
		for (PointUpvaluation row : candidates) {
			if (!gradeMatches(row, grade)) {
				continue;
			}
			if (!sceneMatches(row, usedScene)) {
				continue;
			}
			if (!eligibleTriggerDate(row.getTriggerCondition(), nowSec)) {
				continue;
			}
			eligibleIds.add(row.getActivityId());
		}
		if (eligibleIds.isEmpty()) {
			return Optional.empty();
		}
		PointUpvaluation best =
				pointUpvaluationMapper.selectOne(
						new LambdaQueryWrapper<PointUpvaluation>()
								.eq(PointUpvaluation::getCompanyId, companyId)
								.in(PointUpvaluation::getActivityId, eligibleIds)
								.orderByDesc(PointUpvaluation::getUpvaluation)
								.orderByDesc(PointUpvaluation::getCreated)
								.last("LIMIT 1"));
		if (best == null || best.getUpvaluation() == null || best.getUpvaluation() <= 1) {
			return Optional.empty();
		}
		long uppoints = getDailyPoints(companyId, userId);
		long minUppoints = uppoints + (best.getUpvaluation() - 1L);
		long dailyCap = (long) best.getMaxUpPoint() * (best.getUpvaluation() - 1L);
		if (minUppoints > dailyCap) {
			return Optional.empty();
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("activity_id", best.getActivityId());
		result.put("upvaluation", best.getUpvaluation());
		result.put("max_up_point", best.getMaxUpPoint());
		result.put("uppoints", uppoints);
		return Optional.of(result);
	}

	private MemberGradeTokens resolveMemberGrade(long userId, long companyId) {
		Map<String, Object> vip = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		if (Boolean.TRUE.equals(vip.get("valid"))
				&& Boolean.TRUE.equals(vip.get("is_vip"))
				&& vip.get("vip_type") != null
				&& StringUtils.hasText(String.valueOf(vip.get("vip_type")).trim())) {
			Object gid = vip.get("vip_grade_id");
			long id = gid instanceof Number n ? n.longValue() : parseLong(String.valueOf(gid));
			return new MemberGradeTokens(id, String.valueOf(vip.get("vip_type")).trim());
		}
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		Object gradeId = member.get("grade_id");
		long id = gradeId instanceof Number n ? n.longValue() : parseLong(String.valueOf(gradeId));
		return new MemberGradeTokens(id, "normal");
	}

	private boolean gradeMatches(PointUpvaluation row, MemberGradeTokens grade) {
		List<String> valid = parseStringList(row.getValidGrade());
		if (valid.isEmpty()) {
			return false;
		}
		String idToken = grade.id() > 0L ? String.valueOf(grade.id()) : "";
		return valid.contains(idToken) || valid.contains(grade.lvType());
	}

	private boolean sceneMatches(PointUpvaluation row, String usedScene) {
		List<String> scenes = parseStringList(row.getUsedScene());
		return scenes.contains(usedScene);
	}

	private boolean eligibleTriggerDate(String triggerConditionJson, long nowSec) {
		Map<String, Object> triggerCondition = parseJsonMap(triggerConditionJson);
		Object ttObj = triggerCondition.get("trigger_time");
		if (!(ttObj instanceof Map<?, ?> ttRaw)) {
			return false;
		}
		Map<String, Object> triggerTime = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ttRaw.entrySet()) {
			if (e.getKey() != null) {
				triggerTime.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		String type = String.valueOf(triggerTime.getOrDefault("type", "")).trim();
		LocalDate today = LocalDate.now();
		return switch (type) {
			case "every_year" -> intVal(triggerTime.get("month")) == today.getMonthValue()
					&& intVal(triggerTime.get("day")) == today.getDayOfMonth();
			case "every_month" -> intVal(triggerTime.get("day")) == today.getDayOfMonth();
			case "every_week" -> intVal(triggerTime.get("week")) == today.getDayOfWeek().getValue();
			case "date" -> nowSec > longVal(triggerTime.get("begin_time"))
					&& nowSec < longVal(triggerTime.get("end_time"));
			default -> false;
		};
	}

	private long getDailyPoints(long companyId, long userId) {
		String date = LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
		String key = "uppoints:" + companyId + "_" + userId;
		String pointsRaw = stringRedisTemplate.<String, String>opsForHash().get(key, date);
		delUserDailyPointsIfNeeded(key, date);
		if (!StringUtils.hasText(pointsRaw)) {
			return 0L;
		}
		try {
			return Long.parseLong(pointsRaw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private void delUserDailyPointsIfNeeded(String key, String date) {
		Boolean exists = stringRedisTemplate.opsForHash().hasKey(key, date);
		if (Boolean.TRUE.equals(exists)) {
			return;
		}
		Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
		if (entries == null || entries.isEmpty()) {
			return;
		}
		stringRedisTemplate.opsForHash().delete(key, entries.keySet().toArray());
	}

	private List<String> parseStringList(String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			List<Object> raw = objectMapper.readValue(json, new TypeReference<List<Object>>() {});
			List<String> out = new ArrayList<>();
			for (Object o : raw) {
				if (o != null) {
					out.add(String.valueOf(o).trim());
				}
			}
			return out;
		} catch (Exception e) {
			return List.of();
		}
	}

	private Map<String, Object> parseJsonMap(String json) {
		if (!StringUtils.hasText(json)) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return Map.of();
		}
	}

	private static int intVal(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseLong(String s) {
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private record MemberGradeTokens(long id, String lvType) {}
}
