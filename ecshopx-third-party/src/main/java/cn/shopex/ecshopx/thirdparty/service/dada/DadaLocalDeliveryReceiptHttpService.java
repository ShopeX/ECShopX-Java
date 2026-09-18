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

package cn.shopex.ecshopx.thirdparty.service.dada;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryReceiptPort;
import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaCityResolvePort;
import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaCompanyBindingPort;
import cn.shopex.ecshopx.common.port.localdelivery.LocalDeliveryDadaShopNoPort;
import cn.shopex.ecshopx.thirdparty.config.DadaOpenPlatformProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("dadaLocalDeliveryReceiptHttp")
@ConditionalOnProperty(prefix = "ecshopx.thirdparty.dada", name = "http-enabled", havingValue = "true")
public class DadaLocalDeliveryReceiptHttpService implements DadaLocalDeliveryReceiptPort {

	private final DadaOpenPlatformProperties properties;
	private final LocalDeliveryDadaShopNoPort localDeliveryDadaShopNoPort;
	private final LocalDeliveryDadaCityResolvePort localDeliveryDadaCityResolvePort;
	private final LocalDeliveryDadaCompanyBindingPort localDeliveryDadaCompanyBindingPort;
	private final DadaOpenPlatformJsonApiClient dadaOpenPlatformJsonApiClient;
	private final ObjectMapper objectMapper;
	private final String callbackBaseUrl;

	public DadaLocalDeliveryReceiptHttpService(
			DadaOpenPlatformProperties properties,
			LocalDeliveryDadaShopNoPort localDeliveryDadaShopNoPort,
			LocalDeliveryDadaCityResolvePort localDeliveryDadaCityResolvePort,
			LocalDeliveryDadaCompanyBindingPort localDeliveryDadaCompanyBindingPort,
			DadaOpenPlatformJsonApiClient dadaOpenPlatformJsonApiClient,
			ObjectMapper objectMapper,
			@Value("${ecshopx.thirdparty.dada.callback-base-url:}") String callbackBaseUrl) {
		this.properties = properties;
		this.localDeliveryDadaShopNoPort = localDeliveryDadaShopNoPort;
		this.localDeliveryDadaCityResolvePort = localDeliveryDadaCityResolvePort;
		this.localDeliveryDadaCompanyBindingPort = localDeliveryDadaCompanyBindingPort;
		this.dadaOpenPlatformJsonApiClient = dadaOpenPlatformJsonApiClient;
		this.objectMapper = objectMapper;
		this.callbackBaseUrl = callbackBaseUrl == null ? "" : callbackBaseUrl.trim();
	}

	@Override
	public void queryDeliverFeeAndApplyToOrderData(long companyId, boolean buyerPaysFreight, Map<String, Object> orderInfo) {
		String sourceId = resolveSourceIdForRequest(companyId);
		String structJson;
		try {
			Map<String, Object> orderStruct = buildOrderStruct(companyId, orderInfo);
			structJson = objectMapper.writeValueAsString(orderStruct);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("达达询价参数构造失败: " + e.getMessage());
		}
		JsonNode root;
		try {
			root =
					dadaOpenPlatformJsonApiClient.postJsonBody(
							properties, sourceId, "/api/order/queryDeliverFee", structJson);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("达达询价请求异常: " + e.getMessage());
		}
		JsonNode statusNode = root.get("status");
		String status = statusNode == null || !statusNode.isTextual() ? "" : statusNode.asText("");
		if (!"success".equals(status)) {
			JsonNode msgNode = root.get("msg");
			String msg =
					msgNode == null || !msgNode.isTextual() || msgNode.asText("").isBlank()
							? "达达询价失败"
							: msgNode.asText("");
			throw new ResourceException(msg);
		}
		JsonNode result = root.get("result");
		if (result == null || result.isNull()) {
			throw new ResourceException("达达询价无结果");
		}
		JsonNode deliveryNoNode = result.get("deliveryNo");
		String deliveryNo = deliveryNoNode == null ? "" : deliveryNoNode.asText("").trim();
		if (!StringUtils.hasText(deliveryNo)) {
			throw new ResourceException("达达询价未返回运单号");
		}
		orderInfo.put("dada_delivery_no", deliveryNo);
		if (buyerPaysFreight) {
			JsonNode feeNode = result.get("fee");
			String feeStr = feeNode == null ? "0" : feeNode.asText("0").trim();
			BigDecimal feeYuan;
			try {
				feeYuan = new BigDecimal(feeStr);
			} catch (NumberFormatException e) {
				throw new ResourceException("达达询价运费格式错误");
			}
			long freightFen = feeYuan.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValue();
			orderInfo.put("freight_fee", (int) Math.min(Integer.MAX_VALUE, freightFen));
			Object totalObj = orderInfo.get("total_fee");
			long totalFen = toFenLong(totalObj);
			if (totalFen > 0) {
				orderInfo.put("total_fee", (int) Math.min(Integer.MAX_VALUE, totalFen + freightFen));
			} else {
				orderInfo.put("total_fee", 0);
			}
		}
	}

