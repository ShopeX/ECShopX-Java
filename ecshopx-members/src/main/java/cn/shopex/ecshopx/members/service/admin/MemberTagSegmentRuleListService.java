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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.members.domain.MemberSegmentRule;
import cn.shopex.ecshopx.members.mapper.MemberSegmentRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemberTagSegmentRuleListService {

	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
	private static final Pattern NUMERIC_TOKEN_PATTERN =
			Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");

	private final MemberSegmentRuleMapper memberSegmentRuleMapper;

	public MemberTagSegmentRuleListService(MemberSegmentRuleMapper memberSegmentRuleMapper) {
		this.memberSegmentRuleMapper = memberSegmentRuleMapper;
	}

	public Map<String, Object> getSegmentRuleList(
			long companyId,
			String operatorType,
			long jwtDistributorId,
			String pageRaw,
			String pageSizeRaw,
			String statusRaw,
			String tagNameRaw,
			String ruleNameRaw,
			String createdStartRaw,
			String createdEndRaw,
			String distributorIdQueryRaw) {
		try {
			int page = looseInt(pageRaw, 1);
			if (page < 1) {
				page = 1;
			}
			int pageSize = looseInt(pageSizeRaw, 20);
			if (pageSize < 1 || pageSize > 100) {
				pageSize = 20;
			}

			LambdaQueryWrapper<MemberSegmentRule> w = new LambdaQueryWrapper<>();
			w.eq(MemberSegmentRule::getCompanyId, companyId);

			if ("distributor".equals(operatorType)) {
				if (jwtDistributorId > 0) {
					w.eq(MemberSegmentRule::getDistributorId, jwtDistributorId);
				}
			} else {
				if (distributorIdQueryRaw != null && StringUtils.hasText(distributorIdQueryRaw.trim())) {
					w.eq(MemberSegmentRule::getDistributorId, parseDistributorIdQueryLong(distributorIdQueryRaw));
				}
			}

			if (statusRaw != null && StringUtils.hasText(statusRaw.trim())) {
				int statusInt = (int) LeadingNumberParser.parseAsLong(statusRaw.trim());
				w.eq(MemberSegmentRule::getStatus, statusInt);
			}

			String tagTrim = tagNameRaw == null ? "" : tagNameRaw.trim();
			if (StringUtils.hasText(tagTrim)) {
				w.like(MemberSegmentRule::getRuleName, tagTrim);
			} else {
				String ruleTrim = ruleNameRaw == null ? "" : ruleNameRaw.trim();
				if (StringUtils.hasText(ruleTrim)) {
					w.like(MemberSegmentRule::getRuleName, ruleTrim);
				}
			}

			if (StringUtils.hasText(createdStartRaw)) {
				Long startTs = parseCreatedBoundaryEpoch(createdStartRaw.trim());
				if (startTs != null && startTs > 0) {
					w.ge(MemberSegmentRule::getCreated, startTs);
				}
			}
			if (StringUtils.hasText(createdEndRaw)) {
				String trimEnd = createdEndRaw.trim();
				boolean endOriginalNumeric = isNumericToken(trimEnd);
				Long endTs = parseCreatedBoundaryEpoch(trimEnd);
				if (endTs != null && endTs > 0) {
					if (!endOriginalNumeric) {
						endTs = ZonedDateTime.ofInstant(Instant.ofEpochSecond(endTs), ZONE)
								.toLocalDate()
								.atTime(23, 59, 59)
								.atZone(ZONE)
								.toEpochSecond();
					}
					w.le(MemberSegmentRule::getCreated, endTs);
				}
			}

			w.orderByDesc(MemberSegmentRule::getCreated);

			Long total = memberSegmentRuleMapper.selectCount(w);
			List<MemberSegmentRule> rows;
			if (total == null || total == 0L) {
				rows = Collections.emptyList();
			} else {
				w.select(
						MemberSegmentRule::getRuleId,
						MemberSegmentRule::getRuleName,
						MemberSegmentRule::getDescription,
						MemberSegmentRule::getStatus,
						MemberSegmentRule::getDistributorId,
						MemberSegmentRule::getCreated,
						MemberSegmentRule::getUpdated);
				Page<MemberSegmentRule> p = new Page<>(page, pageSize, false);
				memberSegmentRuleMapper.selectPage(p, w);
				rows = p.getRecords();
			}

			List<Map<String, Object>> list = new ArrayList<>();
			for (MemberSegmentRule rule : rows) {
				list.add(toRowMap(rule));
			}

			Map<String, Object> data = new LinkedHashMap<>();
			Object totalCountVal =
					total != null && total <= Integer.MAX_VALUE ? total.intValue() : total != null ? total : 0;
			data.put("total_count", totalCountVal);
			data.put("page", page);
			data.put("page_size", pageSize);
			data.put("list", list);
			return data;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			String msg = e.getMessage() == null ? "" : e.getMessage();
			throw new ResourceException("获取规则列表失败：" + msg);
		}
	}

	private static int looseInt(String raw, int defaultVal) {
		if (raw == null || raw.trim().isEmpty()) {
			return defaultVal;
		}
		try {
			return (int) Double.parseDouble(raw.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long parseDistributorIdQueryLong(String distributorIdQueryParamRaw) {
		String s = distributorIdQueryParamRaw == null ? "" : distributorIdQueryParamRaw.trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return (long) (int) Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean isNumericToken(String trimEnd) {
		return trimEnd != null && NUMERIC_TOKEN_PATTERN.matcher(trimEnd).matches();
	}

	private static Long parseCreatedBoundaryEpoch(String trim) {
		if (isNumericToken(trim)) {
			try {
				return (long) (int) Double.parseDouble(trim);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return parseDateStringToEpoch(trim);
	}

	private static Long parseDateStringToEpoch(String s) {
		try {
			LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			return ldt.atZone(ZONE).toEpochSecond();
		} catch (DateTimeParseException ignored) {
		}
		try {
			LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
			return ldt.atZone(ZONE).toEpochSecond();
		} catch (DateTimeParseException ignored) {
		}
		try {
			LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
			return ld.atStartOfDay(ZONE).toEpochSecond();
		} catch (DateTimeParseException ignored) {
		}
		return null;
	}

	private static String formatEpoch(Long sec) {
		if (sec == null || sec <= 0) {
			return "";
		}
		return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
				.withZone(ZONE)
				.format(Instant.ofEpochSecond(sec));
	}

	private static Map<String, Object> toRowMap(MemberSegmentRule rule) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("rule_id", rule.getRuleId());
		m.put("rule_name", rule.getRuleName() == null ? "" : rule.getRuleName());
		m.put("description", rule.getDescription() == null ? "" : rule.getDescription());
		m.put("status", rule.getStatus());
		m.put("distributor_id", rule.getDistributorId() == null ? 0 : rule.getDistributorId().intValue());
		m.put("created", formatEpoch(rule.getCreated()));
		Long updated = rule.getUpdated();
		m.put("updated", (updated == null || updated <= 0) ? "" : formatEpoch(updated));
		return m;
	}
}
