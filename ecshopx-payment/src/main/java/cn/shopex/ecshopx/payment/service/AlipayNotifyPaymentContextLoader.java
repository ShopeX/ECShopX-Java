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

package cn.shopex.ecshopx.payment.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.payment.integration.alipay.AlipayNotifyOrderSidePort;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AlipayNotifyPaymentContextLoader {

	private static final Logger log = LoggerFactory.getLogger(AlipayNotifyPaymentContextLoader.class);

	private final AlipayNotifyOrderSidePort alipayNotifyOrderSidePort;

	public AlipayNotifyPaymentContextLoader(AlipayNotifyOrderSidePort alipayNotifyOrderSidePort) {
		this.alipayNotifyOrderSidePort = alipayNotifyOrderSidePort;
	}

	public Optional<AlipayNotifyPaymentContext> load(HttpServletRequest request) {
		Map<String, Object> objectMap = FlexibleHttpServletParameterMap.toObjectMap(request);
		Map<String, String> rawFlat = NotifyHttpParameterFlatten.flattenObjectMap(objectMap);
		logAlipayResponseSummary(rawFlat);

		boolean passbackPresent = request.getParameterMap().containsKey("passback_params");
		if (!passbackPresent) {
			return Optional.empty();
		}

		String charsetName = rawFlat.getOrDefault("charset", "gb2312");
		Charset sourceCharset = resolveSourceCharset(charsetName);
		Map<String, String> encodedFlat = encodeMapValuesToUtf8(rawFlat, sourceCharset);

		String passbackRaw = "";
		if (encodedFlat.containsKey("passback_params")) {
			passbackRaw = Objects.requireNonNullElse(encodedFlat.get("passback_params"), "");
		}
		if (!StringUtils.hasText(passbackRaw)) {
			passbackRaw = Objects.requireNonNullElse(request.getParameter("passback_params"), "");
		}

		String passbackDecoded;
		try {
			passbackDecoded = URLDecoder.decode(passbackRaw, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("回传参数无法解码");
		}
		Map<String, String> returnData = parseQueryString(passbackDecoded);

		String companyRaw = returnData.get("company_id");
		if (!StringUtils.hasText(companyRaw)) {
			throw new BadRequestException("缺少企业编号");
		}
		long companyId;
		try {
			companyId = Long.parseLong(companyRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("企业编号格式错误");
		}

		String outTradeNo = encodedFlat.get("out_trade_no");
		long distributorIdForSetting =
				alipayNotifyOrderSidePort.resolveDistributorIdForAlipayRedis(companyId, outTradeNo);

		AlipayNotifyPaymentContext ctx = new AlipayNotifyPaymentContext(
				encodedFlat, returnData, companyId, outTradeNo, distributorIdForSetting);
		return Optional.of(ctx);
	}

	private static void logAlipayResponseSummary(Map<String, String> rawFlat) {
		List<String> keys = new ArrayList<>(rawFlat.keySet());
		keys.removeIf(k -> "sign".equalsIgnoreCase(k) || "sign_type".equalsIgnoreCase(k));
		log.info("alipay:response: keys={}", keys);
	}

	private static Charset resolveSourceCharset(String rawName) {
		if (!StringUtils.hasText(rawName)) {
			return Charset.forName("GB2312");
		}
		try {
			return Charset.forName(rawName.trim());
		} catch (Exception e) {
			return Charset.forName("GB2312");
		}
	}

	private static Map<String, String> encodeMapValuesToUtf8(Map<String, String> in, Charset fromCharset) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : in.entrySet()) {
			out.put(e.getKey(), convertParamValueToUtf8(e.getValue(), fromCharset));
		}
		return out;
	}

	private static String convertParamValueToUtf8(String value, Charset fromCharset) {
		if (value == null) {
			return null;
		}
		if (StandardCharsets.UTF_8.equals(fromCharset)
				|| "UTF-8".equalsIgnoreCase(fromCharset.name())) {
			return value;
		}
		byte[] bytes = value.getBytes(StandardCharsets.ISO_8859_1);
		return new String(bytes, fromCharset);
	}

	private static Map<String, String> parseQueryString(String qs) {
		Map<String, String> m = new LinkedHashMap<>();
		if (!StringUtils.hasText(qs)) {
			return m;
		}
		for (String part : qs.split("&")) {
			if (!StringUtils.hasText(part)) {
				continue;
			}
			int eq = part.indexOf('=');
			try {
				if (eq < 0) {
					String k = URLDecoder.decode(part, StandardCharsets.UTF_8);
					m.put(k, "");
				} else {
					String k = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
					String v = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
					m.put(k, v);
				}
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("回传参数格式错误");
			}
		}
		return m;
	}
}
