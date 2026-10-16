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
import cn.shopex.ecshopx.common.exception.ConflictException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTask;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskDisplayStatus;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskPurchaseTypes;
import cn.shopex.ecshopx.promotions.domain.membertask.MemberTaskTypeCatalog;
import cn.shopex.ecshopx.promotions.mapper.MemberTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 会员任务新建/编辑草稿（ECX-10422 保存壳；类型专属校验器可 stub）。 */
@Service
public class MemberTaskSaveService {

	private static final int NAME_MAX = 100;

	private final MemberTaskMapper memberTaskMapper;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;

	public MemberTaskSaveService(
			MemberTaskMapper memberTaskMapper, MessageSource messageSource, ObjectMapper objectMapper) {
		this.memberTaskMapper = memberTaskMapper;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public Map<String, Object> create(long companyId, Map<String, Object> body, long nowEpochSec, Locale locale) {
		ValidatedPayload payload = validateAndBuild(body, nowEpochSec, locale, true, null);
		int nowSec = (int) Math.min(nowEpochSec, Integer.MAX_VALUE);
		MemberTask row = new MemberTask();
		row.setCompanyId(companyId);
		applyPayload(row, payload);
		row.setPublished(0);
		row.setOffShelf(0);
		row.setConfigVersion(1L);
		row.setCreated(nowSec);
		row.setUpdated(nowSec);
		memberTaskMapper.insert(row);
		return toSaveResponse(row, nowEpochSec);
	}

	@Transactional
	public Map<String, Object> update(
			long companyId, long taskId, Map<String, Object> body, long nowEpochSec, Locale locale) {
		MemberTask existing =
				memberTaskMapper.selectOne(
						new LambdaQueryWrapper<MemberTask>()
								.eq(MemberTask::getTaskId, taskId)
								.eq(MemberTask::getCompanyId, companyId));
		if (existing == null) {
			throw new ResourceException(
					msg("promotions.member_task.task_not_found", "任务不存在", locale), 404);
		}
		int published = existing.getPublished() != null ? existing.getPublished() : 0;
		int offShelf = existing.getOffShelf() != null ? existing.getOffShelf() : 0;
		int begin = existing.getBeginTime() != null ? existing.getBeginTime() : 0;
		int end = existing.getEndTime() != null ? existing.getEndTime() : 0;
		String display =
				MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, nowEpochSec);
		if (!MemberTaskDisplayStatus.DRAFT.equals(display)) {
			throw new BadRequestException(
					msg("promotions.member_task.edit_not_draft", "仅待发布状态可编辑", locale));
		}

		long clientVersion = parseRequiredConfigVersion(body.get("config_version"), locale);
		long dbVersion = existing.getConfigVersion() == null ? 1L : existing.getConfigVersion();
		if (clientVersion != dbVersion) {
			throw new ConflictException(
					msg(
							"promotions.member_task.config_version_conflict",
							"任务配置已被他人修改，请刷新后重试",
							locale));
		}

		ValidatedPayload payload = validateAndBuild(body, nowEpochSec, locale, false, existing);
		int nowSec = (int) Math.min(nowEpochSec, Integer.MAX_VALUE);
		LambdaUpdateWrapper<MemberTask> uw =
				new LambdaUpdateWrapper<MemberTask>()
						.eq(MemberTask::getTaskId, taskId)
						.eq(MemberTask::getCompanyId, companyId)
						.eq(MemberTask::getConfigVersion, dbVersion)
						.set(MemberTask::getName, payload.name)
						.set(MemberTask::getBeginTime, payload.beginTime)
						.set(MemberTask::getEndTime, payload.endTime)
						.set(MemberTask::getFrequencyType, payload.frequencyType)
						.set(MemberTask::getFrequencyPeriod, payload.frequencyPeriod)
						.set(MemberTask::getAudienceMode, payload.audienceMode)
						.set(MemberTask::getAudienceTagIds, payload.audienceTagIdsJson)
						.set(MemberTask::getAudienceTagCombine, payload.audienceTagCombine)
						.set(MemberTask::getTaskRule, payload.taskRule)
						.set(MemberTask::getRewardConfig, payload.rewardConfigJson)
						.set(MemberTask::getRuleConfig, payload.ruleConfigJson)
						.set(MemberTask::getJumpPage, payload.jumpPage)
						.set(MemberTask::getUpdated, nowSec)
						.set(MemberTask::getConfigVersion, dbVersion + 1L);
		int rows = memberTaskMapper.update(null, uw);
		if (rows == 0) {
			throw new ConflictException(
					msg(
							"promotions.member_task.config_version_conflict",
							"任务配置已被他人修改，请刷新后重试",
							locale));
		}
		existing.setName(payload.name);
		existing.setBeginTime(payload.beginTime);
		existing.setEndTime(payload.endTime);
		existing.setFrequencyType(payload.frequencyType);
		existing.setFrequencyPeriod(payload.frequencyPeriod);
		existing.setAudienceMode(payload.audienceMode);
		existing.setAudienceTagIds(payload.audienceTagIdsJson);
		existing.setAudienceTagCombine(payload.audienceTagCombine);
		existing.setTaskRule(payload.taskRule);
		existing.setRewardConfig(payload.rewardConfigJson);
		existing.setRuleConfig(payload.ruleConfigJson);
		existing.setJumpPage(payload.jumpPage);
		existing.setConfigVersion(dbVersion + 1L);
		existing.setUpdated(nowSec);
		return toSaveResponse(existing, nowEpochSec);
	}

