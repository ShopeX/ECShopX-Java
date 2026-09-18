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

package cn.shopex.ecshopx.promotions.service.checkin;

import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.CheckInLog;
import cn.shopex.ecshopx.promotions.mapper.CheckInLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CheckInAdminListService {

	private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private final CheckInLogMapper checkInLogMapper;
	private final MemberAccountService memberAccountService;
	private final MessageSource messageSource;

	public CheckInAdminListService(
			CheckInLogMapper checkInLogMapper,
			MemberAccountService memberAccountService,
			MessageSource messageSource) {
		this.checkInLogMapper = checkInLogMapper;
		this.memberAccountService = memberAccountService;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getCheckInList(
			long companyId,
			Long queryUserId,
			String mobile,
			String startDate,
			String endDate,
			Integer page,
			Integer pageSize,
			Locale locale) {
		Locale loc = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;

		boolean invalidDate = false;
		Integer startYmdInt = null;
		Integer endYmdInt = null;

		if (StringUtils.hasText(startDate)) {
			String t = startDate.trim();
			if (StringUtils.hasText(t)) {
				try {
					startYmdInt = yyyyMmddInt(LocalDate.parse(t, ISO_DATE));
				} catch (DateTimeParseException e) {
					invalidDate = true;
				}
			}
		}
		if (StringUtils.hasText(endDate)) {
			String t = endDate.trim();
			if (StringUtils.hasText(t)) {
				try {
					endYmdInt = yyyyMmddInt(LocalDate.parse(t, ISO_DATE));
				} catch (DateTimeParseException e) {
					invalidDate = true;
				}
			}
		}

		if (invalidDate) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0);
			empty.put("list", Collections.emptyList());
			return empty;
		}

		LambdaQueryWrapper<CheckInLog> w = new LambdaQueryWrapper<>();
		w.eq(CheckInLog::getCompanyId, companyId);

		boolean mobileMemberResolved = false;
		Map<Long, Map<String, Object>> memberdataByUserId = null;

		if (queryUserId != null && queryUserId > 0) {
			w.eq(CheckInLog::getUserId, queryUserId);
		} else if (StringUtils.hasText(mobile == null ? "" : mobile.trim())) {
			String m = mobile.trim();
			Members mem = memberAccountService.findMemberByCompanyAndMobile(companyId, m);
			if (mem != null) {
				mobileMemberResolved = true;
				long uid = mem.getUserId();
				w.eq(CheckInLog::getUserId, uid);
				Map<String, Object> merged = memberAccountService.getMemberInfo(uid, companyId);
				memberdataByUserId = new HashMap<>();
				memberdataByUserId.put(uid, merged != null ? merged : Map.of());
			} else {
				w.isNull(CheckInLog::getUserId);
			}
		}

		if (startYmdInt != null) {
			w.ge(CheckInLog::getCreateTime, startYmdInt);
		}
		if (endYmdInt != null) {
			w.le(CheckInLog::getCreateTime, endYmdInt);
		}

		w.orderByDesc(CheckInLog::getCreateTime);

		int pRaw = page == null ? 1 : page;
		int ps = pageSize == null ? 20 : pageSize;

		Long total = checkInLogMapper.selectCount(w);
		List<CheckInLog> entities;
		if (pRaw < 1 || ps < 1) {
			entities = Collections.emptyList();
		} else {
			Page<CheckInLog> pg = new Page<>(pRaw, ps, false);
			checkInLogMapper.selectPage(pg, w);
			entities = pg.getRecords();
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		for (CheckInLog e : entities) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("id", e.getId());
			row.put("company_id", e.getCompanyId());
			row.put("user_id", e.getUserId());
			row.put("create_time", e.getCreateTime());
			row.put("tag", e.getTag());
			row.put("created", e.getCreated());
			rows.add(row);
		}

		if (!rows.isEmpty()) {
			if (mobileMemberResolved && memberdataByUserId != null) {
				for (Map<String, Object> row : rows) {
					Long uid = (Long) row.get("user_id");
					if (uid != null && memberdataByUserId.containsKey(uid)) {
						String rawUsername = usernameString(memberdataByUserId.get(uid));
						String display = StringUtils.hasText(rawUsername) ? rawUsername : anonymousLabel(loc);
						row.put("user_name", display);
					}
				}
			} else if (!mobileMemberResolved) {
				Set<Long> ids = rows.stream()
						.map(r -> (Long) r.get("user_id"))
						.filter(Objects::nonNull)
						.collect(Collectors.toCollection(LinkedHashSet::new));
				if (!ids.isEmpty()) {
					List<Map<String, Object>> summaries =
							memberAccountService.listMemberSummariesByUserIds(companyId, ids);
					Map<Long, String> userIdToDisplay = new HashMap<>();
					for (Map<String, Object> srow : summaries) {
						Long uid = (Long) srow.get("user_id");
						if (uid == null) {
							continue;
						}
						String un = srow.get("username") instanceof String str ? str : "";
						userIdToDisplay.put(uid, StringUtils.hasText(un) ? un : anonymousLabel(loc));
					}
					for (Map<String, Object> row : rows) {
						Long uid = (Long) row.get("user_id");
						if (uid != null && userIdToDisplay.containsKey(uid)) {
							row.put("user_name", userIdToDisplay.get(uid));
						}
					}
				}
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total != null ? total.intValue() : 0);
		out.put("list", rows);
		return out;
	}

	private static int yyyyMmddInt(LocalDate d) {
		return d.getYear() * 10_000 + d.getMonthValue() * 100 + d.getDayOfMonth();
	}

	private static String usernameString(Map<String, Object> member) {
		Object v = member.get("username");
		return v instanceof String s ? s : "";
	}

	private String anonymousLabel(Locale loc) {
		try {
			return messageSource.getMessage("promotions.checkin.anonymous_user", null, loc);
		} catch (NoSuchMessageException e) {
			return "匿名用户";
		}
	}
}
