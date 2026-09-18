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

package cn.shopex.ecshopx.promotions.service.register;

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.promotions.domain.DistributorPromotions;
import cn.shopex.ecshopx.promotions.domain.RegisterPromotions;
import cn.shopex.ecshopx.promotions.mapper.DistributorPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.RegisterPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Aligns with PHP {@code DistributorPromotionService::executionMarketing}: resolve {@code register_promotions}
 * (distributor-specific first, then {@code general}) and apply items/coupons/staff_coupons.
 */
@Service
public class RegisterPromotionsExecutionMarketingService {

	private static final Logger log = LoggerFactory.getLogger(RegisterPromotionsExecutionMarketingService.class);

	private final RegisterPromotionsMapper registerPromotionsMapper;
	private final DistributorPromotionsMapper distributorPromotionsMapper;
	private final ObjectMapper objectMapper;
	private final RegisterPromotionMembercardItemsApplyService registerPromotionMembercardItemsApplyService;
	private final RegisterPromotionMembercardCouponsApplyService registerPromotionMembercardCouponsApplyService;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final MemberTagsMapper memberTagsMapper;

	public RegisterPromotionsExecutionMarketingService(
			RegisterPromotionsMapper registerPromotionsMapper,
			DistributorPromotionsMapper distributorPromotionsMapper,
			ObjectMapper objectMapper,
			RegisterPromotionMembercardItemsApplyService registerPromotionMembercardItemsApplyService,
			RegisterPromotionMembercardCouponsApplyService registerPromotionMembercardCouponsApplyService,
			MemberRelTagsMapper memberRelTagsMapper,
			MemberTagsMapper memberTagsMapper) {
		this.registerPromotionsMapper = registerPromotionsMapper;
		this.distributorPromotionsMapper = distributorPromotionsMapper;
		this.objectMapper = objectMapper;
		this.registerPromotionMembercardItemsApplyService = registerPromotionMembercardItemsApplyService;
		this.registerPromotionMembercardCouponsApplyService = registerPromotionMembercardCouponsApplyService;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.memberTagsMapper = memberTagsMapper;
	}

	public void executionMarketing(long companyId, long distributorId, long userId, String mobilePlain) {
		if (!StringUtils.hasText(mobilePlain)) {
			return;
		}
		try {
			RegisterPromotions row = resolvePromotionRow(companyId, distributorId);
			if (row == null || !"true".equalsIgnoreCase(String.valueOf(row.getIsOpen()).trim())) {
				return;
			}
			Map<String, Object> promotionsValue = parsePromotionsValue(row.getPromotionsValue());
			if (promotionsValue == null || promotionsValue.isEmpty()) {
				return;
			}
			Object itemsNode = promotionsValue.get("items");
			if (itemsNode != null && isNonEmptyCollection(itemsNode)) {
				registerPromotionMembercardItemsApplyService.applyItems(companyId, userId, mobilePlain.trim(), itemsNode);
			}
			Object couponsNode = promotionsValue.get("coupons");
			if (couponsNode != null && isNonEmptyCollection(couponsNode)) {
				registerPromotionMembercardCouponsApplyService.applyCoupons(
						companyId, userId, mobilePlain.trim(), couponsNode);
			}
			Object staffCouponsNode = promotionsValue.get("staff_coupons");
			if (staffCouponsNode != null
					&& isNonEmptyCollection(staffCouponsNode)
					&& hasStaffTag(companyId, userId)) {
				registerPromotionMembercardCouponsApplyService.applyCoupons(
						companyId, userId, mobilePlain.trim(), staffCouponsNode, "员工优惠券赠送");
			}
		} catch (RuntimeException e) {
			log.debug("register promotion execution error: {}", e.getMessage(), e);
		}
	}

	private RegisterPromotions resolvePromotionRow(long companyId, long distributorId) {
		if (distributorId > 0L) {
			DistributorPromotions rel =
					distributorPromotionsMapper.selectOne(
							new LambdaQueryWrapper<DistributorPromotions>()
									.eq(DistributorPromotions::getCompanyId, companyId)
									.eq(DistributorPromotions::getDistributorId, distributorId)
									.eq(DistributorPromotions::getPromotionType, "register")
									.last("LIMIT 1"));
			if (rel != null && rel.getPromotionId() != null && rel.getPromotionId() > 0L) {
				RegisterPromotions byDistributor =
						registerPromotionsMapper.selectOne(
								new LambdaQueryWrapper<RegisterPromotions>()
										.eq(RegisterPromotions::getCompanyId, companyId)
										.eq(RegisterPromotions::getId, rel.getPromotionId())
										.last("LIMIT 1"));
				if (byDistributor != null) {
					return byDistributor;
				}
			}
		}
		return registerPromotionsMapper.selectOne(
				new LambdaQueryWrapper<RegisterPromotions>()
						.eq(RegisterPromotions::getCompanyId, companyId)
						.eq(RegisterPromotions::getRegisterType, "general")
						.orderByAsc(RegisterPromotions::getId)
						.last("LIMIT 1"));
	}

	private Map<String, Object> parsePromotionsValue(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(raw.trim());
			if (!root.isObject() || root.isEmpty()) {
				return null;
			}
			return objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException | IllegalArgumentException e) {
			return null;
		}
	}

	private boolean hasStaffTag(long companyId, long userId) {
		List<MemberRelTags> rels =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getCompanyId, companyId)
								.eq(MemberRelTags::getUserId, userId));
		if (rels == null || rels.isEmpty()) {
			return false;
		}
		Set<Long> tagIds =
				rels.stream()
						.map(MemberRelTags::getTagId)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());
		if (tagIds.isEmpty()) {
			return false;
		}
		List<MemberTags> tags =
				memberTagsMapper.selectList(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.in(MemberTags::getTagId, tagIds)
								.eq(MemberTags::getSource, "staff"));
		return tags != null && !tags.isEmpty();
	}

	private static boolean isNonEmptyCollection(Object node) {
		return node instanceof Collection<?> c && !c.isEmpty();
	}
}
