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
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.espier.domain.Address;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationRecordOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.support.RegistrationActivityFrontRegistrationRecordListMessageKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationActivityFrontGetRegistrationRecordListService {

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

	public RegistrationActivityFrontGetRegistrationRecordListService(
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

	public Map<String, Object> getRegistrationRecordList(
			Locale locale,
			String requestLangTag,
			long authCompanyId,
			long authUserId,
			String statusRaw,
			int page,
			int pageSize,
			String activityIdRaw) {
		LambdaQueryWrapper<RegistrationRecord> w = new LambdaQueryWrapper<>();
		w.eq(RegistrationRecord::getCompanyId, authCompanyId);
		w.eq(RegistrationRecord::getUserId, authUserId);

		if (isTruthyQueryParamRaw(activityIdRaw)) {
			long aid = LeadingNumberParser.parseAsLong(activityIdRaw.trim());
			if (aid > 0L) {
				w.eq(RegistrationRecord::getActivityId, aid);
			}
		}

		if (isTruthyQueryParamRaw(statusRaw)) {
			String display = recordStatusDisplayName(statusRaw, locale);
			if (statusRaw.equals(display)) {
				throw new ResourceException(
						messageSource.getMessage(
								RegistrationActivityFrontRegistrationRecordListMessageKeys.PARAMETER_ERROR,
								new Object[] {statusRaw},
								locale));
			}
			w.eq(RegistrationRecord::getStatus, statusRaw);
		}

		w.orderByDesc(RegistrationRecord::getRecordId);

		Page<RegistrationRecord> mpPage = new Page<>(page, pageSize);
		registrationRecordMapper.selectPage(mpPage, w);
		List<RegistrationRecord> records = mpPage.getRecords();
		int totalCount = (int) Math.min(mpPage.getTotal(), Integer.MAX_VALUE);

		if (records == null || records.isEmpty()) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", totalCount);
			empty.put("list", Collections.emptyList());
			return empty;
		}

		List<Long> activityIds =
				records.stream()
						.map(RegistrationRecord::getActivityId)
						.filter(Objects::nonNull)
						.filter(id -> id > 0L)
						.distinct()
						.collect(Collectors.toList());

		Map<Long, RegistrationActivity> actById = new LinkedHashMap<>();
		if (!activityIds.isEmpty()) {
			List<RegistrationActivity> acts = registrationActivityMapper.selectBatchIds(activityIds);
			if (acts != null) {
				for (RegistrationActivity a : acts) {
					if (a != null && a.getActivityId() != null) {
						actById.put(a.getActivityId(), a);
					}
				}
			}
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		List<Map<String, Object>> list = new ArrayList<>(records.size());

		for (RegistrationRecord entity : records) {
			Map<String, Object> row = new LinkedHashMap<>(registrationRecordRowMapAssembler.toApiRow(entity));
			RegistrationActivity act = actById.get(entity.getActivityId());

			LinkedHashMap<String, Object> activityInfo = new LinkedHashMap<>();
			if (act != null) {
				activityInfo.put("address", act.getAddress());
				activityInfo.put("place", act.getPlace());
				activityInfo.put("is_allow_duplicate", act.getIsAllowDuplicate());
				activityInfo.put("intro", act.getIntro());
				activityInfo.put("pics", act.getPics());
				Integer stAct = act.getStartTime();
				int stVal = stAct != null ? stAct : 0;
				activityInfo.put("start_time", formatListActivityStartTimeCn(stVal));

				Integer st = act.getStartTime();
				Integer et = act.getEndTime();
				int stCmp = st != null ? st : 0;
				int etCmp = et != null ? et : 0;
				String msgKey;
				if (stCmp > now) {
					msgKey = MSG_STATUS_WAITING;
				} else if (etCmp < now) {
					msgKey = MSG_STATUS_ENDED;
				} else {
					msgKey = MSG_STATUS_ONGOING;
				}
				activityInfo.put("status_name", messageSource.getMessage(msgKey, null, locale));

				activityInfo.put("area", act.getArea());
				putAreaName(activityInfo);

				row.put("activity_info", activityInfo);
				row.put("activity_name", act.getActivityName());
				row.put("start_time", act.getStartTime());
				row.put("end_time", act.getEndTime());
				row.put("start_date", formatActivityDate(act.getStartTime()));
				row.put("end_date", formatActivityDate(act.getEndTime()));
				Integer jl = act.getJoinLimit();
				row.put("join_limit", jl == null ? 1 : jl);
				Boolean sms = act.getIsSmsNotice();
				row.put("is_sms_notice", sms != null && sms);
				Boolean wx = act.getIsWxappNotice();
				row.put("is_wxapp_notice", wx != null && wx);

				Long ownerCid = act.getCompanyId();
				long aidPositive = entity.getActivityId() != null && entity.getActivityId() > 0L
						? entity.getActivityId()
						: 0L;
				if (ownerCid != null && ownerCid > 0L && aidPositive > 0L) {
					registrationActivityOutsideMultiLangReadService.applyActivityNameOverrides(
							ownerCid.longValue(), List.of(row), requestLangTag);
				}
			} else {
				row.put("activity_name", "");
				row.put("start_time", 0);
				row.put("end_time", 0);
				row.put("join_limit", 0);
				row.put("is_sms_notice", false);
				row.put("is_wxapp_notice", false);
				row.put("start_date", "");
				row.put("end_date", "");
				row.put("activity_info", activityInfo);
			}

			row.put("record_no", formatRecordNo(entity.getRecordNo()));
			row.put("content", parseContentArray(entity.getContent()));
			String createDateStr = formatCreateDate(entity.getCreated());
			row.put("create_date", createDateStr);
			row.put("created_date", createDateStr);
			row.put("status_name", recordStatusDisplayName(entity.getStatus(), locale));

			long recId = entity.getRecordId() == null ? 0L : entity.getRecordId();
			Long recordCompanyId = entity.getCompanyId();
			long recordOwnerCompanyId =
					(recordCompanyId != null && recordCompanyId > 0L) ? recordCompanyId : authCompanyId;
			registrationRecordOutsideMultiLangReadService.applyRecordLangOverrides(
					recordOwnerCompanyId, recId, row, requestLangTag);

			int nowEpochSec = (int) (System.currentTimeMillis() / 1000L);
			Integer topStart = (Integer) row.get("start_time");
			Integer topEnd = (Integer) row.get("end_time");
			Object isDupRaw = null;
			Object activityInfoObj = row.get("activity_info");
			if (activityInfoObj instanceof Map<?, ?> aiMap) {
				isDupRaw = aiMap.get("is_allow_duplicate");
			}
			row.put(
					"action",
					buildRecordAction(nowEpochSec, topStart, topEnd, entity.getStatus(), entity.getFormId(), isDupRaw));

			list.add(row);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", list);
		return out;
	}

	private static boolean isTruthyQueryParamRaw(String raw) {
		if (raw == null) {
			return false;
		}
		if (raw.isEmpty()) {
			return false;
		}
		if ("0".equals(raw)) {
			return false;
		}
		return true;
	}

	private String recordStatusDisplayName(String statusCode, Locale locale) {
		if (!StringUtils.hasText(statusCode)) {
			return "";
		}
		String key =
				switch (statusCode.trim()) {
					case "pending" -> MSG_REC_PENDING;
					case "passed" -> MSG_REC_PASSED;
					case "rejected" -> MSG_REC_REJECTED;
					case "verified" -> MSG_REC_VERIFIED;
					case "canceled" -> MSG_REC_CANCELED;
					default -> null;
				};
		if (key == null) {
			return statusCode;
		}
		return messageSource.getMessage(key, null, locale);
	}

	private String formatListActivityStartTimeCn(int epochSec) {
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

	private static String formatRecordNo(Long recordNo) {
		long n = recordNo != null ? recordNo : 0L;
		String s = Long.toString(n);
		if (s.length() >= 4) {
			return s;
		}
		return "0".repeat(4 - s.length()) + s;
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

	private static String formatActivityDate(Integer epochSec) {
		if (epochSec == null || epochSec == 0) {
			return "";
		}
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSec.longValue()), CN).format(DATE_TIME_FMT);
	}

	private static String formatCreateDate(Integer created) {
		if (created == null || created == 0) {
			return "";
		}
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(created.longValue()), CN).format(DATE_TIME_FMT);
	}

	private void putAreaName(Map<String, Object> activityInfo) {
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
		List<Address> addrRows = addressMapper.selectList(new LambdaQueryWrapper<Address>().in(Address::getId, ids));
		List<String> labels = new ArrayList<>();
		for (Address a : addrRows) {
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
