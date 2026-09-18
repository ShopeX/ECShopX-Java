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

package cn.shopex.ecshopx.goods.service.cart.wxapp;

import cn.shopex.ecshopx.goods.service.operatorcart.OperatorCartMemberPriceApplicator;
import cn.shopex.ecshopx.goods.service.operatorcart.OperatorCartMemberPriceApplicator.ResolvedMemberTier;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.promotions.repository.MemberPriceExportQueryRepository;
import cn.shopex.ecshopx.promotions.support.MemberPriceColumnCodec;
import cn.shopex.ecshopx.promotions.support.MemberTierDiscountPrice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappH5CartMemberPriceEnrichmentService {

	private final OperatorCartMemberPriceApplicator operatorCartMemberPriceApplicator;
	private final MemberPriceExportQueryRepository memberPriceExportQueryRepository;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final ObjectMapper objectMapper;

	public WxappH5CartMemberPriceEnrichmentService(
			OperatorCartMemberPriceApplicator operatorCartMemberPriceApplicator,
			MemberPriceExportQueryRepository memberPriceExportQueryRepository,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			ObjectMapper objectMapper) {
		this.operatorCartMemberPriceApplicator = operatorCartMemberPriceApplicator;
		this.memberPriceExportQueryRepository = memberPriceExportQueryRepository;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.objectMapper = objectMapper;
	}

	public void apply(long companyId, long userId, List<Map<String, Object>> lines) {
		if (userId <= 0L || lines == null || lines.isEmpty()) {
			return;
		}
		List<Long> itemIds = new ArrayList<>();
		for (Map<String, Object> line : lines) {
			if ("package".equals(stringVal(line.get("activity_type")))) {
				continue;
			}
			if (truthy(line.get("is_last_price"))) {
				continue;
			}
			long itemId = longVal(line.get("item_id"));
			if (itemId > 0L) {
				itemIds.add(itemId);
			}
		}
		if (itemIds.isEmpty()) {
			return;
		}
		ResolvedMemberTier tier = operatorCartMemberPriceApplicator.resolveMemberTier(companyId, userId);
		if (tier == null) {
			return;
		}
		Map<Long, String> mpriceByItem =
				memberPriceExportQueryRepository.mapMpriceJsonByItemId(companyId, itemIds.stream().distinct().toList());
		if (mpriceByItem.isEmpty() && (tier.discount() <= 0 || tier.discount() >= 100)) {
			return;
		}
		Map<String, Object> userVipData = resolveUserVipDataForGuide(companyId, userId, tier);
		String gradeBucket = "normal".equals(tier.lvType()) ? "grade" : "vipGrade";
		Map<String, String> gradeType = Map.of("vip", "vipGrade", "svip", "vipGrade", "normal", "grade");

		for (Map<String, Object> line : lines) {
			if ("package".equals(stringVal(line.get("activity_type")))) {
				continue;
			}
			if (line.get("limitedTimeSaleAct") != null && truthy(line.get("limitedTimeSaleAct"))) {
				continue;
			}
			int unitPriceFen = intAmount(line.get("price"));
			int num = intAmount(line.get("num"));
			line.put("price", unitPriceFen);
			line.put("discount_desc", "");
			line.put("grade_name", tier.gradeName());

			long itemId = longVal(line.get("item_id"));
			JsonNode mpriceRoot = parseMpriceRoot(mpriceByItem.get(itemId));
			boolean issetMemberPrice = false;
			long discountFeeFen = 0L;
			if (mpriceRoot != null) {
				int memberUnitFen = OperatorCartMemberPriceApplicator.tierPriceCentsFromRoot(mpriceRoot, gradeBucket, tier.gradeId());
				if (memberUnitFen > 0) {
					issetMemberPrice = true;
					discountFeeFen = (long) (unitPriceFen - memberUnitFen) * num;
					line.put("member_price", memberUnitFen);
					line.put("member_discount", discountFeeFen);
					line.put("discount_fee", discountFeeFen);
					line.put("activity_info", List.of(memberPriceActivityInfo(discountFeeFen, true, tier.discount())));
				}
			}
			if (!issetMemberPrice && tier.discount() > 0 && tier.discount() < 100) {
				int discountPerUnitFen = MemberTierDiscountPrice.discountPerUnitFen(unitPriceFen, tier.discount());
				int memberUnitFen = unitPriceFen - discountPerUnitFen;
				discountFeeFen = (long) discountPerUnitFen * num;
				line.put("member_price", memberUnitFen);
				line.put("member_discount", discountFeeFen);
				line.put("discount_fee", discountFeeFen);
				line.put("activity_info", List.of(memberPriceActivityInfo(discountFeeFen, false, tier.discount())));
			}
			if (userVipData == null) {
				if (discountFeeFen > 0L && !"normal".equals(tier.lvType())) {
					line.put(
							"discount_desc",
							tier.gradeName() + "为您节省" + (discountFeeFen / 100) + "元");
				}
			} else {
				line.put("discount_desc", buildVipGuideDiscountDesc(line, userVipData, mpriceRoot, gradeType, num));
			}
		}
	}

	private Map<String, Object> resolveUserVipDataForGuide(
			long companyId, long userId, ResolvedMemberTier activeTier) {
		if (!"normal".equals(activeTier.lvType())) {
			return null;
		}
		Map<String, Object> vipRow = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		if (!Boolean.TRUE.equals(vipRow.get("is_open")) || Boolean.TRUE.equals(vipRow.get("is_vip"))) {
			return null;
		}
		return vipRow;
	}

	private String buildVipGuideDiscountDesc(
			Map<String, Object> line,
			Map<String, Object> userVipData,
			JsonNode mpriceRoot,
			Map<String, String> gradeType,
			int num) {
		long unitPriceFen = intAmount(line.get("price"));
		long vipGradeId = longVal(userVipData.get("vip_grade_id"));
		String lvType = stringVal(userVipData.get("lv_type"));
		String bag = gradeType.getOrDefault(lvType, "vipGrade");
		long memberDiscountUnit = 0L;
		if (mpriceRoot != null) {
			int vipCustom = OperatorCartMemberPriceApplicator.tierPriceCentsFromRoot(mpriceRoot, bag, vipGradeId);
			if (vipCustom > 0) {
				memberDiscountUnit = unitPriceFen - vipCustom;
			}
		}
		if (memberDiscountUnit == 0L) {
			int vipDiscount = intAmount(userVipData.get("discount"));
			memberDiscountUnit = MemberTierDiscountPrice.discountPerUnitFen((int) unitPriceFen, vipDiscount);
		}
		if (memberDiscountUnit <= 0L) {
			return "";
		}
		String gradeName = stringVal(userVipData.get("grade_name"));
		return "加入" + gradeName + "立省" + (memberDiscountUnit * num / 100) + "元";
	}

	private static Map<String, Object> memberPriceActivityInfo(long discountFeeFen, boolean customPrice, int discount) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", 0);
		row.put("type", "member_price");
		row.put("info", "会员价");
		if (customPrice) {
			BigDecimal yuan =
					BigDecimal.valueOf(discountFeeFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			row.put("rule", "会员价直减" + yuan.toPlainString());
		} else {
			row.put("rule", "会员折扣优惠" + (discount / 10));
		}
		row.put("discount_fee", discountFeeFen);
		return row;
	}

	private JsonNode parseMpriceRoot(String raw) {
		return MemberPriceColumnCodec.parseRoot(objectMapper, raw);
	}

	private static boolean truthy(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		String s = v.toString().trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private static int intAmount(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long longFen(Object raw) {
		return longVal(raw);
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
