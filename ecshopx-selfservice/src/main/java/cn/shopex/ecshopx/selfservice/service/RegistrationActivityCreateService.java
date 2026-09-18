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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.service.SmsTemplateUpdateService;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivityRelShop;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.RegistrationActivityCreateRequest;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityRelShopMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangWriteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RegistrationActivityCreateService {

	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationActivityRelShopMapper relShopMapper;
	private final RegistrationActivityOutsideMultiLangWriteService multiLangWriteService;
	private final SmsTemplateUpdateService smsTemplateUpdateService;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public RegistrationActivityCreateService(
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationActivityRelShopMapper relShopMapper,
			RegistrationActivityOutsideMultiLangWriteService multiLangWriteService,
			SmsTemplateUpdateService smsTemplateUpdateService,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.registrationActivityMapper = registrationActivityMapper;
		this.relShopMapper = relShopMapper;
		this.multiLangWriteService = multiLangWriteService;
		this.smsTemplateUpdateService = smsTemplateUpdateService;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createData(
			RegistrationActivityCreateRequest request, long companyId, String requestLangTag) {
		if (!StringUtils.hasText(request.getActivityName())) {
			throw new BadRequestException("活动名称必填");
		}
		int startSec = parseUnixSecondsRequired(request.getStartTimeRaw(), "开始时间必填", "开始时间格式无效");
		int endSec = parseUnixSecondsRequired(request.getEndTimeRaw(), "结束时间必填", "结束时间格式无效");

		long distributorIdParam = request.getDistributorId() == null ? 0L : request.getDistributorId();
		long tempIdParam = request.getTempId() == null ? 0L : request.getTempId();

		boolean isSms = registrationFieldTruthy(request.getIsSmsNotice());
		boolean isWxapp = registrationFieldTruthy(request.getIsWxappNotice());

		int giftPoints = parseIntCol(request.getGiftPoints(), "gift_points");
		int isAllowDuplicate = parseIntCol(request.getIsAllowDuplicate(), "is_allow_duplicate");
		int isAllowCancel = parseIntCol(request.getIsAllowCancel(), "is_allow_cancel");
		int isOfflineVerify = parseIntCol(request.getIsOfflineVerify(), "is_offline_verify");
		int isNeedCheck = parseIntCol(request.getIsNeedCheck(), "is_need_check");
		int isWhiteList = parseIntCol(request.getIsWhiteList(), "is_white_list");

		DistributorResolution dist = resolveDistributorResolution(request);

		Optional<String> areaOpt = normalizeStructuredField(request.getArea(), "area");
		Optional<String> showFieldsOpt = normalizeStructuredField(request.getShowFields(), "show_fields");
		Optional<String> picsOpt = normalizeStructuredField(request.getPics(), "pics");

		Optional<String> memberLevelOpt = toCommaSeparatedDbValue(request.getMemberLevel(), "member_level");
		Optional<String> enterpriseIdsOpt = toCommaSeparatedDbValue(request.getEnterpriseIds(), "enterprise_ids");

		String activityName = request.getActivityName().trim();
		int now = (int) (System.currentTimeMillis() / 1000L);

		RegistrationActivity entity = new RegistrationActivity();
		entity.setDistributorIds(null);
		entity.setCompanyId(companyId);
		entity.setActivityName(activityName);
		entity.setStartTime(startSec);
		entity.setEndTime(endSec);
		entity.setIsSmsNotice(isSms);
		entity.setIsWxappNotice(isWxapp);
		entity.setCreated(now);
		entity.setUpdated(now);
		entity.setTempId(tempIdParam);
		entity.setDistributorId(distributorIdParam);

		entity.setGiftPoints(giftPoints);
		entity.setIsAllowDuplicate(isAllowDuplicate);
		entity.setIsAllowCancel(isAllowCancel);
		entity.setIsOfflineVerify(isOfflineVerify);
		entity.setIsNeedCheck(isNeedCheck);
		entity.setIsWhiteList(isWhiteList);

		if (request.getJoinLimit() != null) {
			entity.setJoinLimit(request.getJoinLimit());
		}

		areaOpt.ifPresent(entity::setArea);
		showFieldsOpt.ifPresent(entity::setShowFields);
		picsOpt.ifPresent(entity::setPics);

		if (request.getPlace() != null) {
			entity.setPlace(request.getPlace());
		}
		if (request.getAddress() != null) {
			entity.setAddress(request.getAddress());
		}
		if (request.getIntro() != null) {
			entity.setIntro(request.getIntro());
		}
		if (request.getJoinTips() != null) {
			entity.setJoinTips(request.getJoinTips());
		}
		if (request.getSubmitFormTips() != null) {
			entity.setSubmitFormTips(request.getSubmitFormTips());
		}
		if (request.getContent() != null) {
			entity.setContent(request.getContent());
		}
		if (request.getGroupNo() != null) {
			entity.setGroupNo(request.getGroupNo());
		}

		memberLevelOpt.ifPresent(entity::setMemberLevel);
		enterpriseIdsOpt.ifPresent(entity::setEnterpriseIds);

		if (dist.distributorIdsColumn() != null) {
			entity.setDistributorIds(dist.distributorIdsColumn());
		}

		registrationActivityMapper.insert(entity);
		if (entity.getActivityId() == null || entity.getActivityId() <= 0L) {
			throw new ResourceException("创建报名活动失败");
		}

		Map<String, Object> rawForLang = buildRawForLang(activityName, request, entity, areaOpt, picsOpt);
		multiLangWriteService.addForNewRegistrationActivity(
				entity.getActivityId(), companyId, rawForLang, requestLangTag);

		saveRelShops(entity.getActivityId(), dist.shopIds(), now);

		boolean smsFlag = entity.getIsSmsNotice() != null && entity.getIsSmsNotice();
		String isOpenStr = smsFlag ? "true" : "false";
		smsTemplateUpdateService.updateSmsTemplate(
				entity.getCompanyId(), "registration_result_notice", isOpenStr, Optional.empty());

		return buildResponseMap(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateData(
			RegistrationActivityCreateRequest request,
			long companyId,
			String requestLangTag,
			Locale locale) {
		if (request.getActivityId() == null || request.getActivityId() <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage("selfservice.registration_activity.activity_id_required", null, locale));
		}
		if (!StringUtils.hasText(request.getActivityName())) {
			throw new BadRequestException("活动名称必填");
		}
		int startSec = parseUnixSecondsRequired(request.getStartTimeRaw(), "开始时间必填", "开始时间格式无效");
		int endSec = parseUnixSecondsRequired(request.getEndTimeRaw(), "结束时间必填", "结束时间格式无效");

		long activityId = request.getActivityId();
		long distributorIdFilter = request.getDistributorId() == null ? 0L : request.getDistributorId();

		LambdaQueryWrapper<RegistrationActivity> loadWrapper =
				new LambdaQueryWrapper<RegistrationActivity>()
						.eq(RegistrationActivity::getActivityId, activityId)
						.eq(RegistrationActivity::getCompanyId, companyId)
						.eq(RegistrationActivity::getDistributorId, distributorIdFilter);
		RegistrationActivity existing = registrationActivityMapper.selectOne(loadWrapper);
		if (existing == null) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_activity.no_update_data_found", null, locale));
		}

		RegistrationActivity entity = existing;
		String priorArea = entity.getArea();
		String priorPics = entity.getPics();
		String priorPlace = entity.getPlace();
		String priorAddress = entity.getAddress();
		String priorIntro = entity.getIntro();
		String priorJoinTips = entity.getJoinTips();
		String priorSubmitFormTips = entity.getSubmitFormTips();
		String priorContent = entity.getContent();

		boolean isSms = registrationFieldTruthy(request.getIsSmsNotice());
		boolean isWxapp = registrationFieldTruthy(request.getIsWxappNotice());

		int giftPoints = parseIntCol(request.getGiftPoints(), "gift_points");
		int isAllowDuplicate = parseIntCol(request.getIsAllowDuplicate(), "is_allow_duplicate");
		int isAllowCancel = parseIntCol(request.getIsAllowCancel(), "is_allow_cancel");
		int isOfflineVerify = parseIntCol(request.getIsOfflineVerify(), "is_offline_verify");
		int isNeedCheck = parseIntCol(request.getIsNeedCheck(), "is_need_check");
		int isWhiteList = parseIntCol(request.getIsWhiteList(), "is_white_list");

		DistributorResolution dist = resolveDistributorResolution(request);

		Optional<String> areaOpt = normalizeStructuredField(request.getArea(), "area");
		Optional<String> showFieldsOpt = normalizeStructuredField(request.getShowFields(), "show_fields");
		Optional<String> picsOpt = normalizeStructuredField(request.getPics(), "pics");

		Optional<String> memberLevelOpt = toCommaSeparatedDbValue(request.getMemberLevel(), "member_level");
		Optional<String> enterpriseIdsOpt = toCommaSeparatedDbValue(request.getEnterpriseIds(), "enterprise_ids");

		entity.setActivityName(request.getActivityName().trim());
		entity.setStartTime(startSec);
		entity.setEndTime(endSec);
		long tempIdParam = request.getTempId() == null ? 0L : request.getTempId();
		entity.setTempId(tempIdParam);
		entity.setDistributorId(distributorIdFilter);
		entity.setCompanyId(companyId);
		entity.setIsSmsNotice(isSms);
		entity.setIsWxappNotice(isWxapp);

		entity.setGiftPoints(giftPoints);
		entity.setIsAllowDuplicate(isAllowDuplicate);
		entity.setIsAllowCancel(isAllowCancel);
		entity.setIsOfflineVerify(isOfflineVerify);
		entity.setIsNeedCheck(isNeedCheck);
		entity.setIsWhiteList(isWhiteList);

		if (request.getJoinLimit() != null) {
			entity.setJoinLimit(request.getJoinLimit());
		}

		areaOpt.ifPresent(entity::setArea);
		showFieldsOpt.ifPresent(entity::setShowFields);
		picsOpt.ifPresent(entity::setPics);

		if (request.getPlace() != null) {
			entity.setPlace(request.getPlace());
		}
		if (request.getAddress() != null) {
			entity.setAddress(request.getAddress());
		}
		if (request.getIntro() != null) {
			entity.setIntro(request.getIntro());
		}
		if (request.getJoinTips() != null) {
			entity.setJoinTips(request.getJoinTips());
		}
		if (request.getSubmitFormTips() != null) {
			entity.setSubmitFormTips(request.getSubmitFormTips());
		}
		if (request.getContent() != null) {
			entity.setContent(request.getContent());
		}
		if (request.getGroupNo() != null) {
			entity.setGroupNo(request.getGroupNo());
		}

		memberLevelOpt.ifPresent(entity::setMemberLevel);
		enterpriseIdsOpt.ifPresent(entity::setEnterpriseIds);

		if (dist.distributorIdsColumn() != null) {
			entity.setDistributorIds(dist.distributorIdsColumn());
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setUpdated(now);

		int rows = registrationActivityMapper.updateById(entity);
		if (rows == 0) {
			throw new ResourceException(
					messageSource.getMessage("selfservice.registration_activity.no_update_data_found", null, locale));
		}

		Map<String, Object> langBag =
				buildUpdateLangBag(
						request,
						priorArea,
						priorPics,
						priorPlace,
						priorAddress,
						priorIntro,
						priorJoinTips,
						priorSubmitFormTips,
						priorContent);
		if (!langBag.isEmpty()) {
			multiLangWriteService.updateLangDataForRegistrationActivity(
					companyId, activityId, langBag, requestLangTag);
		}

		saveRelShops(activityId, dist.shopIds(), now);

		boolean smsFlag = entity.getIsSmsNotice() != null && entity.getIsSmsNotice();
		String isOpenStr = smsFlag ? "true" : "false";
		smsTemplateUpdateService.updateSmsTemplate(companyId, "registration_result_notice", isOpenStr, Optional.empty());

		return buildResponseMap(entity);
	}

	private Map<String, Object> buildUpdateLangBag(
			RegistrationActivityCreateRequest request,
			String priorArea,
			String priorPics,
			String priorPlace,
			String priorAddress,
			String priorIntro,
			String priorJoinTips,
			String priorSubmitFormTips,
			String priorContent) {
		LinkedHashMap<String, Object> langBag = new LinkedHashMap<>();
		String trimmedName = request.getActivityName().trim();
		if (StringUtils.hasText(trimmedName)) {
			langBag.put("activity_name", trimmedName);
		}
		if (request.getPlace() != null) {
			langBag.put("place", pickMultiLangScalar(request.getPlace(), priorPlace));
		}
		if (request.getArea() != null) {
			langBag.put("area", pickMultiLangStructured(request.getArea(), priorArea));
		}
		if (request.getAddress() != null) {
			langBag.put("address", pickMultiLangScalar(request.getAddress(), priorAddress));
		}
		if (request.getIntro() != null) {
			langBag.put("intro", pickMultiLangScalar(request.getIntro(), priorIntro));
		}
		if (request.getJoinTips() != null) {
			langBag.put("join_tips", pickMultiLangScalar(request.getJoinTips(), priorJoinTips));
		}
		if (request.getSubmitFormTips() != null) {
			langBag.put("submit_form_tips", pickMultiLangScalar(request.getSubmitFormTips(), priorSubmitFormTips));
		}
		if (request.getContent() != null) {
			langBag.put("content", pickMultiLangScalar(request.getContent(), priorContent));
		}
		if (request.getPics() != null) {
			langBag.put("pics", pickMultiLangStructured(request.getPics(), priorPics));
		}
		return langBag;
	}

	public Object restoreData(HttpServletRequest request) {
		String raw = request.getParameter("activity_id");
		long activityId = 0L;
		if (raw != null && !raw.trim().isEmpty()) {
			try {
				long parsed = Long.parseLong(raw.trim());
				if (parsed > 0L) {
					activityId = parsed;
				}
			} catch (NumberFormatException ignored) {
				activityId = 0L;
			}
		}
		if (activityId == 0L) {
			return Collections.emptyList();
		}
		int endTime = (int) (Instant.now().getEpochSecond() - 3600L);
		LambdaUpdateWrapper<RegistrationActivity> wrapper =
				new LambdaUpdateWrapper<RegistrationActivity>()
						.eq(RegistrationActivity::getActivityId, activityId)
						.set(RegistrationActivity::getEndTime, endTime);
		int rows = registrationActivityMapper.update(null, wrapper);
		Map<String, Object> out = new LinkedHashMap<>(2);
		out.put("status", rows);
		return out;
	}

	private Map<String, Object> buildRawForLang(
			String activityNameTrimmed,
			RegistrationActivityCreateRequest request,
			RegistrationActivity entity,
			Optional<String> areaDb,
			Optional<String> picsDb) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("activity_name", activityNameTrimmed);
		m.put("place", pickMultiLangScalar(request.getPlace(), entity.getPlace()));
		m.put("area", pickMultiLangStructured(request.getArea(), areaDb.orElse(null)));
		m.put("address", pickMultiLangScalar(request.getAddress(), entity.getAddress()));
		m.put("intro", pickMultiLangScalar(request.getIntro(), entity.getIntro()));
		m.put("join_tips", pickMultiLangScalar(request.getJoinTips(), entity.getJoinTips()));
		m.put("submit_form_tips", pickMultiLangScalar(request.getSubmitFormTips(), entity.getSubmitFormTips()));
		m.put("content", pickMultiLangScalar(request.getContent(), entity.getContent()));
		m.put("pics", pickMultiLangStructured(request.getPics(), picsDb.orElse(null)));
		return m;
	}

	private static Object pickMultiLangScalar(String rawRequest, String entityVal) {
		if (rawRequest != null) {
			return rawRequest;
		}
		return entityVal;
	}

	private Object pickMultiLangStructured(Object rawRequest, String entityJson) {
		if (rawRequest instanceof Map<?, ?> || rawRequest instanceof List<?> || rawRequest instanceof JsonNode) {
			return rawRequest;
		}
		return entityJson;
	}

	private void saveRelShops(long activityId, List<Long> distributorIds, int now) {
		LambdaQueryWrapper<RegistrationActivityRelShop> w =
				new LambdaQueryWrapper<RegistrationActivityRelShop>()
						.eq(RegistrationActivityRelShop::getActivityId, activityId);
		List<RegistrationActivityRelShop> rs = relShopMapper.selectList(w);

		if (!distributorIds.isEmpty()) {
			Set<Long> oldIds =
					rs.stream()
							.map(RegistrationActivityRelShop::getDistributorId)
							.filter(id -> id != null)
							.collect(Collectors.toSet());
			Set<Long> newSet = new LinkedHashSet<>(distributorIds);
			List<Long> del =
					oldIds.stream().filter(id -> !newSet.contains(id)).collect(Collectors.toList());
			if (!del.isEmpty()) {
				relShopMapper.delete(
						new LambdaQueryWrapper<RegistrationActivityRelShop>()
								.eq(RegistrationActivityRelShop::getActivityId, activityId)
								.in(RegistrationActivityRelShop::getDistributorId, del));
			}
			List<Long> newIds = new ArrayList<>(newSet);
			newIds.removeAll(oldIds);
			for (Long id : newIds) {
				RegistrationActivityRelShop row = new RegistrationActivityRelShop();
				row.setActivityId(activityId);
				row.setDistributorId(id);
				row.setCreated(now);
				row.setUpdated(now);
				relShopMapper.insert(row);
			}
		} else {
			RegistrationActivityRelShop insertRow = new RegistrationActivityRelShop();
			insertRow.setActivityId(activityId);
			insertRow.setDistributorId(0L);
			insertRow.setCreated(now);
			insertRow.setUpdated(now);
			for (RegistrationActivityRelShop v : rs) {
				if (v.getDistributorId() != null && v.getDistributorId() != 0L) {
					relShopMapper.deleteById(v.getId());
				} else {
					insertRow = null;
				}
			}
			if (insertRow != null) {
				relShopMapper.insert(insertRow);
			}
		}
	}

	private Map<String, Object> buildResponseMap(RegistrationActivity entity) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("activity_id", entity.getActivityId());
		out.put("temp_id", entity.getTempId());
		out.put("activity_name", entity.getActivityName());
		out.put("start_time", entity.getStartTime());
		out.put("end_time", entity.getEndTime());
		out.put("join_limit", entity.getJoinLimit());
		out.put("is_sms_notice", entity.getIsSmsNotice());
		out.put("is_wxapp_notice", entity.getIsWxappNotice());
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		out.put("company_id", entity.getCompanyId());
		out.put("area", entity.getArea());
		out.put("place", entity.getPlace());
		out.put("address", entity.getAddress());
		out.put("intro", entity.getIntro());
		out.put("show_fields", entity.getShowFields());
		out.put("pics", entity.getPics());
		out.put("gift_points", entity.getGiftPoints());
		out.put("is_allow_duplicate", entity.getIsAllowDuplicate());
		out.put("is_allow_cancel", entity.getIsAllowCancel());
		out.put("is_offline_verify", entity.getIsOfflineVerify());
		out.put("is_need_check", entity.getIsNeedCheck());
		out.put("is_white_list", entity.getIsWhiteList());
		out.put("enterprise_ids", entity.getEnterpriseIds());
		out.put("group_no", entity.getGroupNo());
		out.put("member_level", entity.getMemberLevel());
		out.put("distributor_ids", entity.getDistributorIds());
		out.put("join_tips", entity.getJoinTips());
		out.put("submit_form_tips", entity.getSubmitFormTips());
		out.put("content", entity.getContent());
		out.put("distributor_id", entity.getDistributorId());
		return out;
	}

	private DistributorResolution resolveDistributorResolution(RegistrationActivityCreateRequest request) {
		Object raw = request.getDistributorIds();
		if (isTruthyDistributorIdsRaw(raw)) {
			if (raw instanceof CharSequence cs) {
				String s = cs.toString().trim();
				if (s.length() > 100) {
					s = s.substring(0, 100);
				}
				List<Long> ids = parseLongListFromComma(s);
				return new DistributorResolution(ids, s);
			}
			if (raw instanceof Iterable<?> it && !(raw instanceof CharSequence)) {
				List<Long> ids = parseLongListFromCollection(it);
				String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
				if (joined.length() > 100) {
					joined = joined.substring(0, 100);
				}
				return new DistributorResolution(ids, joined);
			}
			if (raw instanceof JsonNode jn) {
				if (jn.isTextual()) {
					String s = jn.asText().trim();
					if (StringUtils.hasText(s)) {
						if (s.length() > 100) {
							s = s.substring(0, 100);
						}
						List<Long> ids = parseLongListFromComma(s);
						return new DistributorResolution(ids, s);
					}
				}
				if (jn.isArray()) {
					List<Long> ids = new ArrayList<>();
					for (JsonNode n : jn) {
						if (n == null || n.isNull()) {
							continue;
						}
						if (n.isNumber()) {
							ids.add(n.longValue());
						} else if (n.isTextual() && StringUtils.hasText(n.asText())) {
							try {
								ids.add(Long.parseLong(n.asText().trim()));
							} catch (NumberFormatException ex) {
								throw new BadRequestException("店铺ID列表无效");
							}
						}
					}
					String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
					String col = joined.length() > 100 ? joined.substring(0, 100) : joined;
					return new DistributorResolution(ids, col);
				}
			}
		}
		if (request.getDistributorId() != null && request.getDistributorId() > 0L) {
			long id = request.getDistributorId();
			return new DistributorResolution(List.of(id), String.valueOf(id));
		}
		return new DistributorResolution(List.of(), null);
	}

	private boolean isTruthyDistributorIdsRaw(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof CharSequence cs) {
			return StringUtils.hasText(cs);
		}
		if (raw instanceof Iterable<?> it && !(raw instanceof CharSequence)) {
			return it.iterator().hasNext();
		}
		if (raw instanceof JsonNode jn) {
			if (jn.isArray()) {
				return jn.size() > 0;
			}
			if (jn.isTextual()) {
				return StringUtils.hasText(jn.asText());
			}
			return false;
		}
		return false;
	}

	private List<Long> parseLongListFromComma(String s) {
		List<Long> ids = new ArrayList<>();
		for (String part : s.split(",")) {
			String t = part.trim();
			if (!StringUtils.hasText(t)) {
				continue;
			}
			try {
				ids.add(Long.parseLong(t));
			} catch (NumberFormatException ex) {
				throw new BadRequestException("店铺ID列表无效");
			}
		}
		return ids;
	}

	private List<Long> parseLongListFromCollection(Iterable<?> col) {
		List<Long> ids = new ArrayList<>();
		for (Object o : col) {
			if (o == null) {
				continue;
			}
			if (o instanceof Number n) {
				ids.add(n.longValue());
			} else if (StringUtils.hasText(String.valueOf(o))) {
				try {
					ids.add(Long.parseLong(String.valueOf(o).trim()));
				} catch (NumberFormatException ex) {
					throw new BadRequestException("店铺ID列表无效");
				}
			}
		}
		return ids;
	}

	private record DistributorResolution(List<Long> shopIds, String distributorIdsColumn) {}

	private Optional<String> normalizeStructuredField(Object raw, String snakeName) {
		if (raw == null) {
			return Optional.empty();
		}
		if (raw instanceof CharSequence s) {
			return Optional.of(s.toString());
		}
		try {
			if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
				return Optional.of(objectMapper.writeValueAsString(raw));
			}
			if (raw instanceof JsonNode jn) {
				return Optional.of(objectMapper.writeValueAsString(jn));
			}
		} catch (JsonProcessingException ex) {
			throw new BadRequestException(snakeName + " 无效");
		}
		throw new BadRequestException(snakeName + " 无效");
	}

	private Optional<String> toCommaSeparatedDbValue(Object raw, String snakeName) {
		if (raw == null) {
			return Optional.empty();
		}
		if (raw instanceof Iterable<?> it && !(raw instanceof CharSequence)) {
			List<String> parts = new ArrayList<>();
			for (Object o : it) {
				if (o != null) {
					parts.add(String.valueOf(o));
				}
			}
			return Optional.of(String.join(",", parts));
		}
		if (raw instanceof JsonNode jn) {
			if (jn.isArray()) {
				List<String> parts = new ArrayList<>();
				for (JsonNode n : jn) {
					if (n != null && !n.isNull()) {
						parts.add(n.asText(""));
					}
				}
				return Optional.of(String.join(",", parts));
			}
			if (jn.isTextual()) {
				return Optional.of(jn.asText());
			}
			throw new BadRequestException(snakeName + " 无效");
		}
		if (raw instanceof CharSequence s) {
			return Optional.of(s.toString());
		}
		throw new BadRequestException(snakeName + " 无效");
	}

	private static int parseIntCol(Object raw, String snakeName) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof CharSequence s) {
			String t = s.toString().trim();
			if (!StringUtils.hasText(t)) {
				return 0;
			}
			try {
				return Integer.parseInt(t);
			} catch (NumberFormatException ex) {
				throw new BadRequestException(snakeName + " 无效");
			}
		}
		throw new BadRequestException(snakeName + " 无效");
	}

	private static boolean registrationFieldTruthy(Object v) {
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		String s = String.valueOf(v).trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static int parseUnixSecondsRequired(Object raw, String emptyMessage, String invalidMessage) {
		if (raw == null || (raw instanceof CharSequence cs && !StringUtils.hasText(cs))) {
			throw new BadRequestException(emptyMessage);
		}
		if (raw instanceof Number n) {
			return (int) n.longValue();
		}
		if (raw instanceof CharSequence s) {
			String t = s.toString().trim();
			if (!StringUtils.hasText(t)) {
				throw new BadRequestException(emptyMessage);
			}
			boolean allDigit = t.chars().allMatch(Character::isDigit);
			if (allDigit) {
				try {
					long v = Long.parseLong(t);
					return (int) v;
				} catch (NumberFormatException ex) {
					throw new BadRequestException(invalidMessage);
				}
			}
			throw new BadRequestException(invalidMessage);
		}
		if (raw instanceof JsonNode node) {
			if (node.isNumber()) {
				return node.asInt();
			}
			if (node.isTextual()) {
				return parseUnixSecondsRequired(node.asText(), emptyMessage, invalidMessage);
			}
			throw new BadRequestException(invalidMessage);
		}
		throw new BadRequestException(invalidMessage);
	}
}
