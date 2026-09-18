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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.espier.domain.Address;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import cn.shopex.ecshopx.selfservice.domain.FormTemplate;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.FormTemplateMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.service.export.RegistrationRecordCsvExportService;
import cn.shopex.ecshopx.selfservice.service.multilang.FormTemplateOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationRecordOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationRecordDataInfoService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String MSG_STATUS_WAITING = "selfservice.registration_activity.status_waiting";
	private static final String MSG_STATUS_ENDED = "selfservice.registration_activity.status_ended";
	private static final String MSG_STATUS_ONGOING = "selfservice.registration_activity.status_ongoing";

	private static final String MSG_REC_PENDING = "selfservice.registration_record.status_pending";
	private static final String MSG_REC_PASSED = "selfservice.registration_record.status_passed";
	private static final String MSG_REC_REJECTED = "selfservice.registration_record.status_rejected";
	private static final String MSG_REC_VERIFIED = "selfservice.registration_record.status_verified";
	private static final String MSG_REC_CANCELED = "selfservice.registration_record.status_canceled";

	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService;
	private final RegistrationRecordOutsideMultiLangReadService registrationRecordOutsideMultiLangReadService;
	private final RegistrationRecordContentDatapassService registrationRecordContentDatapassService;
	private final AddressMapper addressMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final MessageSource messageSource;
	private final ObjectMapper objectMapper;
	private final FormTemplateMapper formTemplateMapper;
	private final RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler;
	private final RegistrationRecordCsvExportService registrationRecordCsvExportService;
	private final FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService;

	public RegistrationRecordDataInfoService(
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService,
			RegistrationRecordOutsideMultiLangReadService registrationRecordOutsideMultiLangReadService,
			RegistrationRecordContentDatapassService registrationRecordContentDatapassService,
			AddressMapper addressMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			MessageSource messageSource,
			ObjectMapper objectMapper,
			FormTemplateMapper formTemplateMapper,
			RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler,
			RegistrationRecordCsvExportService registrationRecordCsvExportService,
			FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService) {
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationActivityOutsideMultiLangReadService = registrationActivityOutsideMultiLangReadService;
		this.registrationRecordOutsideMultiLangReadService = registrationRecordOutsideMultiLangReadService;
		this.registrationRecordContentDatapassService = registrationRecordContentDatapassService;
		this.addressMapper = addressMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.messageSource = messageSource;
		this.objectMapper = objectMapper;
		this.formTemplateMapper = formTemplateMapper;
		this.registrationRecordRowMapAssembler = registrationRecordRowMapAssembler;
		this.registrationRecordCsvExportService = registrationRecordCsvExportService;
		this.formTemplateOutsideMultiLangReadService = formTemplateOutsideMultiLangReadService;
	}

	public Map<String, Object> getDataInfo(
			long recordId, String requestLangTag, Locale locale, boolean datapassBlocked) {
		RegistrationRecord entity = registrationRecordMapper.selectById(recordId);
		if (entity == null) {
			LinkedHashMap<String, Object> missing = new LinkedHashMap<>();
			missing.put("content", null);
			return missing;
		}

		JsonNode parsedContent = parseContentArray(entity.getContent());

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		Long rid = entity.getRecordId();
		out.put("record_id", rid);
		out.put("activity_id", entity.getActivityId());
		out.put("user_id", entity.getUserId());
		out.put("mobile", decryptToString(entity.getMobile()));
		out.put("status", entity.getStatus());
		out.put("reason", entity.getReason());
		out.put("remark", entity.getRemark());
		out.put("created", entity.getCreated());
		out.put("updated", entity.getUpdated());
		out.put("company_id", entity.getCompanyId());
		out.put("verify_time", entity.getVerifyTime());
		out.put("verify_operator", entity.getVerifyOperator());
		out.put("true_name", entity.getTrueName());
		out.put("form_mobile", decryptToString(entity.getFormMobile()));
		out.put("group_no", entity.getGroupNo() == null ? "" : entity.getGroupNo());
		out.put("wxapp_appid", entity.getWxappAppid());
		out.put("open_id", entity.getOpenId());
		out.put("distributor_id", entity.getDistributorId());
		out.put("record_no", formatRecordNo(entity.getRecordNo()));
		out.put("form_id", entity.getFormId());
		out.put("get_points", entity.getGetPoints());
		out.put("verify_code", entity.getVerifyCode());
		out.put("is_white_list", entity.getIsWhiteList());

		long recordOwnerCompanyId = entity.getCompanyId() == null ? 0L : entity.getCompanyId();
		long recId = rid != null ? rid : 0L;
		registrationRecordOutsideMultiLangReadService.applyRecordLangOverrides(
				recordOwnerCompanyId, recId, out, requestLangTag);

		RegistrationActivity activityEntity = null;
		Long aid = entity.getActivityId();
		if (aid != null && aid > 0L) {
			activityEntity = registrationActivityMapper.selectById(aid);
		}

		if (activityEntity != null) {
			out.put("activity_name", activityEntity.getActivityName());
			out.put("start_time", activityEntity.getStartTime());
			out.put("end_time", activityEntity.getEndTime());
			out.put("join_limit", activityEntity.getJoinLimit());
			out.put("is_sms_notice", activityEntity.getIsSmsNotice());
			out.put("is_wxapp_notice", activityEntity.getIsWxappNotice());
			out.put("start_date", formatActivityDate(activityEntity.getStartTime()));
			out.put("end_date", formatActivityDate(activityEntity.getEndTime()));

			LinkedHashMap<String, Object> activityInfo = new LinkedHashMap<>();
			activityInfo.put("is_offline_verify", activityEntity.getIsOfflineVerify());
			activityInfo.put("is_allow_duplicate", activityEntity.getIsAllowDuplicate());
			activityInfo.put("address", activityEntity.getAddress());
			activityInfo.put("place", activityEntity.getPlace());
			activityInfo.put("intro", activityEntity.getIntro());
			activityInfo.put("pics", activityEntity.getPics());
			activityInfo.put("area", activityEntity.getArea());
			activityInfo.put("activity_name", activityEntity.getActivityName());
			activityInfo.put("join_tips", activityEntity.getJoinTips());
			activityInfo.put("submit_form_tips", activityEntity.getSubmitFormTips());
			activityInfo.put("content", activityEntity.getContent());

			int now = (int) (System.currentTimeMillis() / 1000L);
			Integer st = activityEntity.getStartTime();
			Integer et = activityEntity.getEndTime();
			int stVal = st != null ? st : 0;
			int etVal = et != null ? et : 0;
			String msgKey;
			if (stVal > now) {
				msgKey = MSG_STATUS_WAITING;
			} else if (etVal < now) {
				msgKey = MSG_STATUS_ENDED;
			} else {
				msgKey = MSG_STATUS_ONGOING;
			}
			activityInfo.put("status_name", messageSource.getMessage(msgKey, null, locale));

			buildAreaName(activityInfo);

			Long ownerCid = activityEntity.getCompanyId();
			if (ownerCid != null) {
				registrationActivityOutsideMultiLangReadService.applyDetailOutsideLangOverrides(
						ownerCid, aid.longValue(), activityInfo, requestLangTag);
				registrationActivityOutsideMultiLangReadService.applyActivityNameOverrides(
						ownerCid.longValue(), List.of(out), requestLangTag);
			}

			out.put("activity_info", activityInfo);
		} else {
			out.put("activity_name", "");
			out.put("start_time", 0);
			out.put("end_time", 0);
			out.put("join_limit", 0);
			out.put("is_sms_notice", null);
			out.put("is_wxapp_notice", null);
			out.put("start_date", "");
			out.put("end_date", "");
			out.put("activity_info", new LinkedHashMap<String, Object>());
		}

		out.put("status_name", statusNameForRecord(entity.getStatus(), locale));
		out.put(
				"content",
				registrationRecordContentDatapassService.applyMaskIfNeeded(parsedContent, datapassBlocked));

		return out;
	}

	public Map<String, Object> getDatalist(
			long companyId,
			String requestLangTag,
			Locale locale,
			int page,
			int pageSize,
			Long activityIdPositiveOrNull,
			String startTimeRaw,
			String endTimeRaw,
			String mobileTrimOrNull,
			String statusTrimOrNull,
			String trueNameTrimOrNull,
			String isWhiteListRaw,
			boolean datapassBlocked) {
		LambdaQueryWrapper<RegistrationRecord> countW =
				buildRegistrationRecordListFilter(
						companyId,
						activityIdPositiveOrNull,
						mobileTrimOrNull,
						statusTrimOrNull,
						trueNameTrimOrNull,
						isWhiteListRaw,
						startTimeRaw,
						endTimeRaw);
		long total = registrationRecordMapper.selectCount(countW);
		Map<String, Object> empty = new LinkedHashMap<>();
		empty.put("total_count", 0L);
		empty.put("list", Collections.emptyList());
		if (total == 0L) {
			return empty;
		}

		LambdaQueryWrapper<RegistrationRecord> listW =
				buildRegistrationRecordListFilter(
						companyId,
						activityIdPositiveOrNull,
						mobileTrimOrNull,
						statusTrimOrNull,
						trueNameTrimOrNull,
						isWhiteListRaw,
						startTimeRaw,
						endTimeRaw);
		listW.orderByDesc(RegistrationRecord::getRecordId);
		if (pageSize > 0) {
			long offset = Math.max(0L, ((long) page - 1L) * (long) pageSize);
			listW.last("LIMIT " + pageSize + " OFFSET " + offset);
		}
		List<RegistrationRecord> entities = registrationRecordMapper.selectList(listW);

		Set<Long> activityIdSet = new LinkedHashSet<>();
		for (RegistrationRecord e : entities) {
			Long aid = e.getActivityId();
			if (aid != null && aid > 0L) {
				activityIdSet.add(aid);
			}
		}
		Map<Long, RegistrationActivity> activityById = new HashMap<>();
		if (!activityIdSet.isEmpty()) {
			List<RegistrationActivity> batch = registrationActivityMapper.selectBatchIds(activityIdSet);
			for (RegistrationActivity a : batch) {
				if (a.getActivityId() != null) {
					activityById.put(a.getActivityId(), a);
				}
			}
		}

		List<Map<String, Object>> listRows = new ArrayList<>(entities.size());
		for (RegistrationRecord entity : entities) {
			Map<String, Object> row = registrationRecordRowMapAssembler.toApiRow(entity);
			Integer cr = entity.getCreated();
			if (cr != null && cr != 0) {
				String ds = LocalDateTime.ofInstant(Instant.ofEpochSecond(cr.longValue()), CN).format(DATE_TIME_FMT);
				row.put("created_date", ds);
				row.put("create_date", ds);
			}
			row.put("content", parseContentArray(entity.getContent()));
			row.put("record_no", formatRecordNo(entity.getRecordNo()));

			Long rid = entity.getRecordId();
			long recId = rid != null ? rid : 0L;
			registrationRecordOutsideMultiLangReadService.applyRecordLangOverrides(
					companyId, recId, row, requestLangTag);

			Long aid = entity.getActivityId();
			RegistrationActivity act = aid == null ? null : activityById.get(aid);
			if (act != null) {
				row.put("activity_name", act.getActivityName());
				Integer st = act.getStartTime();
				Integer et = act.getEndTime();
				int stVal = st != null ? st : 0;
				int etVal = et != null ? et : 0;
				int now = (int) (System.currentTimeMillis() / 1000L);
				String msgKey;
				if (stVal > now) {
					msgKey = MSG_STATUS_WAITING;
				} else if (etVal < now) {
					msgKey = MSG_STATUS_ENDED;
				} else {
					msgKey = MSG_STATUS_ONGOING;
				}
				LinkedHashMap<String, Object> activityInfo = new LinkedHashMap<>();
				activityInfo.put("address", act.getAddress());
				activityInfo.put("place", act.getPlace());
				activityInfo.put("is_allow_duplicate", act.getIsAllowDuplicate());
				activityInfo.put("intro", act.getIntro());
				activityInfo.put("pics", act.getPics());
				activityInfo.put("start_time", formatActivityListStartTimeDisplay(stVal));
				activityInfo.put("status_name", messageSource.getMessage(msgKey, null, locale));
				activityInfo.put("area", act.getArea());
				buildAreaName(activityInfo);
				row.put("activity_info", activityInfo);
				row.put("start_time", act.getStartTime());
				row.put("end_time", act.getEndTime());
				Integer jl = act.getJoinLimit();
				row.put("join_limit", jl == null ? 1 : jl);
				row.put("is_sms_notice", act.getIsSmsNotice());
				row.put("is_wxapp_notice", act.getIsWxappNotice());
				row.put("start_date", formatActivityDate(act.getStartTime()));
				row.put("end_date", formatActivityDate(act.getEndTime()));
			} else {
				row.put("activity_name", "");
				row.put("start_time", 0);
				row.put("end_time", 0);
				row.put("join_limit", 0);
				row.put("is_sms_notice", null);
				row.put("is_wxapp_notice", null);
				row.put("start_date", "");
				row.put("end_date", "");
				row.put("activity_info", new LinkedHashMap<String, Object>());
			}
			row.put("status_name", statusNameForRecord(entity.getStatus(), locale));
			listRows.add(row);
		}

		if (!listRows.isEmpty()) {
			registrationActivityOutsideMultiLangReadService.applyActivityNameOverrides(
					companyId, listRows, requestLangTag);
		}

		Set<Long> secondActivityIds = new LinkedHashSet<>();
		for (Map<String, Object> row : listRows) {
			Object o = row.get("activity_id");
			if (o instanceof Number n) {
				long v = n.longValue();
				if (v > 0L) {
					secondActivityIds.add(v);
				}
			}
		}
		if (!secondActivityIds.isEmpty()) {
			LambdaQueryWrapper<RegistrationActivity> nameQ = new LambdaQueryWrapper<>();
			nameQ.eq(RegistrationActivity::getCompanyId, companyId);
			nameQ.in(RegistrationActivity::getActivityId, secondActivityIds);
			nameQ.select(RegistrationActivity::getActivityId, RegistrationActivity::getActivityName);
			List<RegistrationActivity> nameRows = registrationActivityMapper.selectList(nameQ);
			List<Map<String, Object>> nameOverrideRows = new ArrayList<>();
			for (RegistrationActivity a : nameRows) {
				LinkedHashMap<String, Object> m = new LinkedHashMap<>();
				m.put("activity_id", a.getActivityId());
				m.put("activity_name", a.getActivityName() != null ? a.getActivityName() : "");
				nameOverrideRows.add(m);
			}
			registrationActivityOutsideMultiLangReadService.applyActivityNameOverrides(
					companyId, nameOverrideRows, requestLangTag);
			Map<Long, Object> idToActivityName = new HashMap<>();
			for (Map<String, Object> m : nameOverrideRows) {
				Object aidObj = m.get("activity_id");
				if (aidObj instanceof Number n) {
					idToActivityName.put(n.longValue(), m.get("activity_name"));
				}
			}
			for (Map<String, Object> row : listRows) {
				Object o = row.get("activity_id");
				if (o instanceof Number n) {
					long vid = n.longValue();
					Object nm = idToActivityName.get(vid);
					row.put("activity_name", nm != null ? nm : vid);
				}
			}
		}

		Set<Long> formIdSet = new LinkedHashSet<>();
		for (Map<String, Object> row : listRows) {
			Object o = row.get("form_id");
			if (o instanceof Number n) {
				long v = n.longValue();
				if (v > 0L) {
					formIdSet.add(v);
				}
			}
		}
		if (!formIdSet.isEmpty()) {
			LambdaQueryWrapper<FormTemplate> ftW = new LambdaQueryWrapper<>();
			ftW.in(FormTemplate::getId, formIdSet);
			ftW.eq(FormTemplate::getCompanyId, companyId);
			ftW.select(FormTemplate::getId, FormTemplate::getTemName);
			List<FormTemplate> tpls = formTemplateMapper.selectList(ftW);
			List<Map<String, Object>> tplRows = new ArrayList<>();
			for (FormTemplate t : tpls) {
				if (t.getId() == null) {
					continue;
				}
				LinkedHashMap<String, Object> m = new LinkedHashMap<>();
				m.put("id", t.getId());
				m.put("tem_name", t.getTemName() != null ? t.getTemName() : "");
				tplRows.add(m);
			}
			formTemplateOutsideMultiLangReadService.applyListLangOverrides(companyId, tplRows, requestLangTag);
			Map<Long, Object> idToTemName = new HashMap<>();
			for (Map<String, Object> m : tplRows) {
				Object idObj = m.get("id");
				if (idObj instanceof Number n) {
					idToTemName.put(n.longValue(), m.get("tem_name"));
				}
			}
			for (Map<String, Object> row : listRows) {
				Object o = row.get("form_id");
				if (o instanceof Number n) {
					long fid = n.longValue();
					if (fid > 0L) {
						Object tem = idToTemName.get(fid);
						row.put("tem_name", tem != null ? tem : fid);
					}
				}
			}
		}

		if (datapassBlocked) {
			for (Map<String, Object> row : listRows) {
				Object mob = row.get("mobile");
				if (mob == null) {
					continue;
				}
				row.put("mobile", registrationRecordCsvExportService.maskMobileLoose(String.valueOf(mob)));
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", listRows);
		return result;
	}

	public Map<String, Object> registrationVerifyLog(
			long companyId,
			String requestLangTag,
			Locale locale,
			int page,
			int pageSize,
			boolean datapassBlocked) {
		LambdaQueryWrapper<RegistrationRecord> countW = new LambdaQueryWrapper<>();
		countW.eq(RegistrationRecord::getCompanyId, companyId);
		long total = registrationRecordMapper.selectCount(countW);
		Map<String, Object> empty = new LinkedHashMap<>();
		empty.put("total_count", 0L);
		empty.put("list", Collections.emptyList());
		if (total == 0L) {
			return empty;
		}

		LambdaQueryWrapper<RegistrationRecord> listW = new LambdaQueryWrapper<>();
		listW.eq(RegistrationRecord::getCompanyId, companyId);
		listW.orderByDesc(RegistrationRecord::getRecordId);
		if (pageSize > 0) {
			long offset = Math.max(0L, ((long) page - 1L) * (long) pageSize);
			listW.last("LIMIT " + pageSize + " OFFSET " + offset);
		}
		List<RegistrationRecord> entities = registrationRecordMapper.selectList(listW);

		Set<Long> activityIdSet = new LinkedHashSet<>();
		for (RegistrationRecord e : entities) {
			Long aid = e.getActivityId();
			if (aid != null && aid > 0L) {
				activityIdSet.add(aid);
			}
		}
		Map<Long, RegistrationActivity> activityById = new HashMap<>();
		if (!activityIdSet.isEmpty()) {
			List<RegistrationActivity> batch = registrationActivityMapper.selectBatchIds(activityIdSet);
			for (RegistrationActivity a : batch) {
				if (a.getActivityId() != null) {
					activityById.put(a.getActivityId(), a);
				}
			}
		}

		List<Map<String, Object>> listRows = new ArrayList<>(entities.size());
		for (RegistrationRecord entity : entities) {
			Map<String, Object> row = registrationRecordRowMapAssembler.toApiRow(entity);
			Integer cr = entity.getCreated();
			if (cr != null && cr != 0) {
				String ds = LocalDateTime.ofInstant(Instant.ofEpochSecond(cr.longValue()), CN).format(DATE_TIME_FMT);
				row.put("created_date", ds);
				row.put("create_date", ds);
			}
			row.put("content", parseContentArray(entity.getContent()));
			row.put("record_no", formatRecordNo(entity.getRecordNo()));

			Long rid = entity.getRecordId();
			long recId = rid != null ? rid : 0L;
			registrationRecordOutsideMultiLangReadService.applyRecordLangOverrides(
					companyId, recId, row, requestLangTag);

			Long aid = entity.getActivityId();
			RegistrationActivity act = aid == null ? null : activityById.get(aid);
			if (act != null) {
				row.put("activity_name", act.getActivityName());
				Integer st = act.getStartTime();
				Integer et = act.getEndTime();
				int stVal = st != null ? st : 0;
				int etVal = et != null ? et : 0;
				int now = (int) (System.currentTimeMillis() / 1000L);
				String msgKey;
				if (stVal > now) {
					msgKey = MSG_STATUS_WAITING;
				} else if (etVal < now) {
					msgKey = MSG_STATUS_ENDED;
				} else {
					msgKey = MSG_STATUS_ONGOING;
				}
				LinkedHashMap<String, Object> activityInfo = new LinkedHashMap<>();
				activityInfo.put("address", act.getAddress());
				activityInfo.put("place", act.getPlace());
				activityInfo.put("is_allow_duplicate", act.getIsAllowDuplicate());
				activityInfo.put("intro", act.getIntro());
				activityInfo.put("pics", act.getPics());
				activityInfo.put("start_time", formatActivityListStartTimeDisplay(stVal));
				activityInfo.put("status_name", messageSource.getMessage(msgKey, null, locale));
				activityInfo.put("area", act.getArea());
				buildAreaName(activityInfo);
				row.put("activity_info", activityInfo);
				row.put("start_time", act.getStartTime());
				row.put("end_time", act.getEndTime());
				Integer jl = act.getJoinLimit();
				row.put("join_limit", jl == null ? 1 : jl);
				row.put("is_sms_notice", act.getIsSmsNotice());
				row.put("is_wxapp_notice", act.getIsWxappNotice());
				row.put("start_date", formatActivityDate(act.getStartTime()));
				row.put("end_date", formatActivityDate(act.getEndTime()));
			} else {
				row.put("activity_name", "");
				row.put("start_time", 0);
				row.put("end_time", 0);
				row.put("join_limit", 0);
				row.put("is_sms_notice", null);
				row.put("is_wxapp_notice", null);
				row.put("start_date", "");
				row.put("end_date", "");
				row.put("activity_info", new LinkedHashMap<String, Object>());
			}
			row.put("status_name", statusNameForRecord(entity.getStatus(), locale));
			listRows.add(row);
		}

		if (!listRows.isEmpty()) {
			registrationActivityOutsideMultiLangReadService.applyActivityNameOverrides(
					companyId, listRows, requestLangTag);
		}

		Set<Long> formIdSet = new LinkedHashSet<>();
		for (Map<String, Object> row : listRows) {
			Object o = row.get("form_id");
			if (o instanceof Number n) {
				long v = n.longValue();
				if (v > 0L) {
					formIdSet.add(v);
				}
			}
		}
		if (!formIdSet.isEmpty()) {
			LambdaQueryWrapper<FormTemplate> ftW = new LambdaQueryWrapper<>();
			ftW.in(FormTemplate::getId, formIdSet);
			ftW.eq(FormTemplate::getCompanyId, companyId);
			ftW.select(FormTemplate::getId, FormTemplate::getTemName);
			List<FormTemplate> tpls = formTemplateMapper.selectList(ftW);
			List<Map<String, Object>> tplRows = new ArrayList<>();
			for (FormTemplate t : tpls) {
				if (t.getId() == null) {
					continue;
				}
				LinkedHashMap<String, Object> m = new LinkedHashMap<>();
				m.put("id", t.getId());
				m.put("tem_name", t.getTemName() != null ? t.getTemName() : "");
				tplRows.add(m);
			}
			formTemplateOutsideMultiLangReadService.applyListLangOverrides(companyId, tplRows, requestLangTag);
			Map<Long, Object> idToTemName = new HashMap<>();
			for (Map<String, Object> m : tplRows) {
				Object idObj = m.get("id");
				if (idObj instanceof Number n) {
					idToTemName.put(n.longValue(), m.get("tem_name"));
				}
			}
			for (Map<String, Object> row : listRows) {
				Object o = row.get("form_id");
				if (o instanceof Number n) {
					long fid = n.longValue();
					if (fid > 0L) {
						Object tem = idToTemName.get(fid);
						row.put("tem_name", tem != null ? tem : fid);
					}
				}
			}
		}

		if (datapassBlocked) {
			for (Map<String, Object> row : listRows) {
				Object mob = row.get("mobile");
				if (mob == null) {
					continue;
				}
				row.put("mobile", registrationRecordCsvExportService.maskMobileLoose(String.valueOf(mob)));
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", listRows);
		return result;
	}

	private LambdaQueryWrapper<RegistrationRecord> buildRegistrationRecordListFilter(
			long companyId,
			Long activityIdPositiveOrNull,
			String mobileTrimOrNull,
			String statusTrimOrNull,
			String trueNameTrimOrNull,
			String isWhiteListRaw,
			String startTimeRaw,
			String endTimeRaw) {
		LambdaQueryWrapper<RegistrationRecord> w = new LambdaQueryWrapper<>();
		w.eq(RegistrationRecord::getCompanyId, companyId);
		if (activityIdPositiveOrNull != null && activityIdPositiveOrNull > 0L) {
			w.eq(RegistrationRecord::getActivityId, activityIdPositiveOrNull);
		}
		if (StringUtils.hasText(mobileTrimOrNull)) {
			w.eq(RegistrationRecord::getMobile, sensitiveFieldEncryptor.encrypt(mobileTrimOrNull.trim()));
		}
		if (StringUtils.hasText(statusTrimOrNull)) {
			w.eq(RegistrationRecord::getStatus, statusTrimOrNull.trim());
		}
		if (StringUtils.hasText(trueNameTrimOrNull)) {
			w.eq(RegistrationRecord::getTrueName, trueNameTrimOrNull.trim());
		}
		applyIsWhiteListFilter(w, isWhiteListRaw);
		if (StringUtils.hasText(startTimeRaw)) {
			w.ge(RegistrationRecord::getCreated, parseEpochSecondsStrict(startTimeRaw.trim()));
		}
		if (StringUtils.hasText(endTimeRaw)) {
			w.le(RegistrationRecord::getCreated, parseEpochSecondsStrict(endTimeRaw.trim()));
		}
		return w;
	}

	private static void applyIsWhiteListFilter(
			LambdaQueryWrapper<RegistrationRecord> w, String isWhiteListRaw) {
		if (!StringUtils.hasText(isWhiteListRaw)) {
			return;
		}
		int intval;
		try {
			intval = Integer.parseInt(isWhiteListRaw.trim());
		} catch (NumberFormatException ex) {
			return;
		}
		if (intval == 0) {
			return;
		}
		long value = intval == 1 ? 1L : 0L;
		w.eq(RegistrationRecord::getIsWhiteList, value);
	}

	private int parseEpochSecondsStrict(String raw) {
		if (!raw.matches("^-?\\d+$")) {
			throw new BadRequestException("时间参数格式错误");
		}
		try {
			long v = Long.parseLong(raw);
			if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
				throw new BadRequestException("时间参数格式错误");
			}
			return (int) v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("时间参数格式错误");
		}
	}

	private String formatActivityListStartTimeDisplay(int epochSec) {
		if (epochSec <= 0) {
			return "";
		}
		ZonedDateTime zdt = Instant.ofEpochSecond(epochSec).atZone(CN);
		String weekCn =
				switch (zdt.getDayOfWeek()) {
					case MONDAY -> "一";
					case TUESDAY -> "二";
					case WEDNESDAY -> "三";
					case THURSDAY -> "四";
					case FRIDAY -> "五";
					case SATURDAY -> "六";
					case SUNDAY -> "日";
				};
		return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
				+ " "
				+ weekCn
				+ " "
				+ zdt.format(DateTimeFormatter.ofPattern("HH:mm"));
	}

	private JsonNode parseContentArray(String contentJson) {
		try {
			if (!StringUtils.hasText(contentJson)) {
				return objectMapper.createArrayNode();
			}
			JsonNode n = objectMapper.readTree(contentJson);
			if (n.isArray()) {
				return n;
			}
			return objectMapper.createArrayNode();
		} catch (Exception e) {
			return objectMapper.createArrayNode();
		}
	}

	private String decryptToString(String encrypted) {
		Object d = sensitiveFieldEncryptor.decrypt(encrypted == null ? "" : encrypted);
		return d == null ? "" : String.valueOf(d);
	}

	private static String formatRecordNo(Long recordNo) {
		long n = recordNo != null ? recordNo : 0L;
		String s = Long.toString(n);
		if (s.length() >= 4) {
			return s;
		}
		return "0".repeat(4 - s.length()) + s;
	}

	private String formatActivityDate(Integer epochSec) {
		if (epochSec == null || epochSec == 0) {
			return "";
		}
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSec.longValue()), CN).format(DATE_TIME_FMT);
	}

	private void buildAreaName(Map<String, Object> activityInfo) {
		Object areaObj = activityInfo.get("area");
		String areaRaw = areaObj == null ? "" : String.valueOf(areaObj);
		if (!StringUtils.hasText(areaRaw)) {
			activityInfo.put("area_name", "");
			return;
		}
		List<String> idTokens = splitCommaPreserveOrder(areaRaw);
		List<Long> ids = new ArrayList<>();
		for (String t : idTokens) {
			try {
				ids.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		if (ids.isEmpty()) {
			activityInfo.put("area_name", "");
			return;
		}
		List<Address> rows = addressMapper.selectList(new LambdaQueryWrapper<Address>().in(Address::getId, ids));
		List<String> labels = new ArrayList<>();
		for (Address a : rows) {
			labels.add(a.getLabel() != null ? a.getLabel() : "");
		}
		activityInfo.put("area_name", String.join("", labels));
	}

	private static List<String> splitCommaPreserveOrder(String raw) {
		List<String> out = new ArrayList<>();
		if (raw == null || raw.isEmpty()) {
			return out;
		}
		String[] parts = raw.split(",", -1);
		for (String p : parts) {
			String t = p.trim();
			if (StringUtils.hasText(t)) {
				out.add(t);
			}
		}
		return out;
	}

	private String statusNameForRecord(String status, Locale locale) {
		if (!StringUtils.hasText(status)) {
			return "";
		}
		String key =
				switch (status.trim()) {
					case "pending" -> MSG_REC_PENDING;
					case "passed" -> MSG_REC_PASSED;
					case "rejected" -> MSG_REC_REJECTED;
					case "verified" -> MSG_REC_VERIFIED;
					case "canceled" -> MSG_REC_CANCELED;
					default -> null;
				};
		if (key == null) {
			return status;
		}
		return messageSource.getMessage(key, null, locale);
	}
}
