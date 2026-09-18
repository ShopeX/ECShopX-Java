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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.kaquan.service.membercard.MemberCardGradeQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeListQueryService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.repository.MemberPriceExportQueryRepository;
import cn.shopex.ecshopx.promotions.support.MemberPriceColumnCodec;
import cn.shopex.ecshopx.promotions.support.MemberTierDiscountPrice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGoodsItemsListMemberPriceApplyService {

	private final MemberPriceExportQueryRepository memberPriceExportQueryRepository;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final MemberCardGradeQueryService memberCardGradeQueryService;
	private final VipGradeListQueryService vipGradeListQueryService;
	private final MemberAccountService memberAccountService;
	private final ObjectMapper objectMapper;

	public WxappGoodsItemsListMemberPriceApplyService(MemberPriceExportQueryRepository memberPriceExportQueryRepository,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			MemberCardGradeQueryService memberCardGradeQueryService,
			VipGradeListQueryService vipGradeListQueryService,
			MemberAccountService memberAccountService,
			ObjectMapper objectMapper) {
		this.memberPriceExportQueryRepository = memberPriceExportQueryRepository;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.memberCardGradeQueryService = memberCardGradeQueryService;
		this.vipGradeListQueryService = vipGradeListQueryService;
		this.memberAccountService = memberAccountService;
		this.objectMapper = objectMapper;
	}

	/**
	 * 批量写回列表行的会员价相关字段：会员工具价 JSON 来自 {@link MemberPriceExportQueryRepository}；
	 * 当前用户等级/折扣通过 {@link VipGradeUserVipGradeGetService} 与 {@link MemberCardGradeQueryService} 解析。
	 *
	 * @param acceptLanguageHeader 与列表接口一致的 Accept-Language / 地区提示；当前会员价规则不区分语言，参数预留与调用方对齐
	 */
	public void applyForRows(long companyId, long userId, List<Map<String, Object>> rows, String acceptLanguageHeader) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		UserMemberPriceTier tier = userId > 0L ? resolveTier(companyId, userId) : resolveGuestTier(companyId);
		if (tier == null) {
			return;
		}
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Long id = itemIdFromRow(row);
			if (id != null) {
				itemIds.add(id);
			}
		}
		if (itemIds.isEmpty()) {
			for (Map<String, Object> row : rows) {
				row.put("member_grade_name", tier.gradeName());
			}
			return;
		}
		Map<Long, String> mpriceByItem = memberPriceExportQueryRepository.mapMpriceJsonByItemId(companyId, itemIds);
		List<Map<String, Object>> vipGradeRows = vipGradeListQueryService.listVipGradesForWxappMembercardGrades(companyId);
		String gradeBucket = "normal".equals(tier.lvType()) ? "grade" : "vipGrade";
		Map<Long, Integer> newPrice = new LinkedHashMap<>();
		Map<Long, Map<String, Integer>> vipPrice = new LinkedHashMap<>();
		for (Map.Entry<Long, String> en : mpriceByItem.entrySet()) {
			JsonNode root = parseMpriceRoot(en.getValue());
			if (root == null || !root.isObject()) {
				continue;
			}
			long itemId = en.getKey();
			int custom = tierPriceCents(root, gradeBucket, tier.gradeId());
			if (custom > 0) {
				newPrice.put(itemId, custom);
			}
			for (Map<String, Object> vg : vipGradeRows) {
				Long vgid = longObj(vg.get("vip_grade_id"));
				if (vgid == null || vgid <= 0L) {
					continue;
				}
				int vpc = tierPriceCents(root, "vipGrade", vgid);
				if (vpc > 0) {
					String lt = vg.get("lv_type") != null ? vg.get("lv_type").toString() : "vip";
					vipPrice.computeIfAbsent(itemId, k -> new LinkedHashMap<>()).put(lt, vpc);
				}
			}
		}
		for (Map<String, Object> item : rows) {
			Long itemId = itemIdFromRow(item);
			item.put("member_grade_name", tier.gradeName());
			long priceFen = longFromPrice(item.get("price"));
			if (itemId != null) {
				Integer np = newPrice.get(itemId);
				if (np != null && np > 0) {
					item.put("member_price", np);
				} else if (tier.discount() >= 0 && tier.discount() < 100) {
					item.put("member_price", MemberTierDiscountPrice.memberPriceFromTierDiscount(priceFen, tier.discount()));
				}
				Map<String, Integer> vp = vipPrice.getOrDefault(itemId, Map.of());
				for (Map<String, Object> vg : vipGradeRows) {
					Long vgid = longObj(vg.get("vip_grade_id"));
					if (vgid == null || vgid <= 0L) {
						continue;
					}
					String lt = vg.get("lv_type") != null ? vg.get("lv_type").toString() : "vip";
					String dynKey = lt + "_price";
					Integer customV = vp.get(lt);
					int vipDisc = parsePrivilegeDiscount(vg.get("privileges"));
					if (customV != null && customV > 0) {
						item.put(dynKey, customV);
					} else if (vipDisc >= 0 && vipDisc < 100) {
						item.put(dynKey, MemberTierDiscountPrice.memberPriceFromTierDiscount(priceFen, vipDisc));
					}
				}
			}
		}
	}

	/**
	 * 未登录访客：与默认会员卡等级对齐的展示名与折扣（无默认等级则跳过会员价回写）。
	 */
	private UserMemberPriceTier resolveGuestTier(long companyId) {
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
		return new UserMemberPriceTier(str(g.get("grade_name")), disc, defId, "normal");
	}

	private UserMemberPriceTier resolveTier(long companyId, long userId) {
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
			return new UserMemberPriceTier(gradeName, disc, gid, lv);
		}
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		Object gObj = memberInfo.get("grade_id");
		if (gObj != null) {
			long mid = longFromObject(gObj);
			if (mid > 0L) {
				Map<String, Object> card = findMemberCardGradeRow(companyId, mid);
				if (card != null) {
					int disc = parsePrivilegeDiscount(card.get("privileges"));
					return new UserMemberPriceTier(str(card.get("grade_name")), disc, mid, "normal");
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
				return new UserMemberPriceTier(str(g.get("grade_name")), disc, defId, "normal");
			}
		}
		return new UserMemberPriceTier("", 100, 0L, "normal");
	}

	private Map<String, Object> findMemberCardGradeRow(long companyId, long gradeId) {
		List<Map<String, Object>> rows = memberCardGradeQueryService.getGradeListByCompanyId(companyId, false);
		for (Map<String, Object> r : rows) {
			Object id = r.get("grade_id");
			if (id instanceof Number && ((Number) id).longValue() == gradeId) {
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

	private JsonNode parseMpriceRoot(String raw) {
		return MemberPriceColumnCodec.parseRoot(objectMapper, raw);
	}

	private static int tierPriceCents(JsonNode root, String bagName, long tierId) {
		if (tierId <= 0L) {
			return 0;
		}
		JsonNode bag = root.get(bagName);
		if (bag == null || !bag.isObject()) {
			return 0;
		}
		JsonNode val = bag.get(Long.toString(tierId));
		if (val == null) {
			val = bag.get(String.valueOf((int) tierId));
		}
		if (val == null || val.isNull()) {
			return 0;
		}
		if (val.isNumber()) {
			return (int) val.longValue();
		}
		if (val.isTextual()) {
			try {
				return Integer.parseInt(val.asText().trim());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private static long longFromPrice(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Long itemIdFromRow(Map<String, Object> row) {
		Object v = row.get("item_id");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			long l = n.longValue();
			return l > 0L ? l : null;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			long l = Long.parseLong(s);
			return l > 0L ? l : null;
		} catch (NumberFormatException e) {
			return null;
		}
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

	private static Long longObj(Object o) {
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		if (o != null) {
			try {
				long v = Long.parseLong(o.toString().trim());
				return v > 0L ? v : null;
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private record UserMemberPriceTier(String gradeName, int discount, long gradeId, String lvType) {}
}