	private ValidatedPayload validateAndBuild(
			Map<String, Object> body,
			long nowEpochSec,
			Locale locale,
			boolean creating,
			MemberTask existing) {
		if (body == null) {
			body = Map.of();
		}
		String name = asTrimmedString(body.get("name"));
		if (!StringUtils.hasText(name)) {
			throw new BadRequestException(
					msg("promotions.member_task.task_name_required", "请填写任务名称", locale));
		}
		if (name.length() > NAME_MAX) {
			throw new BadRequestException(
					msg("promotions.member_task.task_name_too_long", "任务名称不能超过100字", locale));
		}

		Integer beginTime = asEpoch(body.get("begin_time"));
		Integer endTime = asEpoch(body.get("end_time"));
		if (beginTime == null || endTime == null || beginTime <= 0 || endTime <= 0) {
			throw new BadRequestException(
					msg("promotions.member_task.task_time_required", "请填写任务开始和结束时间", locale));
		}
		if (beginTime >= endTime) {
			throw new BadRequestException(
					msg("promotions.member_task.task_time_invalid", "结束时间必须晚于开始时间", locale));
		}
		if (endTime <= nowEpochSec) {
			throw new BadRequestException(
					msg("promotions.member_task.task_end_must_be_future", "结束时间必须晚于当前时间", locale));
		}

		String typeCategory = asTrimmedString(body.get("type_category"));
		String typeDetail = asTrimmedString(body.get("type_detail"));
		if (!creating && existing != null) {
			if (!safeEq(existing.getTypeCategory(), typeCategory)
					|| !safeEq(existing.getTypeDetail(), typeDetail)) {
				throw new BadRequestException(
						msg("promotions.member_task.type_immutable", "任务类型不可修改", locale));
			}
			typeCategory = existing.getTypeCategory();
			typeDetail = existing.getTypeDetail();
		} else if (!MemberTaskTypeCatalog.isKnownPair(typeCategory, typeDetail)) {
			throw new BadRequestException(
					msg("promotions.member_task.type_detail_invalid", "任务类型无效", locale));
		}

		String frequencyType = asTrimmedString(body.get("frequency_type"));
		String frequencyPeriod = asTrimmedString(body.get("frequency_period"));
		if (!"once".equals(frequencyType) && !"cycle".equals(frequencyType)) {
			throw new BadRequestException(
					msg("promotions.member_task.frequency_invalid", "任务频次配置无效", locale));
		}
		if ("cycle".equals(frequencyType)) {
			if (!"daily".equals(frequencyPeriod)
					&& !"weekly".equals(frequencyPeriod)
					&& !"monthly".equals(frequencyPeriod)) {
				throw new BadRequestException(
						msg("promotions.member_task.frequency_invalid", "任务频次配置无效", locale));
			}
		} else {
			frequencyPeriod = null;
		}

		String audienceMode = asTrimmedString(body.get("audience_mode"));
		String audienceTagCombine = asTrimmedString(body.get("audience_tag_combine"));
		String audienceTagIdsJson = null;
		if (!"unlimited".equals(audienceMode)
				&& !"visible".equals(audienceMode)
				&& !"hidden".equals(audienceMode)) {
			throw new BadRequestException(
					msg("promotions.member_task.audience_tags_required", "请配置可见人群标签", locale));
		}
		if ("visible".equals(audienceMode) || "hidden".equals(audienceMode)) {
			List<Long> tagIds = parseTagIds(body.get("audience_tag_ids"), locale);
			if (tagIds.isEmpty()) {
				throw new BadRequestException(
						msg("promotions.member_task.audience_tags_required", "请配置可见人群标签", locale));
			}
			if (!"intersection".equals(audienceTagCombine) && !"union".equals(audienceTagCombine)) {
				throw new BadRequestException(
						msg("promotions.member_task.audience_tags_required", "请配置可见人群标签", locale));
			}
			audienceTagIdsJson = writeJson(tagIds, locale);
		} else {
			audienceTagCombine = null;
		}

		String taskRule = asTrimmedString(body.get("task_rule"));
		if (!StringUtils.hasText(taskRule)) {
			throw new BadRequestException(
					msg("promotions.member_task.task_rule_required", "请填写任务规则说明", locale));
		}

		String rewardConfigJson = normalizeRewardConfig(body.get("reward_config"), locale);
		String ruleConfigJson = normalizeRuleConfigStub(body.get("rule_config"), typeDetail, locale);
		String jumpPage = asTrimmedString(body.get("jump_page"));
		if (!StringUtils.hasText(jumpPage)) {
			jumpPage = null;
		}

		return new ValidatedPayload(
				name,
				typeCategory,
				typeDetail,
				beginTime,
				endTime,
				frequencyType,
				frequencyPeriod,
				audienceMode,
				audienceTagIdsJson,
				audienceTagCombine,
				taskRule,
				rewardConfigJson,
				ruleConfigJson,
				jumpPage);
	}

