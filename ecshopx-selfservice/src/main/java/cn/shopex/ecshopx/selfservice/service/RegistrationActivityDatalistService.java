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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivity;
import cn.shopex.ecshopx.selfservice.domain.RegistrationActivityRelShop;
import cn.shopex.ecshopx.selfservice.dto.admin.v1.ActivityUserJoinCountRow;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationActivityRelShopMapper;
import cn.shopex.ecshopx.selfservice.mapper.RegistrationRecordMapper;
import cn.shopex.ecshopx.selfservice.service.multilang.RegistrationActivityOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
public class RegistrationActivityDatalistService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 20;

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String MSG_STATUS_WAITING = "selfservice.registration_activity.status_waiting";
	private static final String MSG_STATUS_ENDED = "selfservice.registration_activity.status_ended";
	private static final String MSG_STATUS_ONGOING = "selfservice.registration_activity.status_ongoing";

	private final RegistrationActivityMapper registrationActivityMapper;
	private final RegistrationActivityRelShopMapper registrationActivityRelShopMapper;
	private final DistributorMapper distributorMapper;
	private final RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService;
	private final RegistrationRecordMapper registrationRecordMapper;
	private final MessageSource messageSource;

	public RegistrationActivityDatalistService(
			RegistrationActivityMapper registrationActivityMapper,
			RegistrationActivityRelShopMapper registrationActivityRelShopMapper,
			DistributorMapper distributorMapper,
			RegistrationActivityOutsideMultiLangReadService registrationActivityOutsideMultiLangReadService,
			RegistrationRecordMapper registrationRecordMapper,
			MessageSource messageSource) {
		this.registrationActivityMapper = registrationActivityMapper;
		this.registrationActivityRelShopMapper = registrationActivityRelShopMapper;
		this.distributorMapper = distributorMapper;
		this.registrationActivityOutsideMultiLangReadService = registrationActivityOutsideMultiLangReadService;
		this.registrationRecordMapper = registrationRecordMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getDatalist(
			long companyId,
			String requestLangTag,
			Locale locale,
			String pageRaw,
			String pageSizeRaw,
			String startTimeRaw,
			String endTimeRaw,
			String statusRaw,
			String isValidRaw,
			String distributorIdRaw,
			String fieldTitleRaw) {
		Map<String, Object> empty = new LinkedHashMap<>();
		empty.put("total_count", 0);
		empty.put("list", Collections.emptyList());

		int page = parsePage(pageRaw);
		int pageSize = parsePageSize(pageSizeRaw);
		long distributorIdParsed = parseDistributorIdIntvalStyle(distributorIdRaw);
		int now = (int) (System.currentTimeMillis() / 1000L);

		LambdaQueryWrapper<RegistrationActivity> w = new LambdaQueryWrapper<>();
		w.eq(RegistrationActivity::getCompanyId, companyId);

		applyStatusFilter(w, statusRaw, now);

		if (isTruthyIsValid(isValidRaw)) {
			w.le(RegistrationActivity::getStartTime, now);
			w.ge(RegistrationActivity::getEndTime, now);
		}

		if (isTruthyQueryTimeParam(startTimeRaw) && isTruthyQueryTimeParam(endTimeRaw)) {
			Integer startSec = parseEpochSecondOrNull(startTimeRaw);
			Integer endSec = parseEpochSecondOrNull(endTimeRaw);
			if (startSec != null && endSec != null) {
				w.ge(RegistrationActivity::getCreated, startSec);
				w.le(RegistrationActivity::getCreated, endSec);
			}
		}

		if (distributorIdParsed > 0) {
			List<Long> firstIds = queryActivityIdsByDistributor(distributorIdParsed);
			if (firstIds.isEmpty()) {
				return empty;
			}
			w.in(RegistrationActivity::getActivityId, firstIds);
		}

		String ft = fieldTitleRaw == null ? "" : fieldTitleRaw.trim();
		if (StringUtils.hasText(ft)) {
			w.like(RegistrationActivity::getActivityName, "%" + escapeLike(ft) + "%");
		}

		if (distributorIdParsed > 0) {
			List<Long> secondIds = queryActivityIdsByDistributor(distributorIdParsed);
			if (secondIds.isEmpty()) {
				return empty;
			}
			w.in(RegistrationActivity::getActivityId, secondIds);
		}

		long total = registrationActivityMapper.selectCount(w.clone());
		w.orderByDesc(RegistrationActivity::getActivityId);
		long offset = ((long) page - 1L) * (long) pageSize;
		List<RegistrationActivity> entities =
				registrationActivityMapper.selectList(w.last("LIMIT " + pageSize + " OFFSET " + offset));

		ZoneId zone = ZoneId.systemDefault();
		List<Map<String, Object>> listMaps = new ArrayList<>(entities.size());
		for (RegistrationActivity e : entities) {
			listMaps.add(activityToRow(e, zone));
		}

		if (!listMaps.isEmpty()) {
			registrationActivityOutsideMultiLangReadService.applyActivityNameOverrides(
					companyId, listMaps, requestLangTag);
		}

		Map<Long, List<Long>> activityRelShopsByActivityId = new LinkedHashMap<>();
		List<Long> distributorIdsForQuery = new ArrayList<>();

		// Load rel shops for current page so distributor_name lists all linked shops even when
		// the main query is narrowed by distributor_id.
		if (!listMaps.isEmpty()) {
			List<Long> activityIds = new ArrayList<>(listMaps.size());
			for (Map<String, Object> row : listMaps) {
				Object aid = row.get("activity_id");
				if (aid instanceof Number n) {
					activityIds.add(n.longValue());
				}
			}
			if (!activityIds.isEmpty()) {
				LambdaQueryWrapper<RegistrationActivityRelShop> relW = new LambdaQueryWrapper<>();
				relW.in(RegistrationActivityRelShop::getActivityId, activityIds);
				relW.select(RegistrationActivityRelShop::getActivityId, RegistrationActivityRelShop::getDistributorId);
				List<RegistrationActivityRelShop> rels = registrationActivityRelShopMapper.selectList(relW);
				Set<Long> distinctForQuery = new LinkedHashSet<>();
				for (RegistrationActivityRelShop rel : rels) {
					Long aid = rel.getActivityId();
					Long did = rel.getDistributorId();
					if (aid == null) {
						continue;
					}
					List<Long> bucket = activityRelShopsByActivityId.computeIfAbsent(aid, k -> new ArrayList<>());
					if (did != null && did > 0L && !bucket.contains(did)) {
						bucket.add(did);
						distinctForQuery.add(did);
					}
				}
				distributorIdsForQuery = new ArrayList<>(distinctForQuery);
			}
		}

		Map<Long, String> distributorIdToName = new LinkedHashMap<>();
		if (!distributorIdsForQuery.isEmpty()) {
			LambdaQueryWrapper<Distributor> distW = new LambdaQueryWrapper<>();
			distW.in(Distributor::getDistributorId, distributorIdsForQuery);
			distW.eq(Distributor::getCompanyId, companyId);
			distW.select(Distributor::getDistributorId, Distributor::getName);
			List<Distributor> distributors = distributorMapper.selectList(distW);
			for (Distributor d : distributors) {
				if (d.getDistributorId() == null) {
					continue;
				}
				String name = d.getName();
				if (name != null) {
					distributorIdToName.put(d.getDistributorId(), name);
				}
			}
		}

		for (Map<String, Object> row : listMaps) {
			int st = intOrZero(row.get("start_time"));
			int et = intOrZero(row.get("end_time"));
			String statusCode;
			String msgKey;
			if (st > now) {
				statusCode = "waiting";
				msgKey = MSG_STATUS_WAITING;
			} else if (et < now) {
				statusCode = "end";
				msgKey = MSG_STATUS_ENDED;
			} else {
				statusCode = "ongoing";
				msgKey = MSG_STATUS_ONGOING;
			}
			row.put("status", statusCode);
			row.put("status_name", messageSource.getMessage(msgKey, null, locale));

			List<String> names = new ArrayList<>();
			Object aidObj = row.get("activity_id");
			Long activityId = aidObj instanceof Number ? ((Number) aidObj).longValue() : null;
			if (activityId != null && activityRelShopsByActivityId.containsKey(activityId)) {
				for (Long relDistributorId : activityRelShopsByActivityId.get(activityId)) {
					if (distributorIdToName.containsKey(relDistributorId)) {
						names.add(distributorIdToName.get(relDistributorId));
					}
				}
			}
			row.put("distributor_name", names);
		}

		if (!listMaps.isEmpty()) {
			List<Long> ids = new ArrayList<>(listMaps.size());
			for (Map<String, Object> row : listMaps) {
				Object aid = row.get("activity_id");
				if (aid instanceof Number n) {
					ids.add(n.longValue());
				}
			}
			if (!ids.isEmpty()) {
				List<ActivityUserJoinCountRow> countRows =
						registrationRecordMapper.countUserJoinsByActivityIds(companyId, ids);
				if (!countRows.isEmpty()) {
					Map<Long, Integer> joinCountByActivity = new LinkedHashMap<>();
					for (ActivityUserJoinCountRow r : countRows) {
						if (r.getActivityId() != null && r.getJoinCount() != null) {
							long jc = r.getJoinCount();
							joinCountByActivity.put(
									r.getActivityId(),
									jc > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) jc);
						}
					}
					if (!joinCountByActivity.isEmpty()) {
						for (Map<String, Object> row : listMaps) {
							Object aidObj = row.get("activity_id");
							if (aidObj instanceof Number n) {
								long aid = n.longValue();
								row.put("total_join_num", joinCountByActivity.getOrDefault(aid, 0));
							}
						}
					}
				}
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total);
		out.put("list", listMaps);
		return out;
	}

	private List<Long> queryActivityIdsByDistributor(long distributorId) {
		LambdaQueryWrapper<RegistrationActivityRelShop> relW = new LambdaQueryWrapper<>();
		relW.eq(RegistrationActivityRelShop::getDistributorId, distributorId);
		relW.select(RegistrationActivityRelShop::getActivityId);
		relW.last("LIMIT 100");
		List<RegistrationActivityRelShop> relHits = registrationActivityRelShopMapper.selectList(relW);
		List<Long> activityIdList = new ArrayList<>(relHits.size());
		for (RegistrationActivityRelShop hit : relHits) {
			if (hit.getActivityId() != null) {
				activityIdList.add(hit.getActivityId());
			}
		}
		return activityIdList;
	}

	private static void applyStatusFilter(
			LambdaQueryWrapper<RegistrationActivity> wrapper, String statusRaw, int now) {
		if (!StringUtils.hasText(statusRaw)) {
			return;
		}
		String st = statusRaw.trim();
		switch (st) {
			case "waiting" -> {
				wrapper.ge(RegistrationActivity::getStartTime, now);
				wrapper.ge(RegistrationActivity::getEndTime, now);
			}
			case "ongoing" -> {
				wrapper.le(RegistrationActivity::getStartTime, now);
				wrapper.ge(RegistrationActivity::getEndTime, now);
			}
			case "end" -> {
				wrapper.le(RegistrationActivity::getStartTime, now);
				wrapper.le(RegistrationActivity::getEndTime, now);
			}
			default -> {
				// unknown status: no extra filter
			}
		}
	}

	private static LinkedHashMap<String, Object> activityToRow(RegistrationActivity e, ZoneId zone) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("activity_id", e.getActivityId());
		m.put("temp_id", e.getTempId());
		m.put("activity_name", e.getActivityName());
		m.put("start_time", e.getStartTime());
		m.put("end_time", e.getEndTime());
		m.put("join_limit", e.getJoinLimit());
		m.put("is_sms_notice", e.getIsSmsNotice());
		m.put("is_wxapp_notice", e.getIsWxappNotice());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("company_id", e.getCompanyId());
		m.put("area", e.getArea());
		m.put("place", e.getPlace());
		m.put("address", e.getAddress());
		m.put("intro", e.getIntro());
		m.put("show_fields", e.getShowFields());
		m.put("pics", e.getPics());
		m.put("gift_points", e.getGiftPoints());
		m.put("is_allow_duplicate", e.getIsAllowDuplicate());
		m.put("is_allow_cancel", e.getIsAllowCancel());
		m.put("is_offline_verify", e.getIsOfflineVerify());
		m.put("is_need_check", e.getIsNeedCheck());
		m.put("is_white_list", e.getIsWhiteList());
		m.put("enterprise_ids", e.getEnterpriseIds());
		m.put("group_no", e.getGroupNo());
		m.put("member_level", e.getMemberLevel());
		m.put("distributor_ids", e.getDistributorIds());
		m.put("join_tips", e.getJoinTips());
		m.put("submit_form_tips", e.getSubmitFormTips());
		m.put("content", e.getContent());
		m.put("distributor_id", e.getDistributorId());

		Integer st = e.getStartTime();
		if (st != null && st != 0) {
			m.put("start_date", formatEpochSecond(st, zone));
		} else {
			m.put("start_date", null);
		}
		Integer et = e.getEndTime();
		if (et != null && et != 0) {
			m.put("end_date", formatEpochSecond(et, zone));
		} else {
			m.put("end_date", null);
		}
		return m;
	}

	private static String formatEpochSecond(int epochSec, ZoneId zone) {
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSec), zone).format(DATE_TIME_FMT);
	}

	private static int intOrZero(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return 0;
	}

	private static String escapeLike(String input) {
		String s = input.replace("\\", "\\\\");
		s = s.replace("%", "\\%");
		s = s.replace("_", "\\_");
		return s;
	}

	private static int parsePage(String pageRaw) {
		if (pageRaw == null || !StringUtils.hasText(pageRaw.trim())) {
			return DEFAULT_PAGE;
		}
		try {
			int p = Integer.parseInt(pageRaw.trim());
			return p < 1 ? DEFAULT_PAGE : p;
		} catch (NumberFormatException e) {
			return DEFAULT_PAGE;
		}
	}

	private static int parsePageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || !StringUtils.hasText(pageSizeRaw.trim())) {
			return DEFAULT_PAGE_SIZE;
		}
		try {
			int n = Integer.parseInt(pageSizeRaw.trim());
			return n <= 0 ? DEFAULT_PAGE_SIZE : n;
		} catch (NumberFormatException e) {
			return DEFAULT_PAGE_SIZE;
		}
	}

	private static long parseDistributorIdIntvalStyle(String raw) {
		if (raw == null) {
			return 0L;
		}
		String s = raw;
		int len = s.length();
		int i = 0;
		while (i < len && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
		if (i >= len) {
			return 0L;
		}
		boolean negative = false;
		char c = s.charAt(i);
		if (c == '+') {
			i++;
		} else if (c == '-') {
			negative = true;
			i++;
		}
		if (i >= len) {
			return 0L;
		}
		int startDigits = i;
		while (i < len && Character.isDigit(s.charAt(i))) {
			i++;
		}
		if (i == startDigits) {
			return 0L;
		}
		String digitStr = s.substring(startDigits, i);
		try {
			BigInteger bi = new BigInteger(negative ? "-" + digitStr : digitStr);
			if (bi.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
				return Long.MAX_VALUE;
			}
			if (bi.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
				return Long.MIN_VALUE;
			}
			return bi.longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Integer parseEpochSecondOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return null;
		}
		if (!t.matches("^-?\\d+$")) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			if (v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
				return null;
			}
			return (int) v;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean isTruthyIsValid(String isValidRaw) {
		return isValidRaw != null && !isValidRaw.isEmpty() && !"0".equals(isValidRaw);
	}

	private static boolean isTruthyQueryTimeParam(String raw) {
		return raw != null && !raw.isEmpty() && !"0".equals(raw);
	}
}
