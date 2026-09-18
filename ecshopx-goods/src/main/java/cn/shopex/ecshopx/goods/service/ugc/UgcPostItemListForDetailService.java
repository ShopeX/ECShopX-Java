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

package cn.shopex.ecshopx.goods.service.ugc;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsQueryRepository;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import cn.shopex.ecshopx.kaquan.domain.MemberCardGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGrade;
import cn.shopex.ecshopx.kaquan.domain.VipGradeRelUser;
import cn.shopex.ecshopx.kaquan.mapper.MemberCardGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeMapper;
import cn.shopex.ecshopx.kaquan.mapper.VipGradeRelUserMapper;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.promotions.repository.MemberPriceExportQueryRepository;
import cn.shopex.ecshopx.promotions.support.MemberPriceColumnCodec;
import cn.shopex.ecshopx.promotions.support.MemberTierDiscountPrice;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UgcPostItemListForDetailService {

	private final ItemsQueryRepository itemsQueryRepository;
	private final MemberPriceExportQueryRepository memberPriceExportQueryRepository;
	private final VipGradeMapper vipGradeMapper;
	private final VipGradeRelUserMapper vipGradeRelUserMapper;
	private final MembersMapper membersMapper;
	private final MemberCardGradeMapper memberCardGradeMapper;
	private final ObjectMapper objectMapper;

	public UgcPostItemListForDetailService(
			ItemsQueryRepository itemsQueryRepository,
			MemberPriceExportQueryRepository memberPriceExportQueryRepository,
			VipGradeMapper vipGradeMapper,
			VipGradeRelUserMapper vipGradeRelUserMapper,
			MembersMapper membersMapper,
			MemberCardGradeMapper memberCardGradeMapper,
			ObjectMapper objectMapper) {
		this.itemsQueryRepository = itemsQueryRepository;
		this.memberPriceExportQueryRepository = memberPriceExportQueryRepository;
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
		this.membersMapper = membersMapper;
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> listForPostDetail(
			long companyId, List<Long> orderedItemIds, Long memberUserIdOrNull) {
		if (orderedItemIds == null || orderedItemIds.isEmpty()) {
			return Collections.emptyList();
		}
		List<Items> entities = itemsQueryRepository.listByCompanyIdAndItemIds(companyId, orderedItemIds);
		Map<Long, Map<String, Object>> byItemId = new LinkedHashMap<>();
		for (Items it : entities) {
			if (it.getItemId() == null) {
				continue;
			}
			Map<String, Object> row = new LinkedHashMap<>(GoodsItemsListRowMapper.toRow(it));
			row.put("distributor_info", Collections.emptyMap());
			byItemId.put(it.getItemId(), row);
		}
		List<Map<String, Object>> ordered = new ArrayList<>();
		for (Long id : orderedItemIds) {
			Map<String, Object> row = byItemId.get(id);
			if (row != null) {
				ordered.add(row);
			}
		}
		if (memberUserIdOrNull != null && memberUserIdOrNull > 0L && !ordered.isEmpty()) {
			applyItemsListMemberPrice(ordered, memberUserIdOrNull, companyId);
		}
		return ordered;
	}

	private void applyItemsListMemberPrice(List<Map<String, Object>> rows, long memberUserId, long companyId) {
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> row : rows) {
			Long id = itemIdFromRow(row);
			if (id != null && id > 0L) {
				itemIds.add(id);
			}
		}
		if (itemIds.isEmpty()) {
			return;
		}
		Map<Long, String> mpriceByItem = memberPriceExportQueryRepository.mapMpriceJsonByItemId(companyId, itemIds);
		List<VipGrade> vipGradeList = loadVipGrades(companyId);
		UserMemberPriceTier tier = resolveTier(companyId, memberUserId, vipGradeList);
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
			for (VipGrade vg : vipGradeList) {
				if (vg.getVipGradeId() == null) {
					continue;
				}
				int vpc = tierPriceCents(root, "vipGrade", vg.getVipGradeId());
				if (vpc > 0) {
					vipPrice.computeIfAbsent(itemId, k -> new LinkedHashMap<>()).put(
							vg.getLvType() != null ? vg.getLvType() : "vip", vpc);
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
				for (VipGrade vg : vipGradeList) {
					String lt = vg.getLvType() != null ? vg.getLvType() : "vip";
					String dynKey = lt + "_price";
					Integer customV = vp.get(lt);
					int vipDisc = parsePrivilegeDiscount(vg.getPrivileges());
					if (customV != null && customV > 0) {
						item.put(dynKey, customV);
					} else if (vipDisc >= 0 && vipDisc < 100) {
						item.put(dynKey, MemberTierDiscountPrice.memberPriceFromTierDiscount(priceFen, vipDisc));
					}
				}
			}
		}
	}

	private List<VipGrade> loadVipGrades(long companyId) {
		LambdaQueryWrapper<VipGrade> w = new LambdaQueryWrapper<>();
		w.apply("company_id = {0}", companyId);
		w.and(x -> x.eq(VipGrade::getIsDisabled, false).or().isNull(VipGrade::getIsDisabled));
		w.orderByAsc(VipGrade::getVipGradeId);
		return vipGradeMapper.selectList(w);
	}

	private UserMemberPriceTier resolveTier(long companyId, long userId, List<VipGrade> vipGradeList) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		List<VipGradeRelUser> rels = vipGradeRelUserMapper.selectList(
				new LambdaQueryWrapper<VipGradeRelUser>()
						.eq(VipGradeRelUser::getUserId, userId)
						.eq(VipGradeRelUser::getCompanyId, (int) companyId));
		LinkedHashMap<String, VipGradeRelUser> relByType = new LinkedHashMap<>();
		for (VipGradeRelUser r : rels) {
			if (r.getVipType() == null) {
				continue;
			}
			long end = parseEpochSeconds(r.getEndDate());
			if (end <= (long) now) {
				continue;
			}
			relByType.put(r.getVipType(), r);
		}
		VipGradeRelUser chosenRel =
				relByType.containsKey("svip") ? relByType.get("svip") : relByType.isEmpty() ? null
						: relByType.values().iterator().next();
		if (chosenRel != null) {
			VipGrade meta = null;
			Long relVipGradeId = chosenRel.getVipGradeId();
			for (VipGrade v : vipGradeList) {
				if (v.getLvType() != null
						&& v.getLvType().equals(chosenRel.getVipType())
						&& relVipGradeId != null
						&& Objects.equals(v.getVipGradeId(), relVipGradeId)) {
					meta = v;
					break;
				}
			}
			if (meta == null) {
				for (VipGrade v : vipGradeList) {
					if (v.getLvType() != null && v.getLvType().equals(chosenRel.getVipType())) {
						meta = v;
						break;
					}
				}
			}
			if (meta != null) {
				int disc = parsePrivilegeDiscount(meta.getPrivileges());
				long gid = relVipGradeId != null ? relVipGradeId : 0L;
				return new UserMemberPriceTier(
						meta.getGradeName() != null ? meta.getGradeName() : "",
						disc,
						gid,
						chosenRel.getVipType() != null ? chosenRel.getVipType() : "vip");
			}
		}
		Members m = membersMapper.selectOne(
				new LambdaQueryWrapper<Members>()
						.eq(Members::getUserId, userId)
						.eq(Members::getCompanyId, companyId)
						.last("LIMIT 1"));
		if (m != null && m.getGradeId() != null && m.getGradeId() > 0L) {
			MemberCardGrade g = memberCardGradeMapper.selectById(m.getGradeId());
			if (g != null && String.valueOf(companyId).equals(g.getCompanyId())) {
				int disc = parsePrivilegeDiscount(g.getPrivileges());
				return new UserMemberPriceTier(
						g.getGradeName() != null ? g.getGradeName() : "",
						disc,
						m.getGradeId(),
						"normal");
			}
		}
		Long defGradeId = membersMapper.selectDefaultGradeId(String.valueOf(companyId));
		if (defGradeId != null && defGradeId > 0L) {
			MemberCardGrade g = memberCardGradeMapper.selectById(defGradeId);
			if (g != null) {
				int disc = parsePrivilegeDiscount(g.getPrivileges());
				return new UserMemberPriceTier(
						g.getGradeName() != null ? g.getGradeName() : "",
						disc,
						defGradeId,
						"normal");
			}
		}
		return new UserMemberPriceTier("", 100, 0L, "normal");
	}

	private int parsePrivilegeDiscount(String privilegesJson) {
		if (!StringUtils.hasText(privilegesJson)) {
			return 100;
		}
		try {
			JsonNode n = objectMapper.readTree(privilegesJson);
			if (n != null && n.has("discount") && n.get("discount").canConvertToInt()) {
				return n.get("discount").asInt();
			}
		} catch (Exception ignored) {
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

	private static long parseEpochSeconds(String endDate) {
		if (!StringUtils.hasText(endDate)) {
			return 0L;
		}
		try {
			return Long.parseLong(endDate.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private record UserMemberPriceTier(String gradeName, int discount, long gradeId, String lvType) {}
}