	private void applyPayload(MemberTask row, ValidatedPayload payload) {
		row.setName(payload.name);
		row.setTypeCategory(payload.typeCategory);
		row.setTypeDetail(payload.typeDetail);
		row.setBeginTime(payload.beginTime);
		row.setEndTime(payload.endTime);
		row.setFrequencyType(payload.frequencyType);
		row.setFrequencyPeriod(payload.frequencyPeriod);
		row.setAudienceMode(payload.audienceMode);
		row.setAudienceTagIds(payload.audienceTagIdsJson);
		row.setAudienceTagCombine(payload.audienceTagCombine);
		row.setTaskRule(payload.taskRule);
		row.setRewardConfig(payload.rewardConfigJson);
		row.setRuleConfig(payload.ruleConfigJson);
		row.setJumpPage(payload.jumpPage);
	}

	private Map<String, Object> toSaveResponse(MemberTask row, long nowEpochSec) {
		int published = row.getPublished() != null ? row.getPublished() : 0;
		int offShelf = row.getOffShelf() != null ? row.getOffShelf() : 0;
		int begin = row.getBeginTime() != null ? row.getBeginTime() : 0;
		int end = row.getEndTime() != null ? row.getEndTime() : 0;
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("task_id", row.getTaskId());
		out.put("config_version", row.getConfigVersion() == null ? 1L : row.getConfigVersion());
		out.put(
				"display_status",
				MemberTaskDisplayStatus.resolve(published, offShelf, begin, end, nowEpochSec));
		out.put("name", row.getName());
		out.put("type_category", row.getTypeCategory());
		out.put("type_detail", row.getTypeDetail());
		out.put("begin_time", begin);
		out.put("end_time", end);
		return out;
	}

