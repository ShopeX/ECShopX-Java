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

package cn.shopex.ecshopx.orders.service.fapiao.hangxin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.setting.CompanyBaseSettingService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HangxinFapiaoService {

	private static final DateTimeFormatter REQ_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	private final CompanyBaseSettingService companyBaseSettingService;
	private final AdminNormalOrderDetailService adminNormalOrderDetailService;
	private final MemberAccountService memberAccountService;
	private final HangxinRequestClient hangxinRequestClient;

	public HangxinFapiaoService(
			CompanyBaseSettingService companyBaseSettingService,
			AdminNormalOrderDetailService adminNormalOrderDetailService,
			MemberAccountService memberAccountService,
			HangxinRequestClient hangxinRequestClient) {
		this.companyBaseSettingService = companyBaseSettingService;
		this.adminNormalOrderDetailService = adminNormalOrderDetailService;
		this.memberAccountService = memberAccountService;
		this.hangxinRequestClient = hangxinRequestClient;
	}

	public Map<String, Object> createFapiao(Map<String, Object> params) {
		long companyId = longVal(params.get("company_id"));
		String orderIdRaw = str(params.get("order_id"));
		if (!StringUtils.hasText(orderIdRaw)) {
			throw new ResourceException("订单号缺失");
		}
		Map<String, Object> fapiaoConfig = loadFapiaoConfig(companyId);
		String desKey = cfgStrRequired(fapiaoConfig, "key", "des_key", "secret_key", "platform_key");
		String endpoint = cfgStrRequired(fapiaoConfig, "url", "endpoint", "request_url");
		String interfaceCode =
				firstNonBlank(
						cfgStr(fapiaoConfig, "interface_create", "interfaceCode", "fpkj_code"),
						"DZFPKJ");

		Map<String, Object> bundle =
				adminNormalOrderDetailService.buildOrderBundle(companyId, orderIdRaw, false);
		@SuppressWarnings("unchecked")
		Map<String, Object> orderInfo = (Map<String, Object>) bundle.getOrDefault("orderInfo", Map.of());
		@SuppressWarnings("unchecked")
		Map<String, Object> tradeInfo = (Map<String, Object>) bundle.getOrDefault("tradeInfo", Map.of());

		long userId = longVal(orderInfo.get("user_id"));
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);

		String kptype = str(params.get("kptype"));
		if (!StringUtils.hasText(kptype)) {
			kptype = "1";
		}
		String fpqqlsh = str(params.get("FPQQLSH"));
		if (!StringUtils.hasText(fpqqlsh)) {
			fpqqlsh = "HX" + companyId + "_" + orderIdRaw + "_" + System.currentTimeMillis();
		}

		String inner = buildCreateInnerXml(params, orderInfo, tradeInfo, memberInfo, fpqqlsh, kptype);
		String encrypted = HangxinTransportCrypto.encryptToTransport(inner, desKey);
		String outer = buildOuterRequestXml(fapiaoConfig, interfaceCode, encrypted);
		String responseBody = hangxinRequestClient.postXml(endpoint, outer);
		Map<String, Object> res = parseOuterResponse(responseBody, desKey);
		res.put("FPQQLSH", fpqqlsh);
		return res;
	}

	public Map<String, Object> getFapiao(Map<String, Object> params) {
		long companyId = longVal(params.get("company_id"));
		Map<String, Object> fapiaoConfig = loadFapiaoConfig(companyId);
		String desKey = cfgStrRequired(fapiaoConfig, "key", "des_key", "secret_key", "platform_key");
		String endpoint = cfgStrRequired(fapiaoConfig, "url", "endpoint", "request_url");
		String interfaceCode =
				firstNonBlank(
						cfgStr(fapiaoConfig, "interface_query", "interface_query_code", "fpcx_code"),
						"DZFPCX");

		String kptype = str(params.get("kptype"));
		if (!StringUtils.hasText(kptype)) {
			kptype = "1";
		}
		String inner = buildQueryInnerXml(params, kptype);
		String encrypted = HangxinTransportCrypto.encryptToTransport(inner, desKey);
		String outer = buildOuterRequestXml(fapiaoConfig, interfaceCode, encrypted);
		String responseBody = hangxinRequestClient.postXml(endpoint, outer);
		return parseOuterResponse(responseBody, desKey);
	}

	private Map<String, Object> loadFapiaoConfig(long companyId) {
		Map<String, Object> setting =
				companyBaseSettingService.getSetting(companyId, null, null, null, null);
		Object fc = setting.get("fapiao_config");
		if (!(fc instanceof Map<?, ?>)) {
			throw new ResourceException("发票接口配置不完整");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> cast = new LinkedHashMap<>((Map<String, Object>) fc);
		return cast;
	}

	private static String buildOuterRequestXml(
			Map<String, Object> cfg, String interfaceCode, String encryptedContent) {
		String userName = cfgStrRequired(cfg, "username", "user_name", "platform_no", "app_id");
		String passWord = cfgStrRequired(cfg, "password", "pass_word", "passWord");
		String requestTime = LocalDateTime.now().format(REQ_TIME_FMT);
		String requestCode = UUID.randomUUID().toString().replace("-", "");
		String dataExchangeId = UUID.randomUUID().toString().replace("-", "");
		String appId = firstNonBlank(cfgStr(cfg, "app_id", "appId"), "");

		StringBuilder sb = new StringBuilder(512);
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<einvoice>");
		sb.append("<globalInfo>");
		sb.append("<appId>").append(xmlEsc(appId)).append("</appId>");
		sb.append("<interfaceCode>").append(xmlEsc(interfaceCode)).append("</interfaceCode>");
		sb.append("<userName>").append(xmlEsc(userName)).append("</userName>");
		sb.append("<passWord>").append(xmlEsc(passWord)).append("</passWord>");
		sb.append("<requestTime>").append(xmlEsc(requestTime)).append("</requestTime>");
		sb.append("<requestCode>").append(xmlEsc(requestCode)).append("</requestCode>");
		sb.append("<responseCode>2</responseCode>");
		sb.append("<dataExchangeId>").append(xmlEsc(dataExchangeId)).append("</dataExchangeId>");
		sb.append("</globalInfo>");
		sb.append("<returnStateInfo/>");
		sb.append("<Data>");
		sb.append("<dataDescription>");
		sb.append("<zipCode>0</zipCode>");
		sb.append("<encryptCode>1</encryptCode>");
		sb.append("<codeType>0</codeType>");
		sb.append("</dataDescription>");
		sb.append("<content>").append(encryptedContent).append("</content>");
		sb.append("</Data>");
		sb.append("</einvoice>");
		return sb.toString();
	}

	private String buildCreateInnerXml(
			Map<String, Object> params,
			Map<String, Object> orderInfo,
			Map<String, Object> tradeInfo,
			Map<String, Object> memberInfo,
			String fpqqlsh,
			String kptype) {
		StringBuilder sb = new StringBuilder(2048);
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<business>");
		sb.append("<ORDER_ID>").append(xmlEsc(str(params.get("order_id")))).append("</ORDER_ID>");
		sb.append("<COMPANY_ID>").append(xmlEsc(str(params.get("company_id")))).append("</COMPANY_ID>");
		sb.append("<KPTYPE>").append(xmlEsc(kptype)).append("</KPTYPE>");
		sb.append("<FPQQLSH>").append(xmlEsc(fpqqlsh)).append("</FPQQLSH>");
		sb.append("<TOTAL_FEE>").append(xmlEsc(str(orderInfo.get("total_fee")))).append("</TOTAL_FEE>");
		sb.append("<USER_ID>").append(xmlEsc(str(orderInfo.get("user_id")))).append("</USER_ID>");
		sb.append("<TRADE_PAY_TYPE>")
				.append(xmlEsc(str(tradeInfo.get("pay_type"))))
				.append("</TRADE_PAY_TYPE>");
		sb.append("<BUYER_NAME>")
				.append(xmlEsc(firstNonBlank(str(memberInfo.get("username")), str(orderInfo.get("receiver_name")))))
				.append("</BUYER_NAME>");
		sb.append("<BUYER_MOBILE>")
				.append(
						xmlEsc(
								firstNonBlank(
										str(memberInfo.get("mobile")),
										str(orderInfo.get("receiver_mobile")),
										str(orderInfo.get("mobile")))))
				.append("</BUYER_MOBILE>");
		Object items = orderInfo.get("items");
		if (items instanceof List<?> list) {
			int i = 0;
			for (Object o : list) {
				if (o instanceof Map<?, ?> im) {
					sb.append("<ITEM idx=\"").append(i++).append("\">");
					sb.append("<NAME>").append(xmlEsc(str(im.get("item_name")))).append("</NAME>");
					sb.append("<PRICE>").append(xmlEsc(str(im.get("price")))).append("</PRICE>");
					sb.append("<NUM>").append(xmlEsc(str(im.get("num")))).append("</NUM>");
					sb.append("</ITEM>");
				}
			}
		}
		appendParamInvoiceHints(sb, params);
		if ("2".equals(kptype)) {
			Object prev = params.get("fapiaoinfo");
			Map<String, Object> pm = asObjectMap(prev);
			if (!pm.isEmpty()) {
				sb.append("<ORIGINAL_FP_DM>").append(xmlEsc(str(pm.get("FP_DM")))).append("</ORIGINAL_FP_DM>");
				sb.append("<ORIGINAL_FP_HM>").append(xmlEsc(str(pm.get("FP_HM")))).append("</ORIGINAL_FP_HM>");
			}
		}
		sb.append("</business>");
		return sb.toString();
	}

	private static void appendParamInvoiceHints(StringBuilder sb, Map<String, Object> params) {
		for (String k :
				List.of(
						"title",
						"invoice_title",
						"taxpayer_id",
						"tax_no",
						"bank_name",
						"bank_account",
						"address",
						"telephone",
						"email")) {
			if (params.containsKey(k) && params.get(k) != null) {
				sb.append("<P_")
						.append(k.toUpperCase())
						.append(">")
						.append(xmlEsc(str(params.get(k))))
						.append("</P_")
						.append(k.toUpperCase())
						.append(">");
			}
		}
	}

	private String buildQueryInnerXml(Map<String, Object> params, String kptype) {
		Object fpInfo = "2".equals(kptype) ? params.get("fapiaoinfo_red") : params.get("fapiaoinfo");
		Map<String, Object> fm = asObjectMap(fpInfo);
		String fpqqlsh = firstNonBlank(str(fm.get("FPQQLSH")), str(fm.get("fpqqlsh")), str(fm.get("FPQQLSH")));
		if (!StringUtils.hasText(fpqqlsh)) {
			fpqqlsh = str(params.get("FPQQLSH"));
		}
		StringBuilder sb = new StringBuilder(512);
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<query>");
		sb.append("<ORDER_ID>").append(xmlEsc(str(params.get("order_id")))).append("</ORDER_ID>");
		sb.append("<COMPANY_ID>").append(xmlEsc(str(params.get("company_id")))).append("</COMPANY_ID>");
		sb.append("<KPTYPE>").append(xmlEsc(kptype)).append("</KPTYPE>");
		sb.append("<FPQQLSH>").append(xmlEsc(fpqqlsh)).append("</FPQQLSH>");
		sb.append("<PDF_TYPE>2</PDF_TYPE>");
		sb.append("</query>");
		return sb.toString();
	}

	private Map<String, Object> parseOuterResponse(String responseXml, String desKey) {
		String code = xmlFirstTagText(responseXml, "returnCode");
		String msg = xmlFirstTagText(responseXml, "returnMessage");
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("returnCode", code.isEmpty() ? "9999" : code);
		out.put("returnMessage", msg.isEmpty() ? "未知错误" : msg);

		String encFlag = xmlFirstTagText(responseXml, "encryptCode");
		String contentBlock = xmlFirstTagText(responseXml, "content");
		List<Map<String, Object>> resultList = new ArrayList<>();
		if ("0000".equals(code)
				&& StringUtils.hasText(contentBlock)
				&& "1".equals(encFlag)) {
			String innerPlain = HangxinTransportCrypto.decryptFromTransport(contentBlock.trim(), desKey);
			if (StringUtils.hasText(innerPlain)) {
				String pdfUrl = xmlFirstTagText(innerPlain, "PDF_URL");
				if (!StringUtils.hasText(pdfUrl)) {
					pdfUrl = xmlFirstTagText(innerPlain, "pdf_url");
				}
				if (StringUtils.hasText(pdfUrl)) {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("c_url", pdfUrl);
					resultList.add(row);
				}
			}
		}
		out.put("result", resultList);
		return out;
	}

	private static String xmlFirstTagText(String xml, String tag) {
		if (!StringUtils.hasText(xml)) {
			return "";
		}
		Pattern p = Pattern.compile("<" + tag + ">\\s*([^<]*)\\s*</" + tag + ">", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(xml);
		if (m.find()) {
			return m.group(1).trim();
		}
		return "";
	}

	private static String xmlEsc(String s) {
		if (s == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(s.length() + 8);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '&' -> sb.append("&amp;");
				case '<' -> sb.append("&lt;");
				case '>' -> sb.append("&gt;");
				case '"' -> sb.append("&quot;");
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	private static Map<String, Object> asObjectMap(Object o) {
		if (o instanceof Map<?, ?> m) {
			Map<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				if (e.getKey() != null) {
					out.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			return out;
		}
		return Map.of();
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String cfgStr(Map<String, Object> cfg, String... keys) {
		for (String k : keys) {
			Object v = cfg.get(k);
			if (v != null) {
				String s = String.valueOf(v).trim();
				if (!s.isEmpty()) {
					return s;
				}
			}
		}
		return "";
	}

	private static String cfgStrRequired(Map<String, Object> cfg, String... keys) {
		String s = cfgStr(cfg, keys);
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("发票接口配置不完整");
		}
		return s;
	}

	private static String firstNonBlank(String... parts) {
		if (parts == null) {
			return "";
		}
		for (String p : parts) {
			if (StringUtils.hasText(p)) {
				return p;
			}
		}
		return "";
	}
}
