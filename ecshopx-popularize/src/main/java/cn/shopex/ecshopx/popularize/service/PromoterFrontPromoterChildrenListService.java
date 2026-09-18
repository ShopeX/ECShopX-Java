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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.popularize.domain.Promoter;
import cn.shopex.ecshopx.popularize.domain.PromoterIdentity;
import cn.shopex.ecshopx.popularize.mapper.PromoterIdentityMapper;
import cn.shopex.ecshopx.popularize.mapper.PromoterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PromoterFrontPromoterChildrenListService {

	private static final int DEFAULT_RELATION_DEPTH = 2;
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter BIND_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final Set<String> LONG_FILTER_KEYS =
			Set.of("company_id", "user_id", "pid", "id", "promoter_id", "identity_id");
	private static final Set<String> INT_FILTER_KEYS = Set.of("is_buy", "is_promoter", "disabled");

	private final PromoterMapper promoterMapper;
	private final PromoterGradeService promoterGradeService;
	private final MemberAccountService memberAccountService;
	private final PromoterIdentityMapper promoterIdentityMapper;
	private final boolean oemShuyun;

	public PromoterFrontPromoterChildrenListService(
			PromoterMapper promoterMapper,
			PromoterGradeService promoterGradeService,
			MemberAccountService memberAccountService,
			PromoterIdentityMapper promoterIdentityMapper,
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.promoterMapper = promoterMapper;
		this.promoterGradeService = promoterGradeService;
		this.memberAccountService = memberAccountService;
		this.promoterIdentityMapper = promoterIdentityMapper;
		this.oemShuyun = oemShuyun;
	}

	public Map<String, Object> getPromoterchildrenList(
			LinkedHashMap<String, Object> filter, int page, int pageSize) {
		boolean hasUserId = filter.containsKey("user_id") && filter.get("user_id") != null;
		boolean hasPromoterId = filter.containsKey("promoter_id") && filter.get("promoter_id") != null;
		if (!hasUserId && !hasPromoterId) {
			throw new BadRequestException("参数错误");
		}
		if (!filter.containsKey("company_id") || filter.get("company_id") == null) {
			throw new BadRequestException("参数错误");
		}
		long companyId = ((Number) filter.get("company_id")).longValue();

		LinkedHashMap<String, Object> filterCopy = new LinkedHashMap<>(filter);
		Long rootId = null;
		if (hasPromoterId) {
			rootId = ((Number) filter.get("promoter_id")).longValue();
			filterCopy.remove("promoter_id");
		} else {
			long userId = ((Number) filter.get("user_id")).longValue();
			Long resolvedRoot = resolveRootPromoterIdFromUser(companyId, userId);
			if (resolvedRoot == null) {
				LinkedHashMap<String, Object> empty = new LinkedHashMap<>();
				empty.put("total_count", 0L);
				empty.put("list", List.of());
				return empty;
			}
			rootId = resolvedRoot;
			filterCopy.remove("user_id");
		}

		List<Map<String, Object>> collected =
				collectRelationRowsBfs(companyId, rootId, filterCopy, DEFAULT_RELATION_DEPTH);
		collected.sort(
				(a, b) -> {
					Integer ca = extractCreatedEpoch(a.get("created"));
					Integer cb = extractCreatedEpoch(b.get("created"));
					if (ca == null && cb == null) {
						return 0;
					}
					if (ca == null) {
						return 1;
					}
					if (cb == null) {
						return -1;
					}
					return Integer.compare(cb, ca);
				});

		List<Map<String, Object>> filtered = new ArrayList<>();
		for (Map<String, Object> row : collected) {
			if (rowMatchesFilterCopy(row, filterCopy)) {
				filtered.add(row);
			}
		}

		long totalLong = filtered.size();
		int offset = (page - 1) * pageSize;
		if (offset < 0) {
			offset = 0;
		}
		int from = Math.min(offset, filtered.size());
		int to = Math.min(offset + pageSize, filtered.size());
		List<Map<String, Object>> pageRows = filtered.subList(from, to);

		if (pageRows.isEmpty()) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", totalLong);
			out.put("list", List.of());
			return out;
		}

		List<Map<String, Object>> formatted = formatPageRows(companyId, pageRows);
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalLong);
		result.put("list", formatted);
		return result;
	}

	public int relationChildrenCountByUserId(long companyId, long userId, int isBuy) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("user_id", userId);
		filter.put("is_buy", isBuy);

		Long rootId = resolveRootPromoterIdFromUser(companyId, userId);
		if (rootId == null) {
			return 0;
		}

		LinkedHashMap<String, Object> filterCopy = new LinkedHashMap<>(filter);
		filterCopy.remove("user_id");

		List<Map<String, Object>> collected =
				collectRelationRowsBfs(companyId, rootId, filterCopy, DEFAULT_RELATION_DEPTH);
		int n = 0;
		for (Map<String, Object> row : collected) {
			if (rowMatchesFilterCopy(row, filterCopy)) {
				n++;
			}
		}
		return n;
	}

	/**
	 * 下级推广员数量（关系树 BFS，深度由参数指定；与历史 {@code getPromoterchildrenCount(filter, depth)} 语义对齐）。
	 *
	 * @param filter 必须含 {@code company_id}；且含 {@code user_id} 或 {@code promoter_id}；可含 {@code is_promoter} 等附加过滤键
	 * @param depth 关系展开层数；OEM {@code child_promoter_num} 固定传入 {@code 1}
	 * @return 过滤后的下级行数，等价列表接口中的 {@code total_count}
	 */
	public long getPromoterchildrenCount(LinkedHashMap<String, Object> filter, int depth) {
		boolean hasUserId = filter.containsKey("user_id") && filter.get("user_id") != null;
		boolean hasPromoterId = filter.containsKey("promoter_id") && filter.get("promoter_id") != null;
		if (!hasUserId && !hasPromoterId) {
			throw new BadRequestException("参数错误");
		}
		if (!filter.containsKey("company_id") || filter.get("company_id") == null) {
			throw new BadRequestException("参数错误");
		}
		long companyId = ((Number) filter.get("company_id")).longValue();

		LinkedHashMap<String, Object> filterCopy = new LinkedHashMap<>(filter);
		Long rootId = null;
		if (hasPromoterId) {
			rootId = ((Number) filter.get("promoter_id")).longValue();
			filterCopy.remove("promoter_id");
		} else {
			long userId = ((Number) filter.get("user_id")).longValue();
			Long resolvedRoot = resolveRootPromoterIdFromUser(companyId, userId);
			if (resolvedRoot == null) {
				return 0L;
			}
			rootId = resolvedRoot;
			filterCopy.remove("user_id");
		}

		List<Map<String, Object>> collected = collectRelationRowsBfs(companyId, rootId, filterCopy, depth);
		long n = 0L;
		for (Map<String, Object> row : collected) {
			if (rowMatchesFilterCopy(row, filterCopy)) {
				n++;
			}
		}
		return n;
	}

	private Long resolveRootPromoterIdFromUser(long companyId, long userId) {
		Promoter self =
				promoterMapper.selectOne(
						new LambdaQueryWrapper<Promoter>()
								.eq(Promoter::getCompanyId, companyId)
								.eq(Promoter::getUserId, userId)
								.last("LIMIT 1"));
		if (self == null || self.getId() == null) {
			return null;
		}
		return self.getId();
	}

	private List<Map<String, Object>> collectRelationRowsBfs(
			long companyId, long rootId, LinkedHashMap<String, Object> filterCopy, int maxDepth) {
		List<Map<String, Object>> all = new ArrayList<>();
		List<Long> frontier = new ArrayList<>(List.of(rootId));
		int remainingDepth = maxDepth;
		int relationshipDepth = 0;

		while (!frontier.isEmpty() && remainingDepth > 0) {
			relationshipDepth++;
			LambdaQueryWrapper<Promoter> w =
					new LambdaQueryWrapper<Promoter>()
							.eq(Promoter::getCompanyId, companyId)
							.in(Promoter::getPid, frontier);
			if (filterCopy.containsKey("disabled") && filterCopy.get("disabled") != null) {
				w = w.eq(Promoter::getDisabled, ((Number) filterCopy.get("disabled")).intValue());
			}
			if (filterCopy.containsKey("is_buy") && filterCopy.get("is_buy") != null) {
				w = w.eq(Promoter::getIsBuy, ((Number) filterCopy.get("is_buy")).intValue());
			}
			if (filterCopy.containsKey("is_promoter") && filterCopy.get("is_promoter") != null) {
				w = w.eq(Promoter::getIsPromoter, ((Number) filterCopy.get("is_promoter")).intValue());
			}
			w.orderByDesc(Promoter::getCreated);
			List<Promoter> batch = promoterMapper.selectList(w);

			LinkedHashSet<Long> nextFrontier = new LinkedHashSet<>();
			for (Promoter p : batch) {
				Map<String, Object> row = promoterEntityToRowMap(p, relationshipDepth);
				all.add(row);
				if (row.containsKey("id") && row.get("id") != null) {
					long childId = ((Number) row.get("id")).longValue();
					if (childId > 0L) {
						nextFrontier.add(childId);
					}
				}
			}
			frontier = new ArrayList<>(nextFrontier);
			remainingDepth--;
		}
		return all;
	}

	private static LinkedHashMap<String, Object> promoterEntityToRowMap(Promoter p, int relationshipDepth) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("relationship_depth", relationshipDepth);
		Long id = p.getId();
		row.put("id", id);
		row.put("promoter_id", id);
		row.put("company_id", p.getCompanyId());
		row.put("user_id", p.getUserId());
		row.put("pid", p.getPid());
		row.put("grade_level", p.getGradeLevel());
		row.put("shop_status", p.getShopStatus());
		row.put("is_promoter", p.getIsPromoter());
		row.put("disabled", p.getDisabled());
		row.put("is_buy", p.getIsBuy());
		row.put("created", p.getCreated());
		row.put("identity_id", p.getIdentityId());
		row.put("pmobile", p.getPmobile() != null ? p.getPmobile() : "");
		return row;
	}

	private static Integer extractCreatedEpoch(Object created) {
		if (created == null) {
			return null;
		}
		if (created instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(created).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static boolean rowMatchesFilterCopy(Map<String, Object> row, LinkedHashMap<String, Object> filterCopy) {
		for (Map.Entry<String, Object> e : filterCopy.entrySet()) {
			String key = e.getKey();
			if ("user_id".equals(key) || "promoter_id".equals(key)) {
				continue;
			}
			if (!filterCopy.containsKey(key) || filterCopy.get(key) == null) {
				continue;
			}
			Object fv = e.getValue();
			if (LONG_FILTER_KEYS.contains(key)) {
				if (!row.containsKey(key) || row.get(key) == null) {
					return false;
				}
				try {
					long fl = toLongLoose(fv);
					long rl = toLongLoose(row.get(key));
					if (fl != rl) {
						return false;
					}
				} catch (NumberFormatException ex) {
					return false;
				}
			} else if (INT_FILTER_KEYS.contains(key)) {
				if (!row.containsKey(key) || row.get(key) == null) {
					return false;
				}
				if (!looseIntEquals(row.get(key), fv)) {
					return false;
				}
			} else {
				if (!row.containsKey(key) || row.get(key) == null) {
					return false;
				}
				if (!Objects.deepEquals(String.valueOf(row.get(key)), String.valueOf(fv))) {
					return false;
				}
			}
		}
		return true;
	}

	private static long toLongLoose(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static boolean looseIntEquals(Object rowVal, Object filterVal) {
		int a = toIntNormalized(rowVal);
		int b = toIntNormalized(filterVal);
		return a == b;
	}

	private static int toIntNormalized(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}

	private List<Map<String, Object>> formatPageRows(long companyId, List<Map<String, Object>> pageRows) {
		boolean isOpen = promoterGradeService.readIsOpenPromoterGrade(companyId);
		String isOpenStr = isOpen ? "true" : "false";

		List<Long> userIds = new ArrayList<>();
		for (Map<String, Object> row : pageRows) {
			if (row.containsKey("user_id") && row.get("user_id") != null) {
				long uid = ((Number) row.get("user_id")).longValue();
				if (uid > 0L && !userIds.contains(uid)) {
					userIds.add(uid);
				}
			}
		}

		Map<Long, String> usernameByUser = new HashMap<>();
		Map<Long, String> mobileByUser = new HashMap<>();
		for (Map<String, Object> sum : memberAccountService.listMemberSummariesByUserIds(companyId, userIds)) {
			if (sum == null) {
				continue;
			}
			Object uidObj = sum.get("user_id");
			if (uidObj == null) {
				continue;
			}
			long uid = uidObj instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(uidObj).trim());
			usernameByUser.put(
					uid, sum.get("username") != null ? String.valueOf(sum.get("username")) : "");
			mobileByUser.put(uid, sum.get("mobile") != null ? String.valueOf(sum.get("mobile")) : "");
		}

		Map<Long, Map<String, String>> wechatByUser =
				memberAccountService.batchWechatNicknameHeadByUserIds(companyId, userIds);

		List<Long> pidList = new ArrayList<>();
		for (Map<String, Object> row : pageRows) {
			if (row.containsKey("id") && row.get("id") != null) {
				pidList.add(((Number) row.get("id")).longValue());
			}
		}

		Map<Long, Long> pidToCount = new HashMap<>();
		if (!pidList.isEmpty()) {
			List<Map<String, Object>> cntRows =
					promoterMapper.countDirectChildrenByPidList(companyId, pidList);
			for (Map<String, Object> r : cntRows) {
				if (r == null) {
					continue;
				}
				Object pidO = r.get("pid");
				Object cntO = r.get("cnt");
				if (!r.containsKey("pid") || pidO == null || !r.containsKey("cnt") || cntO == null) {
					continue;
				}
				long pid =
						pidO instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(pidO).trim());
				long cnt =
						cntO instanceof Number n
								? n.longValue()
								: Long.parseLong(String.valueOf(cntO).trim());
				pidToCount.put(pid, cnt);
			}
		}

		Map<Long, String> identityNameById = new HashMap<>();
		if (oemShuyun) {
			List<Long> identityIds = new ArrayList<>();
			for (Map<String, Object> row : pageRows) {
				if (row.containsKey("identity_id") && row.get("identity_id") != null) {
					long iid = ((Number) row.get("identity_id")).longValue();
					if (iid > 0L && !identityIds.contains(iid)) {
						identityIds.add(iid);
					}
				}
			}
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

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (Map<String, Object> src : pageRows) {
			long rowId =
					src.containsKey("id") && src.get("id") != null
							? ((Number) src.get("id")).longValue()
							: 0L;
			long uid =
					src.containsKey("user_id") && src.get("user_id") != null
							? ((Number) src.get("user_id")).longValue()
							: 0L;

			LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
			merged.put("username", uid > 0L ? usernameByUser.getOrDefault(uid, "") : "");
			merged.put("mobile", uid > 0L ? mobileByUser.getOrDefault(uid, "") : "");
			LinkedHashMap<String, Object> promoterPart = new LinkedHashMap<>(src);
			merged.putAll(promoterPart);

			merged.put("children_count", pidToCount.getOrDefault(rowId, 0L));

			Object createdObj = src.get("created");
			if (src.containsKey("created") && createdObj != null) {
				int createdSec =
						createdObj instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(createdObj).trim());
				merged.put(
						"bind_date",
						Instant.ofEpochSecond(createdSec).atZone(SHANGHAI).toLocalDate().format(BIND_DATE_FMT));
			} else {
				merged.put("bind_date", "");
			}

			Integer gradeLevel = null;
			if (src.containsKey("grade_level") && src.get("grade_level") != null) {
				Object gradeObj = src.get("grade_level");
				if (gradeObj instanceof Number n) {
					gradeLevel = n.intValue();
				} else {
					gradeLevel = Integer.parseInt(String.valueOf(gradeObj).trim());
				}
			}
			merged.put("promoter_grade_name", promoterGradeService.readPromoterGradeDisplayName(companyId, gradeLevel));
			merged.put("is_open_promoter_grade", isOpenStr);

			Map<String, String> wx = uid > 0L ? wechatByUser.get(uid) : null;
			if (wx != null) {
				merged.put("nickname", wx.getOrDefault("nickname", ""));
				merged.put("headimgurl", wx.getOrDefault("headimgurl", ""));
			} else {
				merged.put("nickname", "");
				merged.put("headimgurl", "");
			}

			String regionMobile = "";
			if (src.containsKey("user_id") && src.get("user_id") != null && uid > 0L) {
				Map<String, Object> info = memberAccountService.getMemberInfo(uid, companyId);
				if (info.containsKey("region_mobile") && info.get("region_mobile") != null) {
					String s = String.valueOf(info.get("region_mobile")).trim();
					if (!s.isEmpty()) {
						regionMobile = s;
					}
				}
			}
			merged.put("region_mobile", regionMobile);

			if (oemShuyun
					&& src.containsKey("identity_id")
					&& src.get("identity_id") != null) {
				long iid = ((Number) src.get("identity_id")).longValue();
				if (iid > 0L) {
					String iname = identityNameById.get(iid);
					if (iname != null) {
						merged.put("identity_name", iname);
					}
				}
			}

			listMaps.add(merged);
		}
		return listMaps;
	}
}
