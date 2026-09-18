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

import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeOrderReceiveMembercardPromotionService;
import cn.shopex.ecshopx.promotions.domain.RegisterPromotions;
import cn.shopex.ecshopx.promotions.mapper.RegisterPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegisterPromotionsMembercardPromotionService {

	private static final Logger log = LoggerFactory.getLogger(RegisterPromotionsMembercardPromotionService.class);

	private final RegisterPromotionsMapper registerPromotionsMapper;
	private final ObjectMapper objectMapper;
	private final RegisterPromotionMembercardItemsApplyService registerPromotionMembercardItemsApplyService;
	private final RegisterPromotionMembercardCouponsApplyService registerPromotionMembercardCouponsApplyService;
	private final VipGradeOrderReceiveMembercardPromotionService vipGradeOrderReceiveMembercardPromotionService;

	public RegisterPromotionsMembercardPromotionService(
			RegisterPromotionsMapper registerPromotionsMapper,
			ObjectMapper objectMapper,
			RegisterPromotionMembercardItemsApplyService registerPromotionMembercardItemsApplyService,
			RegisterPromotionMembercardCouponsApplyService registerPromotionMembercardCouponsApplyService,
			VipGradeOrderReceiveMembercardPromotionService vipGradeOrderReceiveMembercardPromotionService) {
		this.registerPromotionsMapper = registerPromotionsMapper;
		this.objectMapper = objectMapper;
		this.registerPromotionMembercardItemsApplyService = registerPromotionMembercardItemsApplyService;
		this.registerPromotionMembercardCouponsApplyService = registerPromotionMembercardCouponsApplyService;
		this.vipGradeOrderReceiveMembercardPromotionService = vipGradeOrderReceiveMembercardPromotionService;
	}

	/**
	 * Member-create success path (Bus listener): same effects as {@link #getMembercardPromotions} inner call to
	 * {@code actionPromotionByCompanyId}, without HTTP response shaping.
	 */
	public void executeMembercardRegisterPromotionAfterMemberCreate(
			long companyId, long userId, String mobilePlain) {
		try {
			actionPromotionByCompanyId(companyId, userId, mobilePlain);
		} catch (Exception e) {
			log.debug("新客营销错误: {}", e.getMessage(), e);
		}
	}

	public Map<String, Object> getMembercardPromotions(long companyId, long userId, String mobilePlain) {
		Object result = null;
		try {
			result = actionPromotionByCompanyId(companyId, userId, mobilePlain);
		} catch (Exception e) {
			log.debug("新客营销错误: {}", e.getMessage(), e);
			result = null;
		}
		Map<String, Object> body = new LinkedHashMap<>(1);
		if (result instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> orderMap = (Map<String, Object>) m;
			body.put("status", orderMap);
		} else {
			body.put("status", Boolean.FALSE);
		}
		return body;
	}

	private Object actionPromotionByCompanyId(long companyId, long userId, String mobilePlain) {
		RegisterPromotions row = registerPromotionsMapper.selectOne(new LambdaQueryWrapper<RegisterPromotions>()
				.eq(RegisterPromotions::getCompanyId, companyId)
				.eq(RegisterPromotions::getRegisterType, "membercard")
				.orderByAsc(RegisterPromotions::getId)
				.last("LIMIT 1"));
		if (row == null) {
			return null;
		}
		if (!"true".equals(row.getIsOpen())) {
			return null;
		}
		String raw = row.getPromotionsValue();
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return null;
		}
		Map<String, Object> pv;
		try {
			JsonNode root = objectMapper.readTree(raw.trim());
			if (!root.isObject() || root.size() == 0) {
				return null;
			}
			pv = objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException | IllegalArgumentException e) {
			return null;
		}
		if (pv == null || pv.isEmpty()) {
			return null;
		}

		Object itemsNode = pv.get("items");
		if (itemsNode != null && isNonEmptyCollection(itemsNode)) {
			registerPromotionMembercardItemsApplyService.applyItems(companyId, userId, mobilePlain, itemsNode);
		}
		Object couponsNode = pv.get("coupons");
		if (couponsNode != null && isNonEmptyCollection(couponsNode)) {
			registerPromotionMembercardCouponsApplyService.applyCoupons(companyId, userId, mobilePlain, couponsNode);
		}

		Object mc = pv.get("membercard");
		if (mc instanceof Map<?, ?> mcMap && !mcMap.isEmpty()) {
			@SuppressWarnings("unchecked")
			Map<String, Object> membercard = (Map<String, Object>) mc;
			long vipGradeId = parsePositiveLong(membercard.get("vip_grade_id"));
			if (vipGradeId > 0L) {
				return vipGradeOrderReceiveMembercardPromotionService.receiveMemberCardForPromotionReceive(
						companyId, userId, mobilePlain, membercard);
			}
		}
		return null;
	}

	private static boolean isNonEmptyCollection(Object node) {
		return node instanceof Collection<?> c && !c.isEmpty();
	}

	private static long parsePositiveLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
