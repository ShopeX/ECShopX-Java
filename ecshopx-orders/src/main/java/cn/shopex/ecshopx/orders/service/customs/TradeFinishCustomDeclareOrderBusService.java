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

package cn.shopex.ecshopx.orders.service.customs;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.CustomDeclareOrderResult;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.CustomDeclareOrderResultMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.payment.OrdersExternalPayParamBuildService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Trade-finish handler for cross-border customs submission over wxpay (listener:
 * {@code listener:orders.listeners.TradeFinishCustomDeclareOrder}).
 */
@Service
@Slf4j
public class TradeFinishCustomDeclareOrderBusService {

	private static final String WX_CUSTOM_DECLARE_URL =
			"https://api.mch.weixin.qq.com/cgi-bin/mch/customs/customdeclareorder";

	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final CustomDeclareOrderResultMapper customDeclareOrderResultMapper;
	private final OrdersExternalPayParamBuildService ordersExternalPayParamBuildService;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public TradeFinishCustomDeclareOrderBusService(
			NormalOrdersMapper normalOrdersMapper,
			TradeMapper tradeMapper,
			CustomDeclareOrderResultMapper customDeclareOrderResultMapper,
			OrdersExternalPayParamBuildService ordersExternalPayParamBuildService,
			ObjectMapper objectMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.tradeMapper = tradeMapper;
		this.customDeclareOrderResultMapper = customDeclareOrderResultMapper;
		this.ordersExternalPayParamBuildService = ordersExternalPayParamBuildService;
		this.objectMapper = objectMapper;
		this.restTemplate = new RestTemplate();
	}

