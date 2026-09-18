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

package cn.shopex.ecshopx.goods.service.operatorcart;

import cn.shopex.ecshopx.common.operatorcart.dto.OperatorCartSkuRowDto;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Applies cart line unit prices after member / VIP tier rules (cart member price + valid user grade lookup),
 * without company default grade fallback when the user has no grade.
 */
@Service
public class OperatorCartMemberPriceApplicator {

	private final MemberPriceExportQueryRepository memberPriceExportQueryRepository;
	private final VipGradeMapper vipGradeMapper;
	private final VipGradeRelUserMapper vipGradeRelUserMapper;
	private final MembersMapper membersMapper;
	private final MemberCardGradeMapper memberCardGradeMapper;
	private final ObjectMapper objectMapper;

	public OperatorCartMemberPriceApplicator(
			MemberPriceExportQueryRepository memberPriceExportQueryRepository,
			VipGradeMapper vipGradeMapper,
			VipGradeRelUserMapper vipGradeRelUserMapper,
			MembersMapper membersMapper,
			MemberCardGradeMapper memberCardGradeMapper,
			ObjectMapper objectMapper) {
		this.memberPriceExportQueryRepository = memberPriceExportQueryRepository;
		this.vipGradeMapper = vipGradeMapper;
		this.vipGradeRelUserMapper = vipGradeRelUserMapper;
		this.membersMapper = membersMapper;
		this.memberCardGradeMapper = memberCardGradeMapper;
		this.objectMapper = objectMapper;
	}

	public void apply(long companyId, long targetUserId, List<OperatorCartSkuRowDto> rows) {
		if (rows == null || rows.isEmpty() || targetUserId <= 0L) {
			return;
		}
		CartMemberTier tier = resolveCartMemberTier(companyId, targetUserId);
		if (tier == null) {
			return;
		}
		List<Long> itemIds = rows.stream().map(OperatorCartSkuRowDto::getItemId).distinct().toList();
		Map<Long, String> mpriceByItem = memberPriceExportQueryRepository.mapMpriceJsonByItemId(companyId, itemIds);
		if (mpriceByItem.isEmpty() && (tier.discount() <= 0 || tier.discount() >= 100)) {
			return;
		}
		String gradeBucket = "normal".equals(tier.lvType()) ? "grade" : "vipGrade";
		for (OperatorCartSkuRowDto row : rows) {
			int originalUnitFen = row.getUnitPriceFen();
			long itemId = row.getItemId();
			String rawJson = mpriceByItem.get(itemId);
			JsonNode root = parseMpriceRoot(rawJson);
			boolean usedCustomMemberPrice = false;
			if (root != null) {
				int custom = tierPriceCents(root, gradeBucket, tier.gradeId());
				if (custom > 0) {
					usedCustomMemberPrice = true;
					row.setUnitPriceFen(custom);
				}
			}
			if (!usedCustomMemberPrice && tier.discount() > 0 && tier.discount() < 100) {
				row.setUnitPriceFen(MemberTierDiscountPrice.memberPriceFromTierDiscount(originalUnitFen, tier.discount()));
			}
		}
	}

	/** Same resolution order as {@code getValidUserGradeUniqueByUserId}; {@code null} = no grade. */
	public ResolvedMemberTier resolveMemberTier(long companyId, long userId) {
		CartMemberTier tier = resolveCartMemberTier(companyId, userId);
		if (tier == null) {
			return null;
		}
		return new ResolvedMemberTier(tier.gradeName(), tier.discount(), tier.gradeId(), tier.lvType());
	}

	public static int tierPriceCentsFromRoot(JsonNode root, String bagName, long tierId) {
		return tierPriceCents(root, bagName, tierId);
	}

	public static int memberUnitPriceFromDiscount(int priceFen, int discount) {
		return MemberTierDiscountPrice.memberPriceFromTierDiscount(priceFen, discount);
	}

	/** Same resolution order as {@code getValidUserGradeUniqueByUserId}; {@code null} = no grade. */
	private CartMemberTier resolveCartMemberTier(long companyId, long userId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		List<VipGrade> vipGradeList = loadVipGrades(companyId);
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
			int day = (int) Math.ceil((parseEpochSeconds(chosenRel.getEndDate()) - (long) now) / 86400.0);
			boolean valid = day > 0;
			if (valid) {
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
					return new CartMemberTier(
							meta.getGradeName() != null ? meta.getGradeName() : "",
							disc,
							gid,
							chosenRel.getVipType() != null ? chosenRel.getVipType() : "vip");
				}
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
				return new CartMemberTier(
						g.getGradeName() != null ? g.getGradeName() : "",
						disc,
						m.getGradeId(),
						"normal");
			}
		}
		return null;
	}

	private List<VipGrade> loadVipGrades(long companyId) {
		LambdaQueryWrapper<VipGrade> w = new LambdaQueryWrapper<>();
		w.apply("company_id = {0}", companyId);
		w.and(x -> x.eq(VipGrade::getIsDisabled, false).or().isNull(VipGrade::getIsDisabled));
		w.orderByAsc(VipGrade::getVipGradeId);
		return vipGradeMapper.selectList(w);
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

	private record CartMemberTier(String gradeName, int discount, long gradeId, String lvType) {}

	public record ResolvedMemberTier(String gradeName, int discount, long gradeId, String lvType) {}
}
