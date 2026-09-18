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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.domain.PromoterIdentity;
import cn.shopex.ecshopx.popularize.mapper.PromoterIdentityMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PromoterFrontPromoterInfoReadService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter BIND_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final PromoterMapper promoterMapper;
	private final MemberAccountService memberAccountService;
	private final PromoterGradeService promoterGradeService;
	private final PromoterIdentityMapper promoterIdentityMapper;
	private final boolean oemShuyun;

	public PromoterFrontPromoterInfoReadService(
			PromoterMapper promoterMapper,
			MemberAccountService memberAccountService,
			PromoterGradeService promoterGradeService,
			PromoterIdentityMapper promoterIdentityMapper,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.promoterMapper = promoterMapper;
		this.memberAccountService = memberAccountService;
		this.promoterGradeService = promoterGradeService;
		this.promoterIdentityMapper = promoterIdentityMapper;
		this.oemShuyun = oemShuyun;
	}

	/**
	 * Single enriched promoter row for H5, aligned with list export formatting (no reflection).
	 *
	 * @return {@code null} when no promoter row exists; empty map when {@code userId <= 0}
	 */
	public Map<String, Object> getPromoterInfoMap(long companyId, long userId) {
		if (userId <= 0L) {
			return new LinkedHashMap<>();
		}
		Promoter main =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, userId)
								.last("LIMIT 1"));
		if (main == null) {
			return null;
		}
		LinkedHashMap<String, Object> row = promoterEntityToSnakeRow(main);
		mergeMemberWideSliceFirst(companyId, row, main.getUserId());
		enrichPromoterRow(companyId, row, main, true);
		Object idv = row.get("id");
		if (idv != null) {
			row.put("promoter_id", String.valueOf(idv));
		}
		stringifyWideIdFieldsForH5Json(row);
		stringifyTopLevelUpdated(row);
		normalizeJsonMapValues(row);
		return row;
	}

	private void mergeMemberWideSliceFirst(long companyId, LinkedHashMap<String, Object> row, Long userId) {
		if (userId == null || userId <= 0L) {
			return;
		}
		LinkedHashMap<String, Object> slice = memberAccountService.readMemberWideMergeSlice(companyId, userId);
		if (slice.isEmpty()) {
			return;
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : slice.entrySet()) {
			merged.put(e.getKey(), e.getValue());
		}
		for (Map.Entry<String, Object> e : row.entrySet()) {
			merged.put(e.getKey(), e.getValue());
		}
		row.clear();
		row.putAll(merged);
	}

	/** Stringify common wide-row id keys so H5 JSON matches numeric ids as string values. */
	private static void stringifyWideIdFieldsForH5Json(LinkedHashMap<String, Object> row) {
		for (String k :
				List.of("user_id", "company_id", "grade_id", "identity_id", "id", "promoter_id")) {
			Object v = row.get(k);
			if (v == null || v instanceof String) {
				continue;
			}
			row.put(k, String.valueOf(v));
		}
	}

	/**
	 * Ensures the top-level {@code updated} field is a string in the wide-row map so H5 JSON serializes that
	 * property as a quoted string when the underlying value is not already a string.
	 */
	private static void stringifyTopLevelUpdated(LinkedHashMap<String, Object> row) {
		Object u = row.get("updated");
		if (u == null) {
			return;
		}
		if (!(u instanceof String)) {
			row.put("updated", String.valueOf(u));
		}
	}

	private void enrichPromoterRow(long companyId, LinkedHashMap<String, Object> row, Promoter entity, boolean loadParentInfo) {
		long pid = entity.getId() == null ? 0L : entity.getId();
		long uid = entity.getUserId() == null ? 0L : entity.getUserId();

		List<Long> promoterIds = pid > 0L ? List.of(pid) : List.of();
		Map<Long, Long> pidToCount = new HashMap<>();
		if (!promoterIds.isEmpty()) {
			List<Map<String, Object>> cntRows = promoterMapper.countDirectChildrenByPidList(companyId, promoterIds);
			for (Map<String, Object> r : cntRows) {
				if (r == null) {
					continue;
				}
				Object pidO = r.get("pid");
				Object cntO = r.get("cnt");
				if (pidO == null) {
					continue;
				}
				long pkey = longFrom(pidO);
				long cnt = longFrom(cntO);
				pidToCount.put(pkey, cnt);
			}
		}
		row.put("children_count", pidToCount.getOrDefault(pid, 0L));

		Integer createdInt = intOrNull(row.get("created"));
		if (createdInt != null) {
			row.put(
					"bind_date",
					Instant.ofEpochSecond(createdInt.longValue())
							.atZone(SHANGHAI)
							.toLocalDate()
							.format(BIND_DATE_FMT));
		} else {
			row.put("bind_date", "");
		}

		Map<Long, String> usernameByUser = new HashMap<>();
		Map<Long, String> mobileByUser = new HashMap<>();
		if (uid > 0L) {
			for (Map<String, Object> sum : memberAccountService.listMemberSummariesByUserIds(companyId, List.of(uid))) {
				Object uidObj = sum.get("user_id");
				if (uidObj == null) {
					continue;
				}
				long u = longFrom(uidObj);
				usernameByUser.put(
						u, sum.get("username") != null ? String.valueOf(sum.get("username")) : "");
				mobileByUser.put(u, sum.get("mobile") != null ? String.valueOf(sum.get("mobile")) : "");
			}
		}

		Map<Long, Map<String, String>> wechatByUser =
				uid > 0L
						? memberAccountService.batchWechatNicknameHeadByUserIds(companyId, List.of(uid))
						: Map.of();

		row.put("mobile", uid > 0L ? mobileByUser.getOrDefault(uid, "") : "");
		row.put("username", uid > 0L ? usernameByUser.getOrDefault(uid, "") : "");

		Map<String, String> wx = uid > 0L ? wechatByUser.get(uid) : null;
		if (wx != null) {
			row.put("nickname", wx.getOrDefault("nickname", ""));
			String head = wx.get("headimgurl");
			row.put("headimgurl", (head == null || head.isEmpty()) ? null : head);
		} else {
			row.put("nickname", "");
			row.put("headimgurl", null);
		}

		boolean isOpen = promoterGradeService.readIsOpenPromoterGrade(companyId);
		String isOpenStr = isOpen ? "true" : "false";
		Integer gradeLevel = intOrNull(row.get("grade_level"));
		row.put("promoter_grade_name", promoterGradeService.readPromoterGradeDisplayName(companyId, gradeLevel));
		row.put("is_open_promoter_grade", isOpenStr);

		if (oemShuyun) {
			Long iid = longOrNull(row.get("identity_id"));
			if (iid != null && iid > 0L) {
				PromoterIdentity one = promoterIdentityMapper.selectById(iid);
				if (one != null && one.getName() != null) {
					row.put("identity_name", one.getName());
				}
			}
		}

		if (loadParentInfo) {
			Long parentPromoterId = entity.getPid();
			if (parentPromoterId != null && parentPromoterId > 0L) {
				Promoter parentEntity = promoterMapper.selectById(parentPromoterId);
				if (parentEntity != null) {
					LinkedHashMap<String, Object> parentRow = promoterEntityToSnakeRow(parentEntity);
					enrichPromoterRow(companyId, parentRow, parentEntity, false);
					row.put("parent_info", parentRow);
				}
			}
		}
	}

	/**
	 * Converts {@code p} into a snake_case {@link LinkedHashMap} for API-style rows. String getters that should
	 * serialize as JSON {@code null} when empty pass through {@link #emptyStringToNull(String)}. The {@code updated}
	 * timestamp is intentionally omitted here so it can come from the member wide slice after merge when the
	 * promoter payload does not carry {@code updated}.
	 */
	private static LinkedHashMap<String, Object> promoterEntityToSnakeRow(Promoter p) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", p.getId());
		m.put("company_id", p.getCompanyId());
		m.put("user_id", p.getUserId());
		m.put("identity_id", p.getIdentityId());
		m.put("is_subordinates", p.getIsSubordinates());
		Long pidCol = p.getPid();
		m.put("pid", pidCol == null || pidCol == 0L ? 0 : pidCol);
		m.put("pmobile", p.getPmobile() != null ? p.getPmobile() : "");
		m.put("pname", emptyStringToNull(p.getPname()));
		m.put("shop_name", emptyStringToNull(p.getShopName()));
		m.put("alipay_name", p.getAlipayName() != null ? p.getAlipayName() : "");
		m.put("brief", emptyStringToNull(p.getBrief()));
		m.put("shop_pic", emptyStringToNull(p.getShopPic()));
		m.put("alipay_account", emptyStringToNull(p.getAlipayAccount()));
		m.put("grade_level", p.getGradeLevel());
		m.put("is_promoter", p.getIsPromoter());
		m.put("shop_status", p.getShopStatus());
		m.put("reason", emptyStringToNull(p.getReason()));
		m.put("disabled", p.getDisabled());
		m.put("is_buy", p.getIsBuy());
		m.put("promoter_name", emptyStringToNull(p.getPromoterName()));
		m.put("regions_id", emptyStringToNull(p.getRegionsId()));
		m.put("address", emptyStringToNull(p.getAddress()));
		m.put("created", p.getCreated());
		return m;
	}

	private static String emptyStringToNull(String s) {
		return (s == null || s.isEmpty()) ? null : s;
	}

	private static long longFrom(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long longOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer intOrNull(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Ensures map values are JSON-friendly (e.g. JDBC {@code byte[]}, {@code java.sql.Timestamp}) for nested rows
	 * like {@code parent_info}.
	 */
	@SuppressWarnings("unchecked")
	private static void normalizeJsonMapValues(Map<String, Object> m) {
		if (m == null || m.isEmpty()) {
			return;
		}
		for (Map.Entry<String, Object> e : m.entrySet()) {
			Object v = e.getValue();
			if (v instanceof Map<?, ?> rawChild) {
				normalizeJsonMapValues((Map<String, Object>) rawChild);
			} else {
				e.setValue(jsonSafeScalar(v));
			}
		}
	}

	private static Object jsonSafeScalar(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Boolean || v instanceof Number || v instanceof String) {
			return v;
		}
		if (v instanceof byte[] b) {
			return new String(b, StandardCharsets.UTF_8);
		}
		if (v instanceof java.sql.Timestamp ts) {
			return ts.getTime() / 1000L;
		}
		if (v instanceof java.sql.Date sd) {
			return sd.getTime() / 1000L;
		}
		if (v instanceof java.util.Date d) {
			return d.getTime() / 1000L;
		}
		return String.valueOf(v);
	}
}
