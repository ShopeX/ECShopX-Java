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

import cn.shopex.ecshopx.common.members.port.MembersSubscribeNoticeItemNamePort;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.domain.SubscribeNotice;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.SubscribeNoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MembersSubscribeNoticeListService {

	private static final Pattern DECIMAL_OR_EXPONENT_NUMERIC =
			Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");

	private final SubscribeNoticeMapper subscribeNoticeMapper;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MembersSubscribeNoticeItemNamePort itemNamePort;
	private final LangueProperties langueProperties;

	public MembersSubscribeNoticeListService(
			SubscribeNoticeMapper subscribeNoticeMapper,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			MembersSubscribeNoticeItemNamePort itemNamePort,
			LangueProperties langueProperties) {
		this.subscribeNoticeMapper = subscribeNoticeMapper;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.itemNamePort = itemNamePort;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> getLists(long companyId, HttpServletRequest request) {
		String requestLang = RequestLangTag.current(langueProperties);
		String timeStartBegin = request.getParameter("time_start_begin");
		String timeStartEnd = request.getParameter("time_start_end");
		String subTypeRaw = request.getParameter("sub_type");
		String relIdRaw = request.getParameter("rel_id");

		String subTypeResolved =
				(subTypeRaw == null || subTypeRaw.trim().isEmpty()) ? "goods" : subTypeRaw.trim();

		LambdaQueryWrapper<SubscribeNotice> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SubscribeNotice::getCompanyId, companyId);

		if (StringUtils.hasText(timeStartBegin != null ? timeStartBegin.trim() : "")) {
			String beginTrim = timeStartBegin.trim();
			Long beginEpoch = parseCreatedBoundaryEpoch(beginTrim);
			if (beginEpoch != null && beginEpoch > 0) {
				wrapper.ge(SubscribeNotice::getCreated, beginEpoch);
			}
			String endRaw = timeStartEnd;
			String endTrim = endRaw == null ? "" : endRaw.trim();
			if (!StringUtils.hasText(endTrim)) {
				wrapper.apply("created <= NULL");
			} else {
				Long endEpoch = parseCreatedBoundaryEpoch(endTrim);
				if (endEpoch != null && endEpoch > 0) {
					wrapper.le(SubscribeNotice::getCreated, endEpoch);
				} else {
					wrapper.apply("created <= NULL");
				}
			}
		}

		wrapper.eq(SubscribeNotice::getSubType, subTypeResolved);

		String relTrim = relIdRaw == null ? "" : relIdRaw.trim();
		long relParsed = LeadingNumberParser.parseAsLong(relTrim);
		if (relParsed != 0L) {
			wrapper.eq(SubscribeNotice::getRelId, relParsed);
		}

		if (request.getParameterMap().containsKey("sub_status")) {
			String ssVal = request.getParameter("sub_status");
			if (ssVal != null && !ssVal.isEmpty()) {
				wrapper.eq(SubscribeNotice::getSubStatus, ssVal);
			}
		}

		int page = looseInt(request.getParameter("page"), 1);
		int pageSize = looseInt(request.getParameter("pageSize"), 20);

		long total = subscribeNoticeMapper.selectCount(wrapper);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", (int) total);

		if (total == 0) {
			data.put("list", Collections.emptyList());
			return data;
		}

		wrapper.orderByDesc(SubscribeNotice::getCreated);
		if (pageSize > 0) {
			int safeOffset = Math.max(0, (page - 1) * pageSize);
			wrapper.last(String.format(Locale.ROOT, "LIMIT %d OFFSET %d", pageSize, safeOffset));
		}
		List<SubscribeNotice> entities = subscribeNoticeMapper.selectList(wrapper);

		ArrayList<LinkedHashMap<String, Object>> listMaps = new ArrayList<>(entities.size());
		for (SubscribeNotice e : entities) {
			listMaps.add(toSubscribeListRowMap(e));
		}

		for (Map<String, Object> row : listMaps) {
			long userId = LeadingNumberParser.parseAsLong(toRawString(row.get("user_id")));
			if (userId != 0L) {
				MembersInfo info =
						membersInfoMapper.selectOne(
								new LambdaQueryWrapper<MembersInfo>()
										.eq(MembersInfo::getCompanyId, companyId)
										.eq(MembersInfo::getUserId, userId));
				Members m = membersMapper.selectMemberRowForAdminByCompanyAndUserId(companyId, userId);
				String u1 =
						(info != null
										&& info.getUsername() != null
										&& !info.getUsername().isEmpty())
								? info.getUsername()
								: null;
				String u2 =
						(m != null && m.getUsername() != null && !m.getUsername().isEmpty())
								? m.getUsername()
								: null;
				row.put("username", u1 != null ? u1 : (u2 != null ? u2 : "匿名"));
			}
			if (Objects.equals("goods", subTypeResolved)) {
				long rel = LeadingNumberParser.parseAsLong(toRawString(row.get("rel_id")));
				if (rel == 0L) {
					row.put("item_name", "");
				} else {
					row.put(
							"item_name",
							itemNamePort.resolveItemName(
									companyId, rel, requestLang));
				}
			}
		}

		data.put("list", listMaps);
		return data;
	}

	private static String toRawString(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
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

	private static boolean isLooseIntDigits(String trimEnd) {
		return trimEnd != null && DECIMAL_OR_EXPONENT_NUMERIC.matcher(trimEnd).matches();
	}

	private static Long parseCreatedBoundaryEpoch(String trim) {
		if (isLooseIntDigits(trim)) {
			try {
				return (long) (int) Double.parseDouble(trim);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return mysqlImplicitStringToNumberForBigintCompare(trim);
	}

	/**
	 * When a BIGINT column is compared to a non-numeric string in MySQL, the string is converted in
	 * numeric context by taking the leading numeric portion (same as {@code '2030-12-31'+0} → 2030).
	 */
	private static Long mysqlImplicitStringToNumberForBigintCompare(String trim) {
		if (trim == null || trim.isEmpty()) {
			return null;
		}
		int i = 0;
		boolean negative = false;
		if (trim.charAt(0) == '-') {
			negative = true;
			i = 1;
		}
		long acc = 0L;
		boolean anyDigit = false;
		boolean sawDot = false;
		while (i < trim.length()) {
			char c = trim.charAt(i);
			if (c >= '0' && c <= '9') {
				anyDigit = true;
				int d = c - '0';
				if (acc > Long.MAX_VALUE / 10 || (acc == Long.MAX_VALUE / 10 && d > Long.MAX_VALUE % 10)) {
					return negative ? Long.MIN_VALUE : Long.MAX_VALUE;
				}
				acc = acc * 10 + d;
				i++;
			} else if (c == '.' && anyDigit && !sawDot) {
				sawDot = true;
				i++;
				while (i < trim.length() && trim.charAt(i) >= '0' && trim.charAt(i) <= '9') {
					i++;
				}
				break;
			} else {
				break;
			}
		}
		if (!anyDigit) {
			return null;
		}
		return negative ? -acc : acc;
	}

	private LinkedHashMap<String, Object> toSubscribeListRowMap(SubscribeNotice e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("sub_id", e.getSubId());
		m.put("company_id", e.getCompanyId());
		m.put("user_id", e.getUserId());
		m.put("open_id", e.getOpenId());
		m.put("rel_id", e.getRelId());
		m.put("sub_type", e.getSubType());
		m.put("remarks", e.getRemarks());
		m.put("sub_status", e.getSubStatus());
		m.put("err_reason", e.getErrReason());
		m.put("updated", e.getUpdated());
		m.put("created", e.getCreated());
		m.put("source", e.getSource());
		m.put("distributor_id", e.getDistributorId());
		return m;
	}
}