	@Override
	public void addAfterQuery(long companyId, String deliveryNo) {
		if (!StringUtils.hasText(deliveryNo)) {
			throw new ResourceException("达达发单缺少运单号");
		}
		String sourceId = resolveSourceIdForRequest(companyId);
		String inner;
		try {
			inner = objectMapper.writeValueAsString(Map.of("deliveryNo", deliveryNo.trim()));
		} catch (Exception e) {
			throw new ResourceException("达达发单参数构造失败");
		}
		JsonNode root;
		try {
			root =
					dadaOpenPlatformJsonApiClient.postJsonBody(
							properties, sourceId, "/api/order/addAfterQuery", inner);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("达达发单请求异常: " + e.getMessage());
		}
		JsonNode statusNode = root.get("status");
		String status = statusNode == null || !statusNode.isTextual() ? "" : statusNode.asText("");
		if (!"success".equals(status)) {
			JsonNode msgNode = root.get("msg");
			String msg =
					msgNode == null || !msgNode.isTextual() || msgNode.asText("").isBlank()
							? "达达发单失败"
							: msgNode.asText("");
			throw new ResourceException(msg);
		}
	}

	private String resolveSourceIdForRequest(long companyId) {
		if (!properties.isOnline()) {
			return properties.getSandboxSourceId() == null ? "" : properties.getSandboxSourceId().trim();
		}
		String sid = localDeliveryDadaCompanyBindingPort.sourceIdForCompany(companyId);
		if (!StringUtils.hasText(sid)) {
			throw new ResourceException("达达商户 source_id 未配置");
		}
		return sid.trim();
	}

	private Map<String, Object> buildOrderStruct(long companyId, Map<String, Object> orderInfo) {
		long distributorId = toLong(orderInfo.get("distributor_id"));
		String shopNo = localDeliveryDadaShopNoPort.resolveShopNo(companyId, distributorId);
		String receiverCity = Objects.toString(orderInfo.get("receiver_city"), "");
		String cityCode = localDeliveryDadaCityResolvePort.resolveCityCode(companyId, receiverCity);
		String originId = Objects.toString(orderInfo.get("order_id"), "").trim();
		if (!StringUtils.hasText(originId)) {
			throw new ResourceException("订单号缺失");
		}
		long totalFen = toFenLong(orderInfo.get("total_fee"));
		BigDecimal cargoPrice =
				BigDecimal.valueOf(totalFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		String receiverState = Objects.toString(orderInfo.get("receiver_state"), "");
		String receiverDistrict = Objects.toString(orderInfo.get("receiver_district"), "");
		String receiverAddress = Objects.toString(orderInfo.get("receiver_address"), "");
		String fullAddress = receiverState + receiverCity + receiverDistrict + receiverAddress;
		String callback = buildCallbackUrl(companyId);
		double cargoWeight = sumCargoWeight(orderInfo.get("items"));
		List<Map<String, Object>> productList = buildProductList(orderInfo.get("items"));
		String remark = Objects.toString(orderInfo.get("remark"), "");
		Map<String, Object> struct = new LinkedHashMap<>();
		struct.put("shop_no", shopNo);
		struct.put("origin_id", originId);
		struct.put("city_code", cityCode);
		struct.put("cargo_price", cargoPrice.toPlainString());
		struct.put("is_prepay", 0);
		struct.put("receiver_name", Objects.toString(orderInfo.get("receiver_name"), ""));
		struct.put("receiver_address", fullAddress);
		struct.put("callback", callback);
		struct.put("cargo_weight", cargoWeight);
		struct.put("receiver_phone", Objects.toString(orderInfo.get("receiver_mobile"), ""));
		struct.put("info", remark);
		struct.put("product_list", productList);
		return struct;
	}

	private String buildCallbackUrl(long companyId) {
		String base = callbackBaseUrl;
		while (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		if (!StringUtils.hasText(base)) {
			return "openapi/dada/callback/" + companyId;
		}
		return base + "/openapi/dada/callback/" + companyId;
	}

	@SuppressWarnings("unchecked")
	private static double sumCargoWeight(Object itemsObj) {
		if (!(itemsObj instanceof List<?> raw)) {
			return 0.0;
		}
		double sum = 0.0;
		for (Object o : raw) {
			if (!(o instanceof Map)) {
				continue;
			}
			Object w = ((Map<?, ?>) o).get("weight");
			if (w instanceof Number n) {
				sum += n.doubleValue();
			} else if (w != null) {
				try {
					sum += Double.parseDouble(w.toString().trim());
				} catch (NumberFormatException ignored) {
					// skip
				}
			}
		}
		return sum;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> buildProductList(Object itemsObj) {
		List<Map<String, Object>> productList = new ArrayList<>();
		if (!(itemsObj instanceof List<?> raw)) {
			return productList;
		}
		for (Object o : raw) {
			if (!(o instanceof Map<?, ?> im)) {
				continue;
			}
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("sku_name", Objects.toString(im.get("item_name"), ""));
			row.put("src_product_no", Objects.toString(im.get("item_bn"), ""));
			row.put("count", toInt(im.get("num")));
			row.put("unit", Objects.toString(im.get("item_unit"), ""));
			productList.add(row);
		}
		return productList;
	}

	private static long toFenLong(Object v) {
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

	private static long toLong(Object v) {
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

	private static int toInt(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

}
