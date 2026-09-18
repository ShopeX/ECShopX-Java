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

package cn.shopex.ecshopx.wechat.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiLegacyZeroCodeFailException;
import cn.shopex.ecshopx.common.openapi.OpenapiWxappQrcodePort;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenapiWxappQrcodePortImpl implements OpenapiWxappQrcodePort {

	private static final Logger log = LoggerFactory.getLogger(OpenapiWxappQrcodePortImpl.class);

	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;
	private final OpenapiWxappShareIdRedisService openapiWxappShareIdRedisService;

	public OpenapiWxappQrcodePortImpl(
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient,
			OpenapiWxappShareIdRedisService openapiWxappShareIdRedisService) {
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
		this.openapiWxappShareIdRedisService = openapiWxappShareIdRedisService;
	}

	@Override
	public byte[] generateSalespersonTaskQrcode(
			long companyId, String pathType, Map<String, Object> scene, String widthRaw) {
		String wxAppid = weappAuthorizerAppidRepository
				.findAuthorizerAppid(companyId, "yykweishop")
				.orElse(null);
		if (wxAppid == null) {
			throw new OpenapiLegacyZeroCodeFailException("参数错误");
		}

		String page = resolvePage(pathType);
		LinkedHashMap<String, Object> sceneForQuery = new LinkedHashMap<>();
		if ("goods_detail".equals(pathType) || "recommend_detail".equals(pathType)) {
			sceneForQuery.put("id", scene.get("id"));
		}
		String shareId = openapiWxappShareIdRedisService.getShareId(companyId, scene);
		sceneForQuery.put("share_id", shareId);

		String sceneStr = httpBuildQuery(sceneForQuery);
		log.debug("daogou - getWxQrCode - scene: {} -> {}", scene, sceneStr);
		Integer widthPx = parseWidth(widthRaw);
		return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxAppid, sceneStr, page, widthPx);
	}

	private static String resolvePage(String pathType) {
		return switch (pathType) {
			case "recommend_detail" -> "pages/recommend/detail";
			case "goods_detail" -> "pages/item/espier-detail";
			case "recommend_list" -> "pages/recommend/list";
			case "goods_list" -> "pages/item/list";
			case "share_land" -> "pages/share-land";
			default -> "pages/index";
		};
	}

	private static Integer parseWidth(String widthRaw) {
		if (widthRaw == null || widthRaw.isBlank()) {
			return null;
		}
		try {
			int v = Integer.parseInt(widthRaw.trim());
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String httpBuildQuery(LinkedHashMap<String, Object> params) {
		List<String> segments = new ArrayList<>();
		for (Map.Entry<String, Object> e : params.entrySet()) {
			String raw = httpBuildQueryScalarToString(e.getValue());
			String encKey = URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8);
			String encVal = URLEncoder.encode(raw, StandardCharsets.UTF_8);
			segments.add(encKey + "=" + encVal);
		}
		return String.join("&", segments);
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
}
