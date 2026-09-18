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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.members.admin.AdminMemberVipGradeFilterUserIdsPort;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.members.service.admin.dto.AdminMemberBatchOperatingMemberQueryFilter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberBatchOperatingFilterService {

	private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final AdminMemberVipGradeFilterUserIdsPort adminMemberVipGradeFilterUserIdsPort;

	public AdminMemberBatchOperatingFilterService(
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			@Qualifier("adminMemberVipGradeFilterUserIdsPortImpl")
					AdminMemberVipGradeFilterUserIdsPort adminMemberVipGradeFilterUserIdsPort) {
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.adminMemberVipGradeFilterUserIdsPort = adminMemberVipGradeFilterUserIdsPort;
	}

	public AdminMemberBatchOperatingMemberQueryFilter buildFilter(
			long companyId, Map<String, Object> merged, Map<?, ?> operatorJwt) {
		AdminMemberBatchOperatingMemberQueryFilter f = new AdminMemberBatchOperatingMemberQueryFilter();
		f.setCompanyId(companyId);

		putMobileIfPresent(merged, f);
		putRemarksIfPresent(merged, f);
		putInviterIfPresent(merged, f);
		putUserCardCodeIfPresent(merged, f);
		putUsernameIfPresent(merged, f);
		putNameIfPresent(merged, f);
		putTimeRangeIfPresent(merged, f);
		putBirthdayRangeIfPresent(merged, f);
		putHaveConsumeIfPresent(merged, f);
		putPointIfPresent(merged, f);

		String vipGradeExpr = "";
		Object gradeRaw = merged.get("grade_id");
		if (gradeRaw != null && StringUtils.hasText(String.valueOf(gradeRaw).trim())) {
			String gs = String.valueOf(gradeRaw).trim();
			if (gs.chars().allMatch(Character::isDigit)) {
				try {
					f.setMembersGradeId(Long.parseLong(gs));
					vipGradeExpr = "notvip";
				} catch (NumberFormatException ignored) {
					vipGradeExpr = gs;
				}
			} else {
				vipGradeExpr = gs;
			}
		}
		Object vg = merged.get("vip_grade");
		if (vg != null && StringUtils.hasText(String.valueOf(vg).trim())) {
			vipGradeExpr = String.valueOf(vg).trim();
		}

		List<Long> shopIds = resolveShopIds(merged);
		List<Long> distributorIds = resolveDistributorIds(merged);
		f.setShopIds(shopIds.isEmpty() ? null : shopIds);
		f.setDistributorIds(distributorIds.isEmpty() ? null : distributorIds);

		List<Long> postUserIds = extractUserIdListFromMerged(merged);

		List<Long> vipUserIds = null;
		boolean vipUserIdsDefined = false;
		if (StringUtils.hasText(vipGradeExpr)) {
			if ("notvip".equalsIgnoreCase(vipGradeExpr)) {
				List<Long> ids =
						new ArrayList<>(
								adminMemberVipGradeFilterUserIdsPort
										.listUserIdsMatchingVipGradeFilter(companyId, "notvip")
										.stream()
										.filter(Objects::nonNull)
										.distinct()
										.toList());
				vipUserIdsDefined = true;
				List<Long> resolved;
				if (!ids.isEmpty() && !postUserIds.isEmpty()) {
					resolved = postUserIds.stream().filter(ids::contains).distinct().toList();
					if (resolved.isEmpty()) {
						return null;
					}
				} else if (!ids.isEmpty()) {
					resolved = new ArrayList<>(ids);
				} else {
					resolved = List.of(0L);
				}
				f.setUserIdsNotIn(resolved);
				f.setUserIdsIn(null);
			} else {
				List<Long> ids =
						adminMemberVipGradeFilterUserIdsPort.listUserIdsMatchingVipGradeFilter(companyId, vipGradeExpr);
				vipUserIdsDefined = true;
				vipUserIds = ids == null ? new ArrayList<>() : new ArrayList<>(ids.stream().filter(Objects::nonNull).distinct().toList());
				if (!postUserIds.isEmpty()) {
					vipUserIds.retainAll(postUserIds);
				}
				if (vipUserIds.isEmpty()) {
					f.setUserIdsIn(List.of(0L));
				} else {
					f.setUserIdsIn(vipUserIds);
				}
			}
		} else if (!postUserIds.isEmpty()) {
			f.setUserIdsIn(postUserIds);
		}

		putTagIdsIfPresent(merged, f);

		return f;
	}

	private void putPointIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Long eq = firstLongFromMerged(merged, "point", "point_eq", "pointEq");
		Long gte = firstLongFromMerged(merged, "point|gte", "point_gte", "pointGte", "point_start");
		Long lte = firstLongFromMerged(merged, "point|lte", "point_lte", "pointLte", "point_end");
		if (eq != null) {
			f.setPointEq(eq);
		}
		if (gte != null) {
			f.setPointGte(gte);
		}
		if (lte != null) {
			f.setPointLte(lte);
		}
	}

	private static Long firstLongFromMerged(Map<String, Object> merged, String... keys) {
		for (String k : keys) {
			Object v = merged.get(k);
			if (v == null) {
				continue;
			}
			if (v instanceof Number n) {
				return n.longValue();
			}
			String s = String.valueOf(v).trim();
			if (!StringUtils.hasText(s)) {
				continue;
			}
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	private static List<Long> extractUserIdListFromMerged(Map<String, Object> merged) {
		Object raw = merged.get("user_id");
		if (raw == null) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		if (raw instanceof Iterable<?> it && !(raw instanceof String)) {
			for (Object el : it) {
				if (el == null) {
					continue;
				}
				try {
					out.add(Long.parseLong(String.valueOf(el).trim()));
				} catch (NumberFormatException ignored) {
				}
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s.trim())) {
			for (String p : s.split(",")) {
				String t = p.trim();
				if (t.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(t));
				} catch (NumberFormatException ignored) {
				}
			}
			return out;
		}
		if (raw instanceof Number n) {
			out.add(n.longValue());
		}
		return out;
	}

	private void putTagIdsIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Object raw = merged.get("tag_id");
		if (raw == null) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		if (raw instanceof String s && StringUtils.hasText(s)) {
			if (s.contains(",")) {
				for (String p : s.split(",")) {
					String t = p.trim();
					if (!t.isEmpty()) {
						try {
							ids.add(Long.parseLong(t));
						} catch (NumberFormatException ignored) {
						}
					}
				}
			} else {
				try {
					ids.add(Long.parseLong(s.trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		} else if (raw instanceof Iterable<?> it) {
			for (Object el : it) {
				if (el == null) {
					continue;
				}
				try {
					ids.add(Long.parseLong(String.valueOf(el).trim()));
				} catch (NumberFormatException ignored) {
				}
			}
		} else if (raw instanceof Number n) {
			ids.add(n.longValue());
		}
		if (!ids.isEmpty()) {
			f.setTagIds(ids);
		}
	}

	private void putMobileIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Object v = merged.get("mobile");
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			return;
		}
		f.setMobileEqEncrypted(sensitiveFieldEncryptor.encrypt(String.valueOf(v).trim()));
	}

	private void putRemarksIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Object v = merged.get("remarks");
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			return;
		}
		f.setRemarksLike(String.valueOf(v).trim());
	}

	private void putInviterIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Object v = merged.get("inviter_id");
		if (v == null) {
			return;
		}
		try {
			f.setInviterId(Long.parseLong(String.valueOf(v).trim()));
		} catch (NumberFormatException ignored) {
		}
	}

	private void putUserCardCodeIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Object v = merged.get("user_card_code");
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			return;
		}
		f.setUserCardCode(String.valueOf(v).trim());
	}

	private void putUsernameIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Object v = merged.get("username");
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			return;
		}
		f.setUsernameEqEncrypted(sensitiveFieldEncryptor.encrypt(String.valueOf(v).trim()));
	}

	private void putNameIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Object v = merged.get("name");
		if (v == null || !StringUtils.hasText(String.valueOf(v).trim())) {
			return;
		}
		f.setNameEq(String.valueOf(v).trim());
	}

	private void putTimeRangeIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Object b = merged.get("time_start_begin");
		if (b == null || !StringUtils.hasText(String.valueOf(b).trim())) {
			return;
		}
		Long gte = DateExpressionParser.parseToEpochSecond(b, DEFAULT_ZONE);
		Object e = merged.get("time_start_end");
		Long lte = e == null ? null : DateExpressionParser.parseToEpochSecond(e, DEFAULT_ZONE);
		f.setCreatedGte(gte);
		f.setCreatedLte(lte);
	}

	private void putBirthdayRangeIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Object bs = merged.get("birthday_start");
		if (bs != null && StringUtils.hasText(String.valueOf(bs).trim())) {
			Long sec = DateExpressionParser.parseToEpochSecond(bs, DEFAULT_ZONE);
			if (sec != null) {
				f.setBirthdayGte(
						DateTimeFormatter.ISO_LOCAL_DATE.format(
								java.time.Instant.ofEpochSecond(sec).atZone(DEFAULT_ZONE)));
			}
		}
		Object be = merged.get("birthday_end");
		if (be != null && StringUtils.hasText(String.valueOf(be).trim())) {
			Long sec = DateExpressionParser.parseToEpochSecond(be, DEFAULT_ZONE);
			if (sec != null) {
				f.setBirthdayLte(
						DateTimeFormatter.ISO_LOCAL_DATE.format(
								java.time.Instant.ofEpochSecond(sec).atZone(DEFAULT_ZONE)));
			}
		}
	}

	private void putHaveConsumeIfPresent(Map<String, Object> merged, AdminMemberBatchOperatingMemberQueryFilter f) {
		Object v = merged.get("have_consume");
		if (v == null) {
			return;
		}
		String s = String.valueOf(v).trim();
		if ("true".equalsIgnoreCase(s)) {
			f.setHaveConsume(Boolean.TRUE);
		} else if ("false".equalsIgnoreCase(s)) {
			f.setHaveConsume(Boolean.FALSE);
		}
	}

	private static List<Long> resolveShopIds(Map<String, Object> merged) {
		return parseIdListFlexible(merged.get("shop_id"));
	}

	private static List<Long> resolveDistributorIds(Map<String, Object> merged) {
		return parseIdListFlexible(merged.get("distributor_id"));
	}

	private static List<Long> parseIdListFlexible(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v == 0L ? List.of() : List.of(v);
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "0".equals(t)) {
				return List.of();
			}
			List<Long> out = new ArrayList<>();
			for (String p : t.split(",")) {
				String x = p.trim();
				if (x.isEmpty()) {
					continue;
				}
				try {
					out.add(Long.parseLong(x));
				} catch (NumberFormatException ignored) {
				}
			}
			return out;
		}
		if (raw instanceof Iterable<?> it) {
			List<Long> out = new ArrayList<>();
			for (Object el : it) {
				if (el == null) {
					continue;
				}
				try {
					long v = Long.parseLong(String.valueOf(el).trim());
					if (v != 0L) {
						out.add(v);
					}
				} catch (NumberFormatException ignored) {
				}
			}
			return out;
		}
		return List.of();
	}
}
