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

import cn.shopex.ecshopx.espier.domain.Address;
import cn.shopex.ecshopx.espier.mapper.AddressMapper;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationRecord;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationRecordOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationActivityFrontGetRegistrationActivityListService {

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String MSG_STATUS_WAITING = "selfservice.registration_activity.status_waiting";
	private static final String MSG_STATUS_ENDED = "selfservice.registration_activity.status_ended";
	private static final String MSG_STATUS_ONGOING = "selfservice.registration_activity.status_ongoing";

	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationRecordMapper registrationRecordMapper;
	private final RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService;
	private final RegistrationRecordOutsideMultiLangReadService registrationRecordOutsideMultiLangReadService;
	private final AddressMapper addressMapper;
	private final MessageSource messageSource;

	public RegistrationActivityFrontGetRegistrationActivityListService(
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationRecordMapper registrationRecordMapper,
			RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService,
			RegistrationRecordOutsideMultiLangReadService registrationRecordOutsideMultiLangReadService,
			AddressMapper addressMapper,
			MessageSource messageSource) {
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationRecordMapper = registrationRecordMapper;
		this.registrationActivityOutsideMultiLangReadService = registrationActivityOutsideMultiLangReadService;
		this.registrationRecordOutsideMultiLangReadService = registrationRecordOutsideMultiLangReadService;
		this.addressMapper = addressMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getRegistrationActivityList(
			Locale locale,
			String requestLangTag,
			long companyId,
			long userId,
			int page,
			int pageSize,
			int status,
			String activityNameTrimmed) {
		int now = (int) (System.currentTimeMillis() / 1000L);

		LambdaQueryWrapper<RegistrationActivity> w = new LambdaQueryWrapper<>();
		w.eq(RegistrationActivity::getCompanyId, companyId);
		if (status == 1) {
			w.ge(RegistrationActivity::getEndTime, now);
		} else if (status == 2) {
			w.le(RegistrationActivity::getEndTime, now);
		}
		if (StringUtils.hasText(activityNameTrimmed)) {
			w.apply("activity_name LIKE CONCAT('%', {0}, '%')", activityNameTrimmed);
		}
		w.orderByDesc(RegistrationActivity::getActivityId);

		long total = registrationActivityMapper.selectCount(w);
		Page<RegistrationActivity> mpPage = new Page<>(page, pageSize, false);
		registrationActivityMapper.selectPage(mpPage, w);
		List<RegistrationActivity> records = mpPage.getRecords();
		int totalCount = (int) Math.min(total, Integer.MAX_VALUE);

		if (records == null || records.isEmpty()) {
			LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", totalCount);
			empty.put("list", Collections.emptyList());
			return empty;
		}

		List<Map<String, Object>> rows = new ArrayList<>(records.size());
		for (RegistrationActivity entity : records) {
			LinkedHashMap<String, Object> row = toFrontActivityListRowMap(entity);
			row.put("start_date", formatActivityDate(entity.getStartTime()));
			row.put("end_date", formatActivityDate(entity.getEndTime()));
			rows.add(row);
		}

		registrationActivityOutsideMultiLangReadService.applyActivityNameOverrides(companyId, rows, requestLangTag);

		for (int i = 0; i < records.size(); i++) {
			RegistrationActivity entity = records.get(i);
			Map<String, Object> row = rows.get(i);

			putAreaName(row);

			Integer st = (Integer) row.get("start_time");
			Integer et = (Integer) row.get("end_time");
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
			row.put("status", statusCode);
			row.put("status_name", messageSource.getMessage(msgKey, null, locale));

			Integer jl = entity.getJoinLimit();
			if (jl != null && jl != 0) {
				long activityId = entity.getActivityId() != null ? entity.getActivityId() : 0L;
				long cnt = registrationRecordMapper.selectCount(
						new LambdaQueryWrapper<RegistrationRecord>().eq(RegistrationRecord::getActivityId, activityId));
				row.put("total_join_num", (int) Math.min(cnt, Integer.MAX_VALUE));
			}

			if (userId > 0L) {
				long activityId = entity.getActivityId() != null ? entity.getActivityId() : 0L;
				List<RegistrationRecord> recs = registrationRecordMapper.selectList(
						new LambdaQueryWrapper<RegistrationRecord>()
								.eq(RegistrationRecord::getActivityId, activityId)
								.eq(RegistrationRecord::getUserId, userId)
								.select(
										RegistrationRecord::getRecordId,
										RegistrationRecord::getStatus,
										RegistrationRecord::getReason,
										RegistrationRecord::getRemark,
										RegistrationRecord::getCompanyId,
										RegistrationRecord::getCreated));
				List<Map<String, Object>> recordRows = new ArrayList<>();
				for (RegistrationRecord record : recs) {
					LinkedHashMap<String, Object> recRow = new LinkedHashMap<>();
					recRow.put("record_id", record.getRecordId());
					recRow.put("status", record.getStatus());
					recRow.put("reason", record.getReason());
					// Same shape as admin/detail record payloads: expose DB column; multilang may replace non-blank values.
					recRow.put("remark", record.getRemark());
					Integer created = record.getCreated();
					recRow.put("created", created);
					if (created != null && created != 0) {
						recRow.put(
								"created_date",
								LocalDateTime.ofInstant(Instant.ofEpochSecond(created.longValue()), CN).format(DATE_TIME_FMT));
					}
					long recId = record.getRecordId() == null ? 0L : record.getRecordId();
					Long recordCompanyId = record.getCompanyId();
					long recordOwnerCompanyId =
							(recordCompanyId != null && recordCompanyId > 0L) ? recordCompanyId : companyId;
					registrationRecordOutsideMultiLangReadService.applyRecordLangOverrides(
							recordOwnerCompanyId, recId, recRow, requestLangTag);
					recordRows.add(recRow);
				}
				row.put("record_info", recordRows);
			}
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", rows);
		return out;
	}

	private void putAreaName(Map<String, Object> row) {
		Object areaObj = row.get("area");
		String areaRaw = areaObj == null ? "" : String.valueOf(areaObj);
		if (!StringUtils.hasText(areaRaw)) {
			row.put("area_name", "");
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
			row.put("area_name", "");
			return;
		}
		List<Address> addrRows = addressMapper.selectList(new LambdaQueryWrapper<Address>().in(Address::getId, ids));
		List<String> labels = new ArrayList<>();
		for (Address a : addrRows) {
			labels.add(a.getLabel() != null ? a.getLabel() : "");
		}
		row.put("area_name", String.join("", labels));
	}

	private static LinkedHashMap<String, Object> toFrontActivityListRowMap(RegistrationActivity entity) {
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

	private static List<String> splitCommaPreserveOrder(String raw) {
		if (raw == null || !StringUtils.hasText(raw)) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (String p : raw.split(",")) {
			String t = p.trim();
			if (StringUtils.hasText(t)) {
				out.add(t);
			}
		}
		return out;
	}
}
