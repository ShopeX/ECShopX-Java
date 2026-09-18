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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.ValuePresence;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderMemberGetPointsService;
import cn.shopex.ecshopx.orders.service.normal.create.NormalOrderMarkdownApplyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminOrderMarkDownService {

	private final OrderAssociationsMapper orderAssociationsMapper;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final NormalOrderMarkdownApplyService normalOrderMarkdownApplyService;
	private final NormalOrderMemberGetPointsService normalOrderMemberGetPointsService;
	private final AdminOrderMarkDownPersistService adminOrderMarkDownPersistService;
	private final ObjectMapper objectMapper;

	public AdminOrderMarkDownService(
			OrderAssociationsMapper orderAssociationsMapper,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			NormalOrderMarkdownApplyService normalOrderMarkdownApplyService,
			NormalOrderMemberGetPointsService normalOrderMemberGetPointsService,
			AdminOrderMarkDownPersistService adminOrderMarkDownPersistService,
			ObjectMapper objectMapper) {
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.normalOrderMarkdownApplyService = normalOrderMarkdownApplyService;
		this.normalOrderMemberGetPointsService = normalOrderMemberGetPointsService;
		this.adminOrderMarkDownPersistService = adminOrderMarkDownPersistService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> markDown(long companyId, Map<String, Object> mergedParams) {
		return previewOnly(companyId, mergedParams);
	}

	public Map<String, Object> confirmMarkDown(
			long companyId,
			long operatorId,
			String operatorType,
			Map<String, Object> mergedParams) {
		validateMarkdownParams(mergedParams);

		String orderIdRaw = String.valueOf(mergedParams.get("order_id")).trim();
		if (!StringUtils.hasText(orderIdRaw)) {
			throw new BadRequestException("订单号必填");
		}

		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdRaw);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}

		OrderAssociations assoc = loadAssociationOrThrow(companyId, orderIdNum);
		assertMarkdownOrderState(assoc);

		Map<String, Object> bundle =
				adminNormalOrderDetailService.buildOrderBundle(companyId, orderIdRaw, false);
		Object rawOrderInfo = bundle.get("orderInfo");
		if (!(rawOrderInfo instanceof Map<?, ?>)) {
			throw new ResourceException("订单号为" + orderIdRaw + "的订单不存在");
		}

		Object detachedOld = objectMapper.convertValue(rawOrderInfo, Object.class);
		Map<String, Object> oldOrderInfo =
				objectMapper.convertValue(
						detachedOld, new TypeReference<LinkedHashMap<String, Object>>() {});

		Object detached = objectMapper.convertValue(rawOrderInfo, Object.class);
		Map<String, Object> orderInfo =
				objectMapper.convertValue(
						detached, new TypeReference<LinkedHashMap<String, Object>>() {});

		Map<String, Object> markdown = buildMarkdownMap(mergedParams);
		normalOrderMarkdownApplyService.applyMarkdownPreview(orderInfo, markdown);

		long tf = longVal(orderInfo.get("total_fee"), 0L);
		if (tf <= 0L) {
			throw new ResourceException("订单支付金额必须大于0");
		}

		long uid = longVal(orderInfo.get("user_id"), 0L);
		if (uid > 0L) {
			normalOrderMemberGetPointsService.applyMemberGetPoints(companyId, orderInfo);
		}

		LinkedHashMap<String, Object> logParams = new LinkedHashMap<>(mergedParams);
		logParams.put("operator_id", operatorId);
		logParams.put("operator_type", operatorType == null ? "" : operatorType);

		adminOrderMarkDownPersistService.persistAfterMarkdown(
				companyId, oldOrderInfo, orderInfo, logParams, operatorId, operatorType);
		return orderInfo;
	}

	private Map<String, Object> previewOnly(long companyId, Map<String, Object> mergedParams) {
		validateMarkdownParams(mergedParams);

		String orderIdRaw = String.valueOf(mergedParams.get("order_id")).trim();
		if (!StringUtils.hasText(orderIdRaw)) {
			throw new BadRequestException("订单号必填");
		}

		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdRaw);
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}

		OrderAssociations assoc = loadAssociationOrThrow(companyId, orderIdNum);
		assertMarkdownOrderState(assoc);

		Map<String, Object> bundle =
				adminNormalOrderDetailService.buildOrderBundle(companyId, orderIdRaw, false);
		Object rawOrderInfo = bundle.get("orderInfo");
		if (!(rawOrderInfo instanceof Map<?, ?>)) {
			throw new ResourceException("订单号为" + orderIdRaw + "的订单不存在");
		}

		Object detached = objectMapper.convertValue(rawOrderInfo, Object.class);
		Map<String, Object> orderInfo =
				objectMapper.convertValue(
						detached, new TypeReference<LinkedHashMap<String, Object>>() {});

		Map<String, Object> markdown = buildMarkdownMap(mergedParams);
		normalOrderMarkdownApplyService.applyMarkdownPreview(orderInfo, markdown);
		return orderInfo;
	}

	private OrderAssociations loadAssociationOrThrow(long companyId, long orderIdNum) {
		OrderAssociations assoc =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (assoc == null) {
			throw new ResourceException("订单不存在");
		}
		return assoc;
	}

	private void assertMarkdownOrderState(OrderAssociations assoc) {
		String status = assoc.getOrderStatus() == null ? "" : assoc.getOrderStatus().trim();
		if (!"NOTPAY".equals(status)) {
			throw new ResourceException("只有未支付的订单才能改价");
		}

		String effective = effectiveOrderType(assoc);
		if (!isSupportedForNormalPhysicalOrder(effective)) {
			throw new ResourceException("无此类型订单！");
		}
	}

	private static Map<String, Object> buildMarkdownMap(Map<String, Object> mergedParams) {
		Map<String, Object> markdown = new LinkedHashMap<>();
		markdown.put("down_type", mergedParams.get("down_type"));
		markdown.put("total_fee", mergedParams.get("total_fee"));
		markdown.put("items", mergedParams.get("items"));
		if (mergedParams.containsKey("freight_fee") && mergedParams.get("freight_fee") != null) {
			markdown.put("freight_fee", mergedParams.get("freight_fee"));
		}
		return markdown;
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static void validateMarkdownParams(Map<String, Object> mergedParams) {
		if (!mergedParams.containsKey("down_type")) {
			return;
		}
		Object downTypeObj = mergedParams.get("down_type");
		String downType = downTypeObj == null ? "" : downTypeObj.toString().trim();
		if (!"total".equals(downType) && !"items".equals(downType)) {
			throw new BadRequestException("确认整单还是按件改价");
		}

		if ("total".equals(downType)) {
			Object tfo = mergedParams.get("total_fee");
			if (tfo == null || !StringUtils.hasText(String.valueOf(tfo).trim())) {
				throw new BadRequestException("整单改价金额必填");
			}
			try {
				Long.parseLong(String.valueOf(tfo).trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("整单改价金额必填");
			}
		}

		if ("items".equals(downType)) {
			Object itemsRaw = mergedParams.get("items");
			if (!(itemsRaw instanceof List<?> itemsList) || itemsList.isEmpty()) {
				throw new BadRequestException("按件改价商品ID必填");
			}
			boolean anyRow = false;
			for (Object o : itemsList) {
				if (!(o instanceof Map<?, ?> row)) {
					continue;
				}
				anyRow = true;
				@SuppressWarnings("unchecked")
				Map<String, Object> rm = (Map<String, Object>) row;
				if (!rm.containsKey("item_id")
						|| rm.get("item_id") == null
						|| !StringUtils.hasText(String.valueOf(rm.get("item_id")).trim())) {
					throw new BadRequestException("按件改价商品ID必填");
				}
				boolean hasTotalFee = ValuePresence.hasEffectiveValue(rm.get("total_fee"));
				boolean hasDiscount = ValuePresence.hasEffectiveValue(rm.get("discount"));
				if (!hasTotalFee && !hasDiscount) {
					throw new BadRequestException("按件改价商品总价和折扣必须设置一个");
				}
			}
			if (!anyRow) {
				throw new BadRequestException("按件改价商品ID必填");
			}
		}
	}

	private static String effectiveOrderType(OrderAssociations assoc) {
		String ot = assoc.getOrderType() == null ? "" : assoc.getOrderType().toLowerCase(Locale.ROOT);
		String oc = assoc.getOrderClass() == null ? "" : assoc.getOrderClass().toLowerCase(Locale.ROOT);
		if (("normal".equals(ot) || "service".equals(ot))
				&& !ot.equals(oc)
				&& !"normal".equals(oc)
				&& !"service".equals(oc)) {
			return ot + "_" + oc;
		}
		return ot;
	}

	private static boolean isSupportedForNormalPhysicalOrder(String effective) {
		if (effective.isEmpty()) {
			return false;
		}
		if ("membercard".equals(effective) || "supplier_order".equals(effective)) {
			return false;
		}
		if ("normal".equals(effective)
				|| "normal_shopadmin".equals(effective)
				|| "service".equals(effective)
				|| effective.startsWith("service_")
				|| "bargain".equals(effective)
				|| "normal_bargain".equals(effective)) {
			return true;
		}
		return effective.startsWith("normal_");
	}
}
