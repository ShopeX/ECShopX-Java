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
import cn.shopex.ecshopx.selfservice.domain.FormTemplate;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.FormTemplateMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.FormTemplateOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationRecordOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.support.RegistrationActivityFrontSubmitMessageKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class RegistrationActivityFrontGetRegistrationActivityService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String MSG_STATUS_WAITING = "selfservice.registration_activity.status_waiting";
	private static final String MSG_STATUS_ENDED = "selfservice.registration_activity.status_ended";
	private static final String MSG_STATUS_ONGOING = "selfservice.registration_activity.status_ongoing";

	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationRecordMapper registrationRecordMapper;
	private final FormTemplateMapper formTemplateMapper;
	private final FormTemplateApiRowAssembler formTemplateApiRowAssembler;
	private final RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService;
	private final RegistrationRecordOutsideMultiLangReadService registrationRecordOutsideMultiLangReadService;
	private final FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService;
	private final MessageSource messageSource;

	public RegistrationActivityFrontGetRegistrationActivityService(
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationRecordMapper registrationRecordMapper,
			FormTemplateMapper formTemplateMapper,
			FormTemplateApiRowAssembler formTemplateApiRowAssembler,
			RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService,
			RegistrationRecordOutsideMultiLangReadService registrationRecordOutsideMultiLangReadService,
			FormTemplateOutsideMultiLangReadService formTemplateOutsideMultiLangReadService,
			MessageSource messageSource) {
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationRecordMapper = registrationRecordMapper;
		this.formTemplateMapper = formTemplateMapper;
		this.formTemplateApiRowAssembler = formTemplateApiRowAssembler;
		this.registrationActivityOutsideMultiLangReadService = registrationActivityOutsideMultiLangReadService;
		this.registrationRecordOutsideMultiLangReadService = registrationRecordOutsideMultiLangReadService;
		this.formTemplateOutsideMultiLangReadService = formTemplateOutsideMultiLangReadService;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getRegistrationActivity(
			Locale locale, String requestLangTag, long companyId, long userId, long activityId) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_join_num", 0);

		RegistrationActivity entity =
				registrationActivityMapper.selectOne(new LambdaQueryWrapper<RegistrationActivity>()
						.eq(RegistrationActivity::getCompanyId, companyId)
						.eq(RegistrationActivity::getActivityId, activityId));
		if (entity == null) {
			throw new ResourceException(
					messageSource.getMessage(RegistrationActivityFrontSubmitMessageKeys.ACTIVITY_NOT_EXIST_ERR, null, locale));
		}

		LinkedHashMap<String, Object> activityInfoMap = toFrontActivityDetailMap(entity);

		Long ownerCid = entity.getCompanyId();
		long aid = entity.getActivityId() != null ? entity.getActivityId() : 0L;
		if (ownerCid != null && ownerCid > 0L && aid > 0L) {
			registrationActivityOutsideMultiLangReadService.applyDetailOutsideLangOverrides(
					ownerCid, aid, activityInfoMap, requestLangTag);
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		Integer st = (Integer) activityInfoMap.get("start_time");
		Integer et = (Integer) activityInfoMap.get("end_time");
		int stVal = st != null ? st : 0;
		int etVal = et != null ? et : 0;
		String statusCode;
		String msgKey;
		if (stVal > now) {
			statusCode = "waiting";
			msgKey = MSG_STATUS_WAITING;
		} else if (etVal < now) {
			statusCode = "end";
			msgKey = MSG_STATUS_ENDED;
		} else {
			statusCode = "ongoing";
			msgKey = MSG_STATUS_ONGOING;
		}
		activityInfoMap.put("status", statusCode);
		activityInfoMap.put("status_name", messageSource.getMessage(msgKey, null, locale));

		activityInfoMap.put("start_date", formatActivityDate(entity.getStartTime()));
		activityInfoMap.put("end_date", formatActivityDate(entity.getEndTime()));

		if (userId > 0L) {
			List<RegistrationRecord> records = registrationRecordMapper.selectList(
					new LambdaQueryWrapper<RegistrationRecord>()
							.eq(RegistrationRecord::getActivityId, aid)
							.eq(RegistrationRecord::getUserId, userId)
							.select(
									RegistrationRecord::getRecordId,
									RegistrationRecord::getStatus,
									RegistrationRecord::getReason,
									RegistrationRecord::getRemark,
									RegistrationRecord::getCreated));
			List<Map<String, Object>> recordRows = new ArrayList<>();
			for (RegistrationRecord record : records) {
				LinkedHashMap<String, Object> row = new LinkedHashMap<>();
				row.put("record_id", record.getRecordId());
				row.put("status", record.getStatus());
				row.put("reason", record.getReason());
				String remark = record.getRemark();
				row.put("remark", remark != null ? remark : "");
				Integer created = record.getCreated();
				row.put("created", created);
				if (created != null && created != 0) {
					row.put(
							"created_date",
							LocalDateTime.ofInstant(Instant.ofEpochSecond(created.longValue()), CN).format(DATE_TIME_FMT));
				}
				long recId = record.getRecordId() == null ? 0L : record.getRecordId();
				registrationRecordOutsideMultiLangReadService.applyRecordLangOverrides(companyId, recId, row, requestLangTag);
				recordRows.add(row);
			}
			activityInfoMap.put("record_info", recordRows);
		}

		Integer jl = entity.getJoinLimit();
		boolean joinLimitTruthy = jl != null && jl != 0;
		if (joinLimitTruthy) {
			long cnt = registrationRecordMapper.selectCount(new LambdaQueryWrapper<RegistrationRecord>()
					.eq(RegistrationRecord::getCompanyId, companyId)
					.eq(RegistrationRecord::getActivityId, aid));
			result.put("total_join_num", (int) Math.min(cnt, Integer.MAX_VALUE));
		}

		Long tid = entity.getTempId();
		boolean tempTruthy = tid != null && tid != 0L;
		if (tempTruthy) {
			FormTemplate tpl = formTemplateMapper.selectOne(new LambdaQueryWrapper<FormTemplate>()
					.eq(FormTemplate::getCompanyId, companyId)
					.eq(FormTemplate::getId, tid));
			if (tpl == null) {
				activityInfoMap.put("formdata", Collections.emptyList());
			} else {
				Map<String, Object> formRow = formTemplateApiRowAssembler.toRow(tpl);
				formTemplateOutsideMultiLangReadService.applyListLangOverrides(companyId, List.of(formRow), requestLangTag);
				Object content = formRow.get("content");
				if (content instanceof String s) {
					formTemplateApiRowAssembler
							.tryDecodeMultilangJsonField(s)
							.ifPresent(v -> formRow.put("content", v));
				}
				activityInfoMap.put("formdata", formRow);
			}
		}

		result.put("activity_info", activityInfoMap);
		return result;
	}

	private static LinkedHashMap<String, Object> toFrontActivityDetailMap(RegistrationActivity entity) {
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

	private static String formatActivityDate(Integer epochSec) {
		if (epochSec == null || epochSec == 0) {
			return "";
		}
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSec.longValue()), CN).format(DATE_TIME_FMT);
	}

	private void sortFormTemplateContentByFieldKey(Map<String, Object> formRow) {
		if (formRow.containsKey("content")) {
			formRow.put("content", sortValueTree(formRow.get("content")));
		}
		if (formRow.containsKey("formdata")) {
			formRow.put("formdata", sortValueTree(formRow.get("formdata")));
		}
	}

	@SuppressWarnings("unchecked")
	private static Object sortValueTree(Object o) {
		if (o instanceof Map<?, ?> raw) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : raw.entrySet()) {
				m.put(String.valueOf(e.getKey()), sortValueTree(e.getValue()));
			}
			return m;
		}
		if (o instanceof List<?> rawList) {
			List<Object> items = new ArrayList<>();
			for (Object x : rawList) {
				items.add(sortValueTree(x));
			}
			sortListLayerInPlace(items);
			return items;
		}
		return o;
	}

	private static void sortListLayerInPlace(List<Object> items) {
		boolean anyMapHasSort = false;
		for (Object x : items) {
			if (x instanceof Map<?, ?> m && m.containsKey("sort")) {
				anyMapHasSort = true;
				break;
			}
		}
		if (!anyMapHasSort) {
			return;
		}
		List<Map<String, Object>> maps = new ArrayList<>();
		List<Object> nonMaps = new ArrayList<>();
		for (Object x : items) {
			if (x instanceof Map<?, ?> mm) {
				maps.add((Map<String, Object>) mm);
			} else {
				nonMaps.add(x);
			}
		}
		maps.sort(Comparator.comparingInt(m -> ((Number) m.getOrDefault("sort", 0)).intValue()));
		items.clear();
		items.addAll(maps);
		items.addAll(nonMaps);
	}
}
