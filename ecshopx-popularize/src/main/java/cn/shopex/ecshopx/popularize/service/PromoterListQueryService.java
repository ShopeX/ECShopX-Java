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
import cn.shopex.ecshopx.popularize.domain.PromoterIdentity;
import cn.shopex.ecshopx.popularize.mapper.PromoterIdentityMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterListMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterListQueryService {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter BIND_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

	private final PromoterListMapper promoterListMapper;
	private final PromoterMapper promoterMapper;
	private final MemberAccountService memberAccountService;
	private final PromoterGradeService promoterGradeService;
	private final PromoterIdentityMapper promoterIdentityMapper;
	private final boolean oemShuyun;

	public PromoterListQueryService(
			PromoterListMapper promoterListMapper,
			PromoterMapper promoterMapper,
			MemberAccountService memberAccountService,
			PromoterGradeService promoterGradeService,
			PromoterIdentityMapper promoterIdentityMapper,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.promoterListMapper = promoterListMapper;
		this.promoterMapper = promoterMapper;
		this.memberAccountService = memberAccountService;
		this.promoterGradeService = promoterGradeService;
		this.promoterIdentityMapper = promoterIdentityMapper;
		this.oemShuyun = oemShuyun;
	}

	public Map<String, Object> getPromoterList(Map<String, Object> filter, int page, int pageSize) {
		LinkedHashMap<String, Object> f = new LinkedHashMap<>(filter);
		long companyId = longFrom(f.get("company_id"));

		List<Long> userIds = extractInitialUserIdList(f.remove("user_id"));

		String mobile = readTrimmedString(f.get("mobile"));
		if (StringUtils.hasText(mobile)) {
			List<Long> ids = memberAccountService.listUserIdsByCompanyAndMobile(companyId, mobile.trim());
			if (ids.isEmpty()) {
				userIds = List.of(-1L);
			} else {
				if (userIds != null && !userIds.isEmpty()) {
					ArrayList<Long> inter = new ArrayList<>(userIds);
					inter.retainAll(ids);
					userIds = inter.isEmpty() ? List.of(-1L) : inter;
				} else {
					userIds = new ArrayList<>(ids);
				}
			}
			f.remove("mobile");
		}

		if (!isNoMatchUserIdList(userIds)) {
			String username = readTrimmedString(f.get("username"));
			if (StringUtils.hasText(username)) {
				List<Long> ids = memberAccountService.listUserIdsByUsername(companyId, username.trim());
				if (ids.isEmpty()) {
					userIds = List.of(-1L);
				} else {
					if (userIds != null && !userIds.isEmpty()) {
						ArrayList<Long> inter = new ArrayList<>(userIds);
						inter.retainAll(ids);
						userIds = inter.isEmpty() ? List.of(-1L) : inter;
					} else {
						userIds = new ArrayList<>(ids);
					}
				}
				f.remove("username");
			}
		}

		if (f.containsKey("username")) {
			Object un = f.get("username");
			if (un == null || !StringUtils.hasText(String.valueOf(un).trim())) {
				f.remove("username");
			}
		}

		if (isNoMatchUserIdList(userIds)) {
			f.put("user_id", List.of(-1L));
		} else if (userIds == null || userIds.isEmpty()) {
			f.remove("user_id");
		} else {
			f.put("user_id", userIds);
		}

		resolveIdentityNameToId(companyId, f);

		f.put("is_promoter", 1);

		Map<String, Object> queryFilter = f;

		long total = promoterListMapper.countPromoterListForExport(companyId, queryFilter);
		if (total <= 0L) {
			return Map.of("total_count", 0L, "list", List.of());
		}

		int offset = (page - 1) * pageSize;
		List<Map<String, Object>> rawList =
				promoterListMapper.selectPromoterListForExport(companyId, queryFilter, offset, pageSize);
		if (rawList == null || rawList.isEmpty()) {
			return Map.of("total_count", total, "list", List.of());
		}

		List<Map<String, Object>> formatted = formatPromoterData(companyId, rawList);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", formatted);
		return out;
	}

	private void resolveIdentityNameToId(long companyId, Map<String, Object> f) {
		Object rawName = f.get("identity_name");
		if (rawName == null) {
			return;
		}
		String trimmed = String.valueOf(rawName).trim();
		if (!StringUtils.hasText(trimmed)) {
			f.remove("identity_name");
			return;
		}
		PromoterIdentity one =
				promoterIdentityMapper.selectOne(
						new LambdaQueryWrapper<PromoterIdentity>()
								.eq(PromoterIdentity::getCompanyId, companyId)
								.eq(PromoterIdentity::getName, trimmed)
								.orderByDesc(PromoterIdentity::getId)
								.last("LIMIT 1"));
		long identityId = (one == null || one.getId() == null) ? -1L : one.getId();
		f.put("identity_id", identityId);
		f.remove("identity_name");
	}

	private List<Map<String, Object>> formatPromoterData(long companyId, List<Map<String, Object>> list) {
		List<Long> promoterIds = new ArrayList<>();
		for (Map<String, Object> row : list) {
			Object ido = row.get("id");
			if (ido != null) {
				promoterIds.add(longFrom(ido));
			}
		}

		List<Long> userIds =
				list.stream()
						.map(r -> r.get("user_id"))
						.filter(Objects::nonNull)
						.map(PromoterListQueryService::longFrom)
						.filter(uid -> uid > 0L)
						.distinct()
						.toList();

		Map<Long, Long> pidToCount = new HashMap<>();
		if (!promoterIds.isEmpty()) {
			List<Map<String, Object>> cntRows =
					promoterMapper.countDirectChildrenByPidList(companyId, promoterIds);
			for (Map<String, Object> r : cntRows) {
				if (r == null) {
					continue;
				}
				Object pidO = r.get("pid");
				Object cntO = r.get("cnt");
				if (pidO == null) {
					continue;
				}
				long pid = longFrom(pidO);
				long cnt = longFrom(cntO);
				pidToCount.put(pid, cnt);
			}
		}

		boolean isOpen = promoterGradeService.readIsOpenPromoterGrade(companyId);
		String isOpenStr = isOpen ? "true" : "false";

		Map<Long, String> usernameByUser = new HashMap<>();
		Map<Long, String> mobileByUser = new HashMap<>();
		if (!userIds.isEmpty()) {
			for (Map<String, Object> sum : memberAccountService.listMemberSummariesByUserIds(companyId, userIds)) {
				Object uidObj = sum.get("user_id");
				if (uidObj == null) {
					continue;
				}
				long uid = longFrom(uidObj);
				usernameByUser.put(
						uid, sum.get("username") != null ? String.valueOf(sum.get("username")) : "");
				mobileByUser.put(uid, sum.get("mobile") != null ? String.valueOf(sum.get("mobile")) : "");
			}
		}

		Map<Long, Map<String, String>> wechatByUser =
				userIds.isEmpty()
						? Map.of()
						: memberAccountService.batchWechatNicknameHeadByUserIds(companyId, userIds);

		Map<Long, String> identityNameById = new HashMap<>();
		if (oemShuyun) {
			List<Long> identityIds =
					list.stream()
							.map(r -> r.get("identity_id"))
							.filter(Objects::nonNull)
							.map(PromoterListQueryService::longFrom)
							.filter(id -> id > 0L)
							.distinct()
							.toList();
			if (!identityIds.isEmpty()) {
				List<PromoterIdentity> identities = promoterIdentityMapper.selectBatchIds(identityIds);
				if (identities != null) {
					for (PromoterIdentity pi : identities) {
						if (pi != null && pi.getId() != null && pi.getName() != null) {
							identityNameById.put(pi.getId(), pi.getName());
						}
					}
				}
			}
		}

		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> row : list) {
			long pid = longFrom(row.get("id"));
			long uid = longFrom(row.get("user_id"));

			LinkedHashMap<String, Object> m = new LinkedHashMap<>(row);
			m.put("children_count", pidToCount.getOrDefault(pid, 0L));

			Integer createdInt = intOrNull(row.get("created"));
			if (createdInt != null) {
				m.put(
						"bind_date",
						Instant.ofEpochSecond(createdInt.longValue())
								.atZone(SHANGHAI)
								.toLocalDate()
								.format(BIND_DATE_FMT));
			} else {
				m.put("bind_date", "");
			}

			m.put("mobile", uid > 0L ? mobileByUser.getOrDefault(uid, "") : "");
			m.put("username", uid > 0L ? usernameByUser.getOrDefault(uid, "") : "");

			Map<String, String> wx = uid > 0L ? wechatByUser.get(uid) : null;
			if (wx != null) {
				m.put("nickname", wx.getOrDefault("nickname", ""));
				m.put("headimgurl", wx.getOrDefault("headimgurl", ""));
			} else {
				m.put("nickname", "");
				m.put("headimgurl", "");
			}

			Integer gradeLevel = intOrNull(row.get("grade_level"));
			m.put(
					"promoter_grade_name",
					promoterGradeService.readPromoterGradeDisplayName(companyId, gradeLevel));
			m.put("is_open_promoter_grade", isOpenStr);

			if (oemShuyun) {
				Long iid = longOrNull(row.get("identity_id"));
				if (iid != null && iid > 0L) {
					String iname = identityNameById.get(iid);
					if (iname != null) {
						m.put("identity_name", iname);
					}
				}
			}

			out.add(m);
		}
		return out;
	}

	private static boolean isNoMatchUserIdList(List<Long> userIds) {
		return userIds != null && userIds.size() == 1 && userIds.get(0).equals(-1L);
	}

	private static List<Long> extractInitialUserIdList(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof List<?> list) {
			ArrayList<Long> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				out.add(longFrom(o));
			}
			return out.isEmpty() ? null : out;
		}
		return List.of(longFrom(raw));
	}

	private static String readTrimmedString(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
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
}
