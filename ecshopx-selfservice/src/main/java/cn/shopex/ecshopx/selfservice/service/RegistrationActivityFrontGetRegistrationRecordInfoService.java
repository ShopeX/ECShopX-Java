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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.espier.domain.Address;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationRecordOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.support.RegistrationActivityFrontRegistrationRecordInfoMessageKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationActivityFrontGetRegistrationRecordInfoService {

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
	private final RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler;
	private final RegistrationRecordOutsideMultiLangReadService registrationRecordOutsideMultiLangReadService;
	private final RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService;
	private final AddressMapper addressMapper;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public RegistrationActivityFrontGetRegistrationRecordInfoService(
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationRecordRowMapAssembler registrationRecordRowMapAssembler,
			RegistrationRecordOutsideMultiLangReadService registrationRecordOutsideMultiLangReadService,
			RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService,
			AddressMapper addressMapper,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationRecordRowMapAssembler = registrationRecordRowMapAssembler;
		this.registrationRecordOutsideMultiLangReadService = registrationRecordOutsideMultiLangReadService;
		this.registrationActivityOutsideMultiLangReadService = registrationActivityOutsideMultiLangReadService;
		this.addressMapper = addressMapper;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getRegistrationRecordInfo(
			Locale locale, String requestLangTag, long authUserId, long recordId) {
		RegistrationRecord entity = registrationRecordMapper.selectById(recordId);
		Long recordUserId = entity == null ? null : entity.getUserId();
		if (recordUserId == null || recordUserId.longValue() != authUserId) {
			throw new ResourceException(
					messageSource.getMessage(
							RegistrationActivityFrontRegistrationRecordInfoMessageKeys.INFORMATION_ERROR, null, locale));
		}

		Map<String, Object> row = registrationRecordRowMapAssembler.toApiRow(entity);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(row);
		out.put("record_no", formatRecordNo(entity.getRecordNo()));

		long recordOwnerCompanyId = entity.getCompanyId() == null ? 0L : entity.getCompanyId();
		long recId = entity.getRecordId() == null ? 0L : entity.getRecordId();
		registrationRecordOutsideMultiLangReadService.applyRecordLangOverrides(
				recordOwnerCompanyId, recId, out, requestLangTag);

		out.put("content", parseContentArray(entity.getContent()));

		Long aid = entity.getActivityId();
		RegistrationActivity act = null;
		if (aid != null && aid > 0L) {
			act = registrationActivityMapper.selectById(aid);
		}
		long aidPositive = aid != null && aid > 0L ? aid : 0L;

		LinkedHashMap<String, Object> activityInfo = new LinkedHashMap<>();

		if (act != null) {
			out.put("activity_name", act.getActivityName());
			out.put("start_time", act.getStartTime());
			out.put("end_time", act.getEndTime());
			Integer jl = act.getJoinLimit();
			out.put("join_limit", jl == null ? 1 : jl);
			out.put("is_sms_notice", act.getIsSmsNotice());
			out.put("is_wxapp_notice", act.getIsWxappNotice());
			out.put("start_date", formatActivityDate(act.getStartTime()));
			out.put("end_date", formatActivityDate(act.getEndTime()));

			activityInfo.put("is_offline_verify", act.getIsOfflineVerify());
			activityInfo.put("is_allow_duplicate", act.getIsAllowDuplicate());
			activityInfo.put("address", act.getAddress());
			activityInfo.put("place", act.getPlace());
			activityInfo.put("intro", act.getIntro());
			activityInfo.put("pics", act.getPics());

			int now = (int) (System.currentTimeMillis() / 1000L);
			Integer st = act.getStartTime();
			Integer et = act.getEndTime();
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

			activityInfo.put("area", act.getArea());
			buildAreaName(activityInfo);

			Long ownerCid = act.getCompanyId();
			if (ownerCid != null && ownerCid > 0L && aidPositive > 0L) {
				registrationActivityOutsideMultiLangReadService.applyDetailOutsideLangOverrides(
						ownerCid.longValue(), aidPositive, activityInfo, requestLangTag);
				registrationActivityOutsideMultiLangReadService.applyActivityNameOverrides(
						ownerCid.longValue(), List.of(out), requestLangTag);
			}
		} else {
			out.put("activity_name", "");
			out.put("start_time", 0);
			out.put("end_time", 0);
			out.put("join_limit", 0);
			out.put("is_sms_notice", null);
			out.put("is_wxapp_notice", null);
			out.put("start_date", "");
			out.put("end_date", "");
		}

		out.put("activity_info", activityInfo);
		out.put("status_name", statusNameForRecord(entity.getStatus(), locale));

		int nowEpochSec = (int) (System.currentTimeMillis() / 1000L);
		Integer topStart = (Integer) out.get("start_time");
		Integer topEnd = (Integer) out.get("end_time");
		Object isDupRaw = activityInfo.get("is_allow_duplicate");
		out.put(
				"action",
				buildRecordAction(nowEpochSec, topStart, topEnd, entity.getStatus(), entity.getFormId(), isDupRaw));

		return out;
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

	private static Map<String, Object> buildRecordAction(
			int nowEpochSec,
			Integer startTime,
			Integer endTime,
			String status,
			Long formId,
			Object isAllowDuplicateRaw) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		if ((startTime != null && startTime > nowEpochSec) || (endTime != null && endTime < nowEpochSec)) {
			m.put("cancel", 0);
			m.put("edit", 0);
			m.put("apply", 0);
			return m;
		}
		String st = status == null ? "" : status.trim();
		int cancel = ("pending".equals(st) || "passed".equals(st)) ? 1 : 0;
		int edit = (formId != null && formId > 0L && ("pending".equals(st) || "rejected".equals(st))) ? 1 : 0;
		boolean dup =
				(isAllowDuplicateRaw instanceof Boolean b && b)
						|| (isAllowDuplicateRaw instanceof Number n && n.intValue() != 0)
						|| (isAllowDuplicateRaw instanceof String s
								&& !s.isBlank()
								&& !"0".equals(s.trim()));
		int apply = dup ? 1 : 0;
		m.put("cancel", cancel);
		m.put("edit", edit);
		m.put("apply", apply);
		return m;
	}
}
