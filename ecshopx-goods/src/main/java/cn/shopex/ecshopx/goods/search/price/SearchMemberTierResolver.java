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

package cn.shopex.ecshopx.goods.search.price;

import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 解析搜索列表算价所需会员档位，规则对齐
 * {@link cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService}
 * 的 {@code resolveGuestTier}/{@code resolveTier}。
 */
@Service
public class SearchMemberTierResolver {

	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final MemberCardGradeQueryService memberCardGradeQueryService;
	private final MemberAccountService memberAccountService;
	private final ObjectMapper objectMapper;

	public SearchMemberTierResolver(
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			MemberCardGradeQueryService memberCardGradeQueryService,
			MemberAccountService memberAccountService,
			ObjectMapper objectMapper) {
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.memberCardGradeQueryService = memberCardGradeQueryService;
		this.memberAccountService = memberAccountService;
		this.objectMapper = objectMapper;
	}

	/**
	 * @param userId {@code <=0} 走游客默认等级（{@link MemberCardGradeQueryService#getDefaultGradeData}）
	 * @return 无可用档位时 {@code null}（游客无默认等级）
	 */
	@Nullable
	public SearchMemberTier resolve(long companyId, long userId) {
		if (userId <= 0L) {
			return resolveGuestTier(companyId);
		}
		return resolveTier(companyId, userId);
	}

	/** 对齐 WxappGoodsItemsListMemberPriceApplyService.resolveGuestTier */
	@Nullable
	private SearchMemberTier resolveGuestTier(long companyId) {
		Object def = memberCardGradeQueryService.getDefaultGradeData(companyId);
		if (!(def instanceof Map<?, ?>)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> g = (Map<String, Object>) def;
		long defId = longFromObject(g.get("grade_id"));
		if (defId <= 0L) {
			return null;
		}
		int disc = parsePrivilegeDiscount(g.get("privileges"));
		return new SearchMemberTier("normal", defId, disc, str(g.get("grade_name")));
	}

	/** 对齐 WxappGoodsItemsListMemberPriceApplyService.resolveTier */
	private SearchMemberTier resolveTier(long companyId, long userId) {
		Map<String, Object> vipRow = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		if (Boolean.TRUE.equals(vipRow.get("is_vip"))) {
			String gradeName = str(vipRow.get("grade_name"));
			int disc = intFromObject(vipRow.get("discount"));
			if (disc == 0) {
				disc = 100;
			}
			long gid = longFromObject(vipRow.get("vip_grade_id"));
			String lv = str(vipRow.get("lv_type"));
			if (lv.isEmpty()) {
				lv = "vip";
			}
			return new SearchMemberTier(lv, gid, disc, gradeName);
		}
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		Object gObj = memberInfo.get("grade_id");
		if (gObj != null) {
			long mid = longFromObject(gObj);
			if (mid > 0L) {
				Map<String, Object> card = findMemberCardGradeRow(companyId, mid);
				if (card != null) {
					int disc = parsePrivilegeDiscount(card.get("privileges"));
					return new SearchMemberTier("normal", mid, disc, str(card.get("grade_name")));
				}
			}
		}
		Object def = memberCardGradeQueryService.getDefaultGradeData(companyId);
		if (def instanceof Map<?, ?> dm) {
			@SuppressWarnings("unchecked")
			Map<String, Object> g = (Map<String, Object>) dm;
			long defId = longFromObject(g.get("grade_id"));
			if (defId > 0L) {
				int disc = parsePrivilegeDiscount(g.get("privileges"));
				return new SearchMemberTier("normal", defId, disc, str(g.get("grade_name")));
			}
		}
		return new SearchMemberTier("normal", 0L, 100, "");
	}

	private Map<String, Object> findMemberCardGradeRow(long companyId, long gradeId) {
		List<Map<String, Object>> rows = memberCardGradeQueryService.getGradeListByCompanyId(companyId, false);
		for (Map<String, Object> r : rows) {
			Object id = r.get("grade_id");
			if (id instanceof Number num && num.longValue() == gradeId) {
				return r;
			}
		}
		return null;
	}

	private int parsePrivilegeDiscount(Object privileges) {
		if (privileges == null) {
			return 100;
		}
		if (privileges instanceof Map<?, ?> m) {
			Object d = m.get("discount");
			if (d instanceof Number n) {
				return n.intValue();
			}
			if (d != null) {
				try {
					return Integer.parseInt(d.toString().trim());
				} catch (NumberFormatException ignored) {
					return 100;
				}
			}
			return 100;
		}
		if (privileges instanceof String s && StringUtils.hasText(s)) {
			try {
				JsonNode n = objectMapper.readTree(s);
				if (n != null && n.has("discount") && n.get("discount").canConvertToInt()) {
					return n.get("discount").asInt();
				}
			} catch (Exception ignored) {
			}
		}
		return 100;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static int intFromObject(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o != null) {
			try {
				return Integer.parseInt(o.toString().trim());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private static long longFromObject(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o != null) {
			try {
				return Long.parseLong(o.toString().trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
