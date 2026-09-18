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

package cn.shopex.ecshopx.wsugc.service.tag;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Tag;
import cn.shopex.ecshopx.wsugc.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TagDetailService {

	private static final DateTimeFormatter CREATED_TEXT_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private static final List<String> USER_INFO_WHITELIST_H5 =
			List.of("username", "avatar", "headimgurl", "nickname", "user_id");

	private static final List<String> USER_INFO_WHITELIST_ADMIN =
			List.of("username", "avatar", "headimgurl", "nickname", "user_id", "mobile");

	private final TagMapper tagMapper;
	private final MemberAccountService memberAccountService;

	public TagDetailService(TagMapper tagMapper, MemberAccountService memberAccountService) {
		this.tagMapper = tagMapper;
		this.memberAccountService = memberAccountService;
	}

	@SuppressWarnings("unused")
	public Map<String, Object> buildResponse(
			String tagIdRaw, Map<String, Object> operatorJwt, String requestLangTag) {
		long companyId = readLong(operatorJwt.get("company_id"), 1L);
		Long tagId = parseOptionalLong(tagIdRaw);
		if (tagId == null) {
			return sortedOuter(null);
		}
		LambdaQueryWrapper<Tag> w = new LambdaQueryWrapper<>();
		w.eq(Tag::getCompanyId, companyId).eq(Tag::getTagId, tagId).last("LIMIT 1");
		Tag row = tagMapper.selectOne(w);
		if (row == null) {
			return sortedOuter(null);
		}
		LinkedHashMap<String, Object> rowMap = tagToRowMap(row);
		formatDetail(rowMap, true, USER_INFO_WHITELIST_ADMIN);
		return sortedOuter(ksortCopy(rowMap));
	}

	public Map<String, Object> buildH5TagDetailResponse(long companyId, String tagIdRaw) {
		LambdaQueryWrapper<Tag> w = new LambdaQueryWrapper<>();
		w.eq(Tag::getCompanyId, companyId);
		if (tagIdRaw == null) {
			w.isNull(Tag::getTagId);
		} else {
			String t = tagIdRaw.trim();
			if (t.isEmpty()) {
				w.isNull(Tag::getTagId);
			} else {
				Long parsed = parseOptionalLong(t);
				if (parsed != null) {
					w.eq(Tag::getTagId, parsed);
				} else {
					w.eq(Tag::getTagId, -1L);
				}
			}
		}
		w.last("LIMIT 1");
		Tag row = tagMapper.selectOne(w);
		if (row == null) {
			return sortedOuter(null);
		}
		LinkedHashMap<String, Object> rowMap = tagToRowMap(row);
		formatDetail(rowMap, true, USER_INFO_WHITELIST_H5);
		return sortedOuter(ksortCopy(rowMap));
	}

	public LinkedHashMap<String, Object> toAdminListRowMap(Tag entity) {
		LinkedHashMap<String, Object> m = tagToRowMap(entity);
		formatDetail(m, false, USER_INFO_WHITELIST_ADMIN);
		return ksortCopy(m);
	}

	public LinkedHashMap<String, Object> toH5ListRowMap(Tag entity) {
		LinkedHashMap<String, Object> m = tagToRowMap(entity);
		formatDetail(m, false, USER_INFO_WHITELIST_H5);
		return ksortCopy(m);
	}

	private static Map<String, Object> sortedOuter(Map<String, Object> detailOrNull) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("tag_info", detailOrNull);
		return ksortCopy(out);
	}

	private static LinkedHashMap<String, Object> ksortCopy(Map<String, Object> src) {
		TreeMap<String, Object> sorted = new TreeMap<>(src);
		return new LinkedHashMap<>(sorted);
	}

	private void formatDetail(
			Map<String, Object> rowMap, boolean stripMobileWhenNoUser, List<String> userInfoWhitelist) {
		Object created = rowMap.get("created");
		if (created != null) {
			long createdSec = toEpochSeconds(created);
			if (createdSec > 0) {
				String formatted = CREATED_TEXT_FMT.format(Instant.ofEpochSecond(createdSec));
				rowMap.put("created_text", formatted);
				rowMap.put("updated_text", formatted);
			}
		}

		Object manual = rowMap.get("manual_verify_time");
		if (manual instanceof Number n) {
			long mv = n.longValue();
			if (mv > 0) {
				rowMap.put("manual_verify_time", CREATED_TEXT_FMT.format(Instant.ofEpochSecond(mv)));
			}
		}

		Object st = rowMap.get("status");
		int status = st instanceof Number n ? n.intValue() : parseIntLoose(st);
		rowMap.put("status_text", tagStatusText(status));

		Long uid = longOrNull(rowMap.get("user_id"));
		if (uid != null && uid != 0L) {
			long rowCompanyId = readLong(rowMap.get("company_id"), 1L);
			Map<String, Object> filter = Map.of("user_id", uid, "company_id", rowCompanyId);
			Map<String, Object> userInfo = memberAccountService.getWechatUserInfo(filter);
			if (userInfo == null) {
				userInfo = Collections.emptyMap();
			}
			Map<String, Object> memberInfo = memberAccountService.getMemberInfo(uid, rowCompanyId);
			if (memberInfo != null && !memberInfo.isEmpty()) {
				LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
				merged.putAll(memberInfo);
				merged.putAll(userInfo);
				TreeMap<String, Object> userInfoOut = new TreeMap<>();
				for (String k : userInfoWhitelist) {
					if (merged.containsKey(k)) {
						userInfoOut.put(k, merged.get(k));
					}
				}
				rowMap.put("userInfo", new LinkedHashMap<>(userInfoOut));
			} else {
				TreeMap<String, Object> userInfoOut = new TreeMap<>();
				for (String k : userInfoWhitelist) {
					if (userInfo.containsKey(k)) {
						userInfoOut.put(k, userInfo.get(k));
					}
				}
				rowMap.put("userInfo", new LinkedHashMap<>(userInfoOut));
			}
		}
		if (stripMobileWhenNoUser && (uid == null || uid == 0L)) {
			rowMap.remove("mobile");
		}
	}

	private static String tagStatusText(int status) {
		return switch (status) {
			case 0 -> "待审核";
			case 1 -> "审核通过";
			case 2 -> "机器拒绝";
			case 3 -> "待人工审核";
			case 4 -> "人工拒绝";
			default -> "";
		};
	}

	private static int parseIntLoose(Object st) {
		if (st == null) {
			return -1;
		}
		if (st instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(st.toString().trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static long toEpochSeconds(Object created) {
		if (created instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(created.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Long parseOptionalLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Object aiVerifyTimeForJson(Long ts) {
		if (ts == null) {
			return null;
		}
		if (ts.longValue() == 0L) {
			return "0";
		}
		return ts;
	}

	private static long readLong(Object v, long defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static LinkedHashMap<String, Object> tagToRowMap(Tag e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("tag_id", e.getTagId());
		m.put("tag_name", e.getTagName());
		m.put("created", e.getCreated());
		m.put("p_order", e.getPOrder());
		m.put("user_id", e.getUserId() != null ? e.getUserId().intValue() : 0);
		m.put("company_id", e.getCompanyId());
		m.put("enabled", e.getEnabled());
		m.put("status", e.getStatus());
		m.put("operator_id", e.getOperatorId());
		m.put("source", e.getSource());
		m.put("updated", e.getUpdated());
		m.put("ai_verify_time", aiVerifyTimeForJson(e.getAiVerifyTime()));
		Long manual = e.getManualVerifyTime();
		m.put("manual_verify_time", manual != null ? manual : 0L);
		m.put("manual_refuse_reason", e.getManualRefuseReason());
		m.put("ai_refuse_reason", e.getAiRefuseReason());
		m.put("mobile", e.getMobile());
		return m;
	}
}