	private String normalizeRewardConfig(Object raw, Locale locale) {
		JsonNode node = toJsonNode(raw, locale, "promotions.member_task.reward_config_invalid", "请至少配置一种奖励");
		if (node == null || !node.isObject()) {
			throw new BadRequestException(
					msg("promotions.member_task.reward_config_invalid", "请至少配置一种奖励", locale));
		}
		boolean hasPoints = false;
		JsonNode points = node.get("points");
		if (points != null && !points.isNull()) {
			if (!points.isNumber() || points.asDouble() <= 0) {
				throw new BadRequestException(
						msg("promotions.member_task.reward_config_invalid", "请至少配置一种奖励", locale));
			}
			hasPoints = true;
		}
		boolean hasCoupon = false;
		JsonNode coupon = node.get("coupon_id");
		if (coupon != null && !coupon.isNull()) {
			if (!coupon.isNumber() || coupon.asLong() <= 0L) {
				throw new BadRequestException(
						msg("promotions.member_task.reward_config_invalid", "请至少配置一种奖励", locale));
			}
			hasCoupon = true;
		}
		if (!hasPoints && !hasCoupon) {
			throw new BadRequestException(
					msg("promotions.member_task.reward_config_invalid", "请至少配置一种奖励", locale));
		}
		return writeJson(node, locale);
	}