	public void handleTradeFinishRow(Map<String, Object> tradeRowSnakeCase) {
		if (tradeRowSnakeCase == null || tradeRowSnakeCase.isEmpty()) {
			return;
		}
		String payTypeRaw = normalizeLower(tradeRowSnakeCase.get("pay_type"));
		if ("point".equals(payTypeRaw) || "deposit".equals(payTypeRaw)) {
			log.debug("trade finish custom declare skipped: pay_type point/deposit");
			return;
		}
		String tradeSourceType = normalizeLower(tradeRowSnakeCase.get("trade_source_type"));
		if ("membercard".equals(tradeSourceType)) {
			log.debug("trade finish custom declare skipped: trade_source_type membercard");
			return;
		}
		if (!"wxpay".equals(payTypeRaw)) {
			log.debug("trade finish custom declare skipped: pay_type not wxpay");
			return;
		}
		Long companyId = longFrom(tradeRowSnakeCase.get("company_id"));
		Long orderIdNum = longFrom(tradeRowSnakeCase.get("order_id"));
		if (companyId == null || companyId <= 0L || orderIdNum == null || orderIdNum <= 0L) {
			log.debug("trade finish custom declare skipped: missing company_id or order_id");
			return;
		}
		String tradeId = text(tradeRowSnakeCase.get("trade_id"));
		if (!StringUtils.hasText(tradeId)) {
			log.debug("trade finish custom declare skipped: missing trade_id");
			return;
		}
		Trade trade = tradeMapper.selectById(tradeId);
		if (trade == null) {
			log.debug("trade finish custom declare skipped: trade not found tradeId={}", tradeId);
			return;
		}
		if (!StringUtils.hasText(trade.getTransactionId())) {
			log.debug("trade finish custom declare skipped: missing transaction_id tradeId={}", tradeId);
			return;
		}
		long distributorId = parseDistributorId(trade.getDistributorId());
		NormalOrders order = normalOrdersMapper.selectById(orderIdNum);
		if (order == null) {
			log.debug("trade finish custom declare skipped: order not found orderId={}", orderIdNum);
			return;
		}
		if (order.getCompanyId() != null && !order.getCompanyId().equals(companyId)) {
			log.debug("trade finish custom declare skipped: company mismatch orderId={}", orderIdNum);
			return;
		}
		Integer type = order.getType();
		if (type == null || type == 0) {
			log.debug("trade finish custom declare skipped: normal (non-cross-border) order orderId={}", orderIdNum);
			return;
		}

		CustomsExtras extras = resolveCustomsExtras(order);
		if (extras == null || !StringUtils.hasText(extras.customsCode()) || !StringUtils.hasText(extras.mchCustomsNo())) {
			log.debug(
					"trade finish custom declare skipped: customs or mch_customs_no missing in order context orderId={}",
					orderIdNum);
			return;
		}
		String certId = text(order.getIdentityId());
		String buyerName = text(order.getIdentityName());
		if (!StringUtils.hasText(certId) || !StringUtils.hasText(buyerName)) {
			log.debug("trade finish custom declare skipped: identity fields empty orderId={}", orderIdNum);
			return;
		}

		int orderFeeFen = fenFromNumber(trade.getPayFee(), () -> fenFromFeeString(order.getTotalFee()));
		int transportFen = order.getFreightFee() == null ? 0 : Math.max(0, order.getFreightFee());
		int productFen = fenFromFeeString(order.getItemFee());
		if (productFen <= 0) {
			productFen = orderFeeFen;
		}
		int dutyFen = order.getTotalTax() == null ? 0 : Math.max(0, order.getTotalTax());

		String xml;
		try {
			xml =
					ordersExternalPayParamBuildService.buildWxpayCustomDeclareOrderXml(
							companyId,
							distributorId,
							trade,
							extras.customsCode(),
							extras.mchCustomsNo(),
							"IDCARD",
							certId,
							buyerName,
							dutyFen,
							transportFen,
							productFen,
							orderFeeFen,
							String.valueOf(order.getOrderId()),
							"ADD");
		} catch (ResourceException e) {
			log.debug(
					"trade finish custom declare skipped: wx config / xml build orderId={} msg={}",
					orderIdNum,
					e.getMessage());
			return;
		} catch (Exception e) {
			log.debug("trade finish custom declare skipped: xml build error orderId={} err={}", orderIdNum, e.toString());
			return;
		}

		String respXml;
		try {
			ResponseEntity<String> resp =
					restTemplate.postForEntity(WX_CUSTOM_DECLARE_URL, wxEntity(xml), String.class);
			respXml = resp.getBody();
		} catch (Exception e) {
			log.debug("trade finish custom declare: http error orderId={} err={}", orderIdNum, e.toString());
			return;
		}
		if (!StringUtils.hasText(respXml)) {
			log.debug("trade finish custom declare: empty response orderId={}", orderIdNum);
			return;
		}
		Map<String, String> r = parseWechatXmlToMap(respXml);
		if (!"SUCCESS".equals(r.get("return_code"))) {
			log.debug(
					"trade finish custom declare: return not success orderId={} msg={}",
					orderIdNum,
					r.get("return_msg"));
			return;
		}
		if (!"SUCCESS".equals(r.get("result_code"))) {
			log.debug(
					"trade finish custom declare: result not success orderId={} err={} des={}",
					orderIdNum,
					r.get("err_code"),
					r.get("err_code_des"));
			return;
		}

		String state = text(r.get("state"));
		if (!StringUtils.hasText(state)) {
			log.debug("trade finish custom declare: missing state in response orderId={}", orderIdNum);
			return;
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		CustomDeclareOrderResult row = new CustomDeclareOrderResult();
		row.setOrderId(orderIdNum);
		row.setTradeId(tradeId);
		row.setCompanyId(companyId);
		row.setTransactionId(text(r.get("transaction_id")));
		if (!StringUtils.hasText(row.getTransactionId())) {
			row.setTransactionId(trade.getTransactionId());
		}
		row.setState(state);
		row.setSubOrderNo(text(r.get("sub_order_no")));
		row.setSubOrderId(text(r.get("sub_order_id")));
		row.setModifyTime(text(r.get("modify_time")));
		row.setCertCheckResult(text(r.get("cert_check_result")));
		row.setVerifyDepartment(text(r.get("verify_department")));
		row.setVerifyDepartmentTradeId(text(r.get("verify_department_trade_id")));
		row.setCreateTime(now);
		row.setUpdateTime(now);
		try {
			customDeclareOrderResultMapper.insert(row);
		} catch (Exception e) {
			log.debug("trade finish custom declare: persist failed orderId={} err={}", orderIdNum, e.toString());
		}
	}

	private CustomsExtras resolveCustomsExtras(NormalOrders order) {
		String third = order.getThirdParams();
		if (!StringUtils.hasText(third)) {
			return null;
		}
		try {
			JsonNode root = objectMapper.readTree(third.trim());
			String customs = pickCustomsCode(root);
			String mchNo = pickMchCustomsNo(root);
			return new CustomsExtras(customs, mchNo);
		} catch (Exception e) {
			return null;
		}
	}

	private String pickCustomsCode(JsonNode root) {
		String c = textNode(root, "customs");
		if (StringUtils.hasText(c)) {
			return c;
		}
		return textNode(root, "wei_customs_area");
	}

	private String pickMchCustomsNo(JsonNode root) {
		String m = textNode(root, "mch_customs_no");
		if (StringUtils.hasText(m)) {
			return m;
		}
		return textNode(root, "customs_no");
	}

	private static String textNode(JsonNode root, String field) {
		if (root == null || !root.has(field) || root.get(field).isNull()) {
			return "";
		}
		return root.get(field).asText("").trim();
	}

	private record CustomsExtras(String customsCode, String mchCustomsNo) {}

	private static HttpEntity<String> wxEntity(String xml) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_XML);
		return new HttpEntity<>(xml, headers);
	}

	private static Map<String, String> parseWechatXmlToMap(String xml) {
		Map<String, String> map = new LinkedHashMap<>();
		try {
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			f.setNamespaceAware(false);
			Document doc = f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
			Element root = doc.getDocumentElement();
			if (root == null) {
				return map;
			}
			NodeList nl = root.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					String tag = ((Element) n).getTagName();
					String text = n.getTextContent() == null ? "" : n.getTextContent().trim();
					map.put(tag, text);
				}
			}
		} catch (Exception e) {
			return map;
		}
		return map;
	}

	private static long parseDistributorId(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String normalizeLower(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
	}

	private static String text(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static Long longFrom(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int fenFromNumber(Integer num, java.util.function.IntSupplier fallback) {
		if (num != null && num > 0) {
			return num;
		}
		return fallback.getAsInt();
	}

	private static int fenFromFeeString(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0;
		}
		try {
			String t = raw.trim();
			if (t.contains(".")) {
				double d = Double.parseDouble(t);
				return (int) Math.round(d);
			}
			return Integer.parseInt(t);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
