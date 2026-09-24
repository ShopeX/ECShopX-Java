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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.distribution.DistributorWhiteListCheckUserValidPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.GoodsRecommendCheckoutMergePort;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WxappOrderFreightFeeService {

	private static final Logger log = LoggerFactory.getLogger(WxappOrderFreightFeeService.class);

	private final WxappOrderTypeRegistry wxappOrderTypeRegistry;

	private final OrderCheckoutInvoiceStatusService orderCheckoutInvoiceStatusService;

	private final DistributorWhiteListCheckUserValidPort distributorWhiteListCheckUserValidPort;

	private final GoodsRecommendCheckoutMergePort goodsRecommendCheckoutMergePort;

	public WxappOrderFreightFeeService(
			WxappOrderTypeRegistry wxappOrderTypeRegistry,
			OrderCheckoutInvoiceStatusService orderCheckoutInvoiceStatusService,
			DistributorWhiteListCheckUserValidPort distributorWhiteListCheckUserValidPort,
			GoodsRecommendCheckoutMergePort goodsRecommendCheckoutMergePort) {
		this.wxappOrderTypeRegistry = wxappOrderTypeRegistry;
		this.orderCheckoutInvoiceStatusService = orderCheckoutInvoiceStatusService;
		this.distributorWhiteListCheckUserValidPort = distributorWhiteListCheckUserValidPort;
		this.goodsRecommendCheckoutMergePort = goodsRecommendCheckoutMergePort;
	}

	public Map<String, Object> getOrderFreightFeeInfo(
			HttpServletRequest request,
			Map<String, Object> mergedParams,
			Map<String, Object> h5AuthClaims) {
		Map<String, Object> params = WxappOrderParamMergeSupport.applyDefaultsAndAuthForFreightFee(mergedParams, h5AuthClaims);

		if (mergedParams.containsKey("promoter_user_id")) {
			long promoter = longVal(mergedParams.get("promoter_user_id"));
			if (promoter != 0L) {
				long authUid = longVal(h5AuthClaims != null ? h5AuthClaims.get("user_id") : null);
				if (authUid == promoter) {
					params.put("user_id", longVal(params.get("buy_user_id")));
					params.put("order_source", "salesperson");
					Object rm = params.get("receiver_mobile");
					params.put("mobile", rm == null ? 0L : longVal(rm));
					params.put("salesman_id", longVal(params.get("promoter_user_id")));
				}
			}
		}

		Object distributorIdRaw = mergedParams.get("distributor_id");
		if (distributorIdRaw != null) {
			String s = distributorIdRaw.toString().trim();
			if (!s.isEmpty() && !"0".equals(s) && longVal(distributorIdRaw) != 0L) {
				long companyId = longVal(params.get("company_id"));
				long userId = longVal(params.get("user_id"));
				if (!distributorWhiteListCheckUserValidPort.checkUserValidCommon(
						longVal(distributorIdRaw), userId, companyId)) {
					throw new ResourceException("非本店铺会员无法下单");
				}
			}
		}

		long companyId = longVal(params.get("company_id"));
		goodsRecommendCheckoutMergePort.apply(companyId, params);
		long userId = longVal(params.get("user_id"));
		WxappOrderCreateContext ctx = new WxappOrderCreateContext();
		ctx.setRequest(request);
		ctx.setCompanyId(companyId);
		ctx.setUserId(userId);
		ctx.setSessionAuth(h5AuthClaims);
		ctx.getParams().putAll(params);

		Map<String, Object> result = wxappOrderTypeRegistry.getOrderTempInfo(ctx);
		int invoiceStatus = orderCheckoutInvoiceStatusService.checkoutInvoiceStatus(result);
		result.put("invoice_status", invoiceStatus);
		log.info(
				":invoice_status:WxappOrderFreightFeeService.getOrderFreightFeeInfo::invoice_status:{}",
				Integer.toString(invoiceStatus));

		splitItemsByLogistics(result);
		return result;
	}

	private static void splitItemsByLogistics(Map<String, Object> result) {
		Object rawItems = result.get("items");
		List<?> list;
		if (rawItems instanceof List<?>) {
			list = (List<?>) rawItems;
		} else {
			list = List.of();
		}
		List<Map<String, Object>> nonLogistics = new ArrayList<>();
		List<Map<String, Object>> logistics = new ArrayList<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> rawMap)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> item = (Map<String, Object>) rawMap;
			boolean isLogistics = normalizeIsLogistics(item.get("is_logistics"));
			item.put("is_logistics", isLogistics);
			if (isLogistics) {
				logistics.add(item);
			} else {
				nonLogistics.add(item);
			}
		}
		result.put("items", sinkRecommendToBottom(nonLogistics));
		result.put("logistics_items", sinkRecommendToBottom(logistics));
	}

	private static List<Map<String, Object>> sinkRecommendToBottom(List<Map<String, Object>> lines) {
		if (lines == null || lines.size() < 2) {
			return lines;
		}
		List<Map<String, Object>> original = new ArrayList<>();
		List<Map<String, Object>> recommend = new ArrayList<>();
		for (Map<String, Object> line : lines) {
			if (Boolean.TRUE.equals(line.get("is_recommend"))) {
				recommend.add(line);
			} else {
				original.add(line);
			}
		}
		if (recommend.isEmpty()) {
			return lines;
		}
		original.addAll(recommend);
		return original;
	}

	private static boolean normalizeIsLogistics(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof String s && "true".equalsIgnoreCase(s.trim())) {
			return true;
		}
		return false;
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
}