	private String normalizeRuleConfigStub(Object raw, String typeDetail, Locale locale) {
		boolean purchaseRule = MemberTaskPurchaseTypes.usesPurchaseRule(typeDetail);
		if (raw == null) {
			if (purchaseRule) {
				throw new BadRequestException(
						msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
			}
			return "{}";
		}
		if (raw instanceof String s && !StringUtils.hasText(s)) {
			if (purchaseRule) {
				throw new BadRequestException(
						msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
			}
			return "{}";
		}
		JsonNode node =
				toJsonNode(raw, locale, "promotions.member_task.rule_config_invalid", "任务规则配置无效");
		if (node == null || !node.isObject()) {
			throw new BadRequestException(
					msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
		}
		if (purchaseRule) {
			String scope = node.path("goods_scope").asText("");
			if (!"unlimited".equals(scope) && !"items".equals(scope) && !"category".equals(scope)) {
				throw new BadRequestException(
						msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
			}
			if (!node.has("need_receipt")
					|| (!node.get("need_receipt").isBoolean() && !node.get("need_receipt").isNumber())) {
				throw new BadRequestException(
						msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
			}
			if (node.has("amount_fen")
					&& !node.get("amount_fen").isNull()
					&& (!node.get("amount_fen").isNumber() || node.get("amount_fen").asLong() < 0L)) {
				throw new BadRequestException(
						msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
			}
			if ("items".equals(scope)) {
				requireNonEmptyIdArray(node.get("item_ids"), locale);
			} else if ("category".equals(scope)) {
				requireNonEmptyIdArray(node.get("category_ids"), locale);
			}
			validateOptionalIdArray(node.get("item_ids"), locale);
			validateOptionalIdArray(node.get("category_ids"), locale);
			validateOptionalIdArray(node.get("distributor_ids"), locale);
		}
		if ("profile".equals(typeDetail) || "sign_in".equals(typeDetail)) {
			if (node.size() > 0) {
				throw new BadRequestException(
						msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
			}
			return "{}";
		}
		if ("share_order".equals(typeDetail) || "invite_register".equals(typeDetail)) {
			requireNonBlankText(node, "share_cover", locale);
			requireNonBlankText(node, "share_title", locale);
			requireNonBlankText(node, "share_jump_page", locale);
		}
		if ("browse".equals(typeDetail)) {
			requireNonBlankText(node, "page", locale);
			if (node.has("stay_seconds")
					&& !node.get("stay_seconds").isNull()
					&& (!node.get("stay_seconds").isNumber() || node.get("stay_seconds").asInt() < 0)) {
				throw new BadRequestException(
						msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
			}
		}
		if ("favorite".equals(typeDetail)) {
			validateOptionalIdArray(node.get("item_ids"), locale);
			validateOptionalIdArray(node.get("distributor_ids"), locale);
		}
		return writeJson(node, locale);
	}

	private JsonNode toJsonNode(Object raw, Locale locale, String key, String defaultZh) {
		try {
			if (raw == null) {
				return null;
			}
			if (raw instanceof String s) {
				return objectMapper.readTree(s);
			}
			return objectMapper.valueToTree(raw);
		} catch (Exception e) {
			throw new BadRequestException(msg(key, defaultZh, locale));
		}
	}

	private String writeJson(Object value, Locale locale) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new BadRequestException(
					msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
		}
	}

	private List<Long> parseTagIds(Object raw, Locale locale) {
		List<Long> out = new ArrayList<>();
		if (raw == null) {
			return out;
		}
		JsonNode node;
		try {
			if (raw instanceof String s) {
				if (!StringUtils.hasText(s)) {
					return out;
				}
				node = objectMapper.readTree(s);
			} else {
				node = objectMapper.valueToTree(raw);
			}
		} catch (Exception e) {
			throw new BadRequestException(
					msg("promotions.member_task.audience_tags_required", "请配置可见人群标签", locale));
		}
		if (node == null || !node.isArray()) {
			throw new BadRequestException(
					msg("promotions.member_task.audience_tags_required", "请配置可见人群标签", locale));
		}
		for (JsonNode n : node) {
			if (n != null && n.isNumber() && n.asLong() > 0L) {
				out.add(n.asLong());
			}
		}
		return out;
	}

	private long parseRequiredConfigVersion(Object raw, Locale locale) {
		if (raw == null) {
			throw new ConflictException(
					msg(
							"promotions.member_task.config_version_conflict",
							"任务配置已被他人修改，请刷新后重试",
							locale));
		}
		try {
			long v = raw instanceof Number n ? n.longValue() : Long.parseLong(raw.toString().trim());
			if (v <= 0L) {
				throw new ConflictException(
						msg(
								"promotions.member_task.config_version_conflict",
								"任务配置已被他人修改，请刷新后重试",
								locale));
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ConflictException(
					msg(
							"promotions.member_task.config_version_conflict",
							"任务配置已被他人修改，请刷新后重试",
							locale));
		}
	}

	private static Integer asEpoch(Object raw) {
		if (raw == null) {
			return null;
		}
		try {
			long v = raw instanceof Number n ? n.longValue() : Long.parseLong(raw.toString().trim());
			if (v <= 0L || v > Integer.MAX_VALUE) {
				return null;
			}
			return (int) v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String asTrimmedString(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.toString().trim();
		return s.isEmpty() ? null : s;
	}

	private static boolean safeEq(String a, String b) {
		if (a == null) {
			return b == null;
		}
		return a.equals(b);
	}

	private void requireNonEmptyIdArray(JsonNode arr, Locale locale) {
		if (arr == null || arr.isNull() || !arr.isArray() || arr.isEmpty()) {
			throw new BadRequestException(
					msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
		}
		for (JsonNode n : arr) {
			if (n == null || !n.isNumber() || n.asLong() <= 0L) {
				throw new BadRequestException(
						msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
			}
		}
	}

	private void validateOptionalIdArray(JsonNode arr, Locale locale) {
		if (arr == null || arr.isNull()) {
			return;
		}
		if (!arr.isArray()) {
			throw new BadRequestException(
					msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
		}
		for (JsonNode n : arr) {
			if (n == null || !n.isNumber() || n.asLong() <= 0L) {
				throw new BadRequestException(
						msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
			}
		}
	}

	private void requireNonBlankText(JsonNode node, String field, Locale locale) {
		JsonNode v = node.get(field);
		if (v == null || v.isNull() || !StringUtils.hasText(v.asText())) {
			throw new BadRequestException(
					msg("promotions.member_task.rule_config_invalid", "任务规则配置无效", locale));
		}
	}

	private String msg(String key, String defaultZh, Locale locale) {
		return messageSource.getMessage(key, null, defaultZh, locale);
	}

	private record ValidatedPayload(
			String name,
			String typeCategory,
			String typeDetail,
			int beginTime,
			int endTime,
			String frequencyType,
			String frequencyPeriod,
			String audienceMode,
			String audienceTagIdsJson,
			String audienceTagCombine,
			String taskRule,
			String rewardConfigJson,
			String ruleConfigJson,
			String jumpPage) {}
}
