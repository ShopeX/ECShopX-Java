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

package cn.shopex.ecshopx.orders.service.invoice;

import cn.shopex.ecshopx.companys.service.setting.InvoiceSettingRedisService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderInvoiceSettingService {

	private static final Logger log = LoggerFactory.getLogger(OrderInvoiceSettingService.class);

	private static final String ERR_PREFIX = "设置开票配置出错:";

	private static final String[] RULE_KEY_ORDER = {
		"invoice_status",
		"invoice_limit",
		"invoice_method",
		"freight_invoice",
		"freight_name",
		"apply_type",
		"invoice_open_term",
		"special_invoice",
		"apply_node",
		"invoice_seller_type",
		"freight_tax_rate"
	};

	private final InvoiceSettingRedisService invoiceSettingRedisService;
	private final ObjectMapper objectMapper;

	public OrderInvoiceSettingService(
			InvoiceSettingRedisService invoiceSettingRedisService, ObjectMapper objectMapper) {
		this.invoiceSettingRedisService = invoiceSettingRedisService;
		this.objectMapper = objectMapper;
	}

	public void setInvoiceSetting(long companyId, Map<String, Object> data) {
		if (data == null) {
			data = Map.of();
		}

		for (String key : RULE_KEY_ORDER) {
			validateField(key, data);
		}

		LinkedHashMap<String, Object> dataInvoiceSetting = new LinkedHashMap<>();
		for (String key : RULE_KEY_ORDER) {
			if (data.containsKey(key) && data.get(key) != null) {
				dataInvoiceSetting.put(key, data.get(key));
			}
		}

		persistInvoiceSetting(companyId, dataInvoiceSetting);
	}

	public Object getInvoiceSetting(long companyId) {
		return invoiceSettingRedisService.getInvoiceSetting(companyId);
	}

	private void persistInvoiceSetting(long companyId, LinkedHashMap<String, Object> dataInvoiceSetting) {
		String jsonForLog;
		try {
			jsonForLog = objectMapper.writeValueAsString(dataInvoiceSetting);
		} catch (JsonProcessingException e) {
			throw new ResourceException("companys 发票选项设置 JSON 序列化失败");
		}
		log.info("setInvoiceSetting:dataInvoiceSetting:{}", jsonForLog);

		invoiceSettingRedisService.setInvoiceSetting(companyId, dataInvoiceSetting);
	}

	private void validateField(String key, Map<String, Object> data) {
		switch (key) {
			case "invoice_status" -> {
				requirePresent(data, key, "是否启用开票功能必填");
				Object v = data.get(key);
				String s = String.valueOf(v).trim();
				if (!"1".equals(s) && !"0".equals(s)) {
					throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "是否启用开票功能必填");
				}
			}
			case "invoice_limit" -> {
				requirePresent(data, key, "开票维度必填");
				String s = String.valueOf(data.get(key)).trim().toLowerCase();
				if (!"item".equals(s) && !"order".equals(s)) {
					throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "开票维度必填");
				}
			}
			case "invoice_method" -> {
				requirePresent(data, key, "开票渠道必填");
				String s = String.valueOf(data.get(key)).trim();
				if (!"offline".equals(s) && !"online".equals(s)) {
					throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "开票渠道必填");
				}
			}
			case "freight_invoice" -> {
				requirePresent(data, key, "运费是否开票必填");
				String s = String.valueOf(data.get(key)).trim();
				if (!"1".equals(s) && !"2".equals(s)) {
					throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "运费是否开票必填");
				}
			}
			case "freight_name" -> {
				if (isFreightTwo(data.get("freight_invoice"))) {
					if (!data.containsKey(key) || data.get(key) == null) {
						throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "运费开票名称必填");
					}
					String s = String.valueOf(data.get(key)).trim();
					if (s.isEmpty()) {
						throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "运费开票名称必填");
					}
				}
			}
			case "apply_type" -> validateOneOrTwo(data, key, "开票申请方式必填");
			case "invoice_open_term" -> {
				requirePresent(data, key, "可开票期限应为正整数");
				long term = parseNonNegativeIntegerLike(data.get(key));
				data.put(key, term);
			}
			case "special_invoice" -> validateOneOrTwo(data, key, "专用发票展示必填");
			case "apply_node" -> validateOneOrTwo(data, key, "申请开票节点必填");
			case "invoice_seller_type" -> validateOneOrTwo(data, key, "开票方类型必填");
			case "freight_tax_rate" -> {
				requirePresent(data, key, "运费税率最小为0，最大为100的整数");
				Object v = data.get(key);
				BigDecimal bd;
				try {
					if (v instanceof BigDecimal b) {
						bd = b;
					} else {
						bd = new BigDecimal(String.valueOf(v).trim());
					}
				} catch (NumberFormatException e) {
					throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "运费税率最小为0，最大为100的整数");
				}
				if (bd.compareTo(BigDecimal.ZERO) < 0 || bd.compareTo(new BigDecimal("100")) > 0) {
					throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "运费税率最小为0，最大为100的整数");
				}
			}
			default -> {}
		}
	}

	private static void requirePresent(Map<String, Object> data, String key, String msg) {
		if (!data.containsKey(key) || data.get(key) == null) {
			throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + msg);
		}
	}

	private static void validateOneOrTwo(Map<String, Object> data, String key, String msg) {
		requirePresent(data, key, msg);
		String s = String.valueOf(data.get(key)).trim();
		if (!"1".equals(s) && !"2".equals(s)) {
			throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + msg);
		}
	}

	private static boolean isFreightTwo(Object v) {
		return "2".equals(String.valueOf(v).trim());
	}

	private static long parseNonNegativeIntegerLike(Object v) {
		if (v instanceof Number n) {
			double d = n.doubleValue();
			if (d != Math.floor(d)) {
				throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "可开票期限应为正整数");
			}
			if (d > Long.MAX_VALUE || d < Long.MIN_VALUE) {
				throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "可开票期限应为正整数");
			}
			long lv = n.longValue();
			if (lv < 0) {
				throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "可开票期限应为正整数");
			}
			return lv;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "可开票期限应为正整数");
		}
		try {
			long lv = new BigDecimal(s).longValueExact();
			if (lv < 0) {
				throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "可开票期限应为正整数");
			}
			return lv;
		} catch (NumberFormatException | ArithmeticException e) {
			throw new cn.shopex.ecshopx.common.exception.BadRequestException(ERR_PREFIX + "可开票期限应为正整数");
		}
	}
}
