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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.popularize.config.BrokerageQrcodeProperties;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class BrokerageShareQrcodeService {

	private static final String SHARE_QRCODE_PREFIX = "data:image/png;base64,";

	private final BrokerageQrcodeProperties properties;

	public BrokerageShareQrcodeService(BrokerageQrcodeProperties properties) {
		this.properties = properties;
	}

	public Map<String, Object> getBrokerageQrcode(
			HttpServletRequest request, Map<String, Object> body, Map<String, Object> claims) {
		LinkedHashMap<String, Object> data = mergeRequestInputPreservingQueryKeyOrder(request, body);

		Object rawBrokerageType = data.get("brokerage_type");
		boolean isItem =
				Objects.equals("item", rawBrokerageType == null ? "" : String.valueOf(rawBrokerageType));
		String base = isItem ? nullToEmpty(properties.getUriItem()) : nullToEmpty(properties.getUri());

		String userIdForQuery = claims.get("user_id") == null ? "" : String.valueOf(claims.get("user_id"));
		LinkedHashMap<String, Object> query = new LinkedHashMap<>();
		query.put("user_id", userIdForQuery);
		LinkedHashMap<String, Object> params = new LinkedHashMap<>(query);
		params.putAll(data);

		String queryString = httpBuildQuery(params);
		String scene = URLEncoder.encode(queryString, StandardCharsets.UTF_8);
		String uri = base + "?" + queryString + "&scene=" + scene;

		String shareQrcode = SHARE_QRCODE_PREFIX + encodeUriAsQrPngBase64(uri);

		Map<String, Object> out = new LinkedHashMap<>(2);
		out.put("share_qrcode", shareQrcode);
		out.put("share_uir", uri);
		return out;
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	/** Merges request parameters and body; query keys follow the URL query segment order. */
	private static LinkedHashMap<String, Object> mergeRequestInputPreservingQueryKeyOrder(
			HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> flat = FlexibleHttpServletParameterMap.toObjectMap(request);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		LinkedHashSet<String> queryKeyOrder = new LinkedHashSet<>();
		String qs = request.getQueryString();
		if (qs != null && !qs.isEmpty()) {
			for (String pair : qs.split("&")) {
				if (pair.isEmpty()) {
					continue;
				}
				int eq = pair.indexOf('=');
				String rawKey = eq >= 0 ? pair.substring(0, eq) : pair;
				String decoded;
				try {
					decoded = URLDecoder.decode(rawKey.replace('+', ' '), StandardCharsets.UTF_8);
				} catch (IllegalArgumentException e) {
					decoded = rawKey;
				}
				if (decoded.isEmpty()) {
					continue;
				}
				String key = decoded.endsWith("[]") ? decoded.substring(0, decoded.length() - 2) : decoded;
				if (key.isEmpty()) {
					continue;
				}
				queryKeyOrder.add(key);
			}
		}
		for (String k : queryKeyOrder) {
			if (flat.containsKey(k)) {
				data.put(k, flat.get(k));
			}
		}
		for (Map.Entry<String, Object> e : flat.entrySet()) {
			if (!data.containsKey(e.getKey())) {
				data.put(e.getKey(), e.getValue());
			}
		}
		if (body != null) {
			data.putAll(body);
		}
		return data;
	}

	private static String httpBuildQuery(LinkedHashMap<String, Object> params) {
		List<String> segments = new ArrayList<>();
		for (Map.Entry<String, Object> e : params.entrySet()) {
			appendHttpQueryParts(e.getKey(), e.getValue(), segments);
		}
		return String.join("&", segments);
	}

	private static void appendHttpQueryParts(String key, Object v, List<String> segments) {
		if (v instanceof List<?> list) {
			if (list.isEmpty()) {
				return;
			}
			for (int i = 0; i < list.size(); i++) {
				appendHttpQueryParts(key + "[" + i + "]", list.get(i), segments);
			}
			return;
		}
		if (v instanceof Object[] arr) {
			if (arr.length == 0) {
				return;
			}
			for (int i = 0; i < arr.length; i++) {
				appendHttpQueryParts(key + "[" + i + "]", arr[i], segments);
			}
			return;
		}
		if (v instanceof Map<?, ?> map) {
			if (map.isEmpty()) {
				return;
			}
			for (Map.Entry<?, ?> inner : map.entrySet()) {
				String sk = String.valueOf(inner.getKey());
				appendHttpQueryParts(key + "[" + sk + "]", inner.getValue(), segments);
			}
			return;
		}
		String raw = httpBuildQueryScalarToString(v);
		String encKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
		String encVal = URLEncoder.encode(raw, StandardCharsets.UTF_8);
		segments.add(encKey + "=" + encVal);
	}

	private static String httpBuildQueryScalarToString(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof Boolean b) {
			return b ? "1" : "";
		}
		if (v instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue()).stripTrailingZeros().toPlainString();
		}
		return String.valueOf(v);
	}

	private static String encodeUriAsQrPngBase64(String content) {
		Map<EncodeHintType, Object> hints = new HashMap<>();
		hints.put(EncodeHintType.MARGIN, 0);
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
		try {
			BitMatrix matrix =
					new MultiFormatWriter()
							.encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);
			BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			if (!ImageIO.write(image, "png", baos)) {
				throw new ResourceException("二维码生成失败");
			}
			return Base64.getEncoder().encodeToString(baos.toByteArray());
		} catch (WriterException | IOException e) {
			throw new ResourceException("二维码生成失败");
		}
	}
}
