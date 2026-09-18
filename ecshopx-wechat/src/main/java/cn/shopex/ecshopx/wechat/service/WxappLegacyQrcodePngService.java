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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.openapi.OpenapiWxappShareIdRedisService;
import cn.shopex.ecshopx.wechat.repository.WeappAuthorizerAppidRepository;
import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 对齐 PHP {@code WechatBundle\Http\Controllers\Qrcode@getQrcode}：
 * 将 query 参数写入 Redis share_id，scene 仅携带 share_id，再生成小程序 unlimited 码 PNG。
 */
@Service
public class WxappLegacyQrcodePngService {

	private static final String DEFAULT_TEMPLATE_NAME = "yykweishop";
	private static final String DEFAULT_PAGE = "pages/index";
	private static final String GOODS_DETAIL_PAGE = "pages/goodsdetail";

	private static final Set<String> EXCLUDED_PARAM_KEYS =
			Set.of("appname", "appsecret", "appid", "company_id", "temp_name");

	private final WeappAuthorizerAppidRepository weappAuthorizerAppidRepository;
	private final OpenapiWxappShareIdRedisService openapiWxappShareIdRedisService;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;

	public WxappLegacyQrcodePngService(
			WeappAuthorizerAppidRepository weappAuthorizerAppidRepository,
			OpenapiWxappShareIdRedisService openapiWxappShareIdRedisService,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient) {
		this.weappAuthorizerAppidRepository = weappAuthorizerAppidRepository;
		this.openapiWxappShareIdRedisService = openapiWxappShareIdRedisService;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
	}

	public byte[] generateQrcodePng(HttpServletRequest request) {
		String wxappAppid = firstParamValue(request, "appid");
		long companyId = parsePositiveLongOrZero(firstParamValue(request, "company_id"));

		if (!StringUtils.hasText(wxappAppid)) {
			String templateName = firstParamValue(request, "temp_name");
			if (!StringUtils.hasText(templateName)) {
				templateName = DEFAULT_TEMPLATE_NAME;
			}
			if (companyId <= 0L) {
				throw new ResourceException("参数错误");
			}
			wxappAppid =
					weappAuthorizerAppidRepository
							.findAuthorizerAppid(companyId, templateName)
							.orElseThrow(() -> new ResourceException("没有绑定小程序"));
		}

		LinkedHashMap<String, Object> sceneParams = buildSceneParams(request);
		String page = resolvePage(request);
		if (companyId <= 0L) {
			throw new ResourceException("参数错误");
		}

		String shareId = openapiWxappShareIdRedisService.getShareId(companyId, sceneParams);
		String scene = buildShareIdScene(shareId);
		return wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(wxappAppid, scene, page);
	}

	private static LinkedHashMap<String, Object> buildSceneParams(HttpServletRequest request) {
		LinkedHashMap<String, Object> input = new LinkedHashMap<>();
		for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
			String key = entry.getKey();
			if (key == null || EXCLUDED_PARAM_KEYS.contains(key)) {
				continue;
			}
			String value = firstNonNullValue(entry.getValue());
			if (shouldDropParamValue(value)) {
				continue;
			}
			input.put(key, value);
		}

		if (input.containsKey("distributor_id")) {
			Object distributorId = input.remove("distributor_id");
			if (distributorId != null && !input.containsKey("dtid")) {
				input.put("dtid", distributorId);
			}
		}
		input.remove("page");
		return input;
	}

	private static String resolvePage(HttpServletRequest request) {
		boolean hasPageParam = request.getParameterMap().containsKey("page");
		String pageRaw = firstParamValue(request, "page");
		if (StringUtils.hasText(firstParamValue(request, "id")) && !hasPageParam) {
			return GOODS_DETAIL_PAGE;
		}
		String page = StringUtils.hasText(pageRaw) ? pageRaw.trim() : DEFAULT_PAGE;
		if (page.startsWith("/")) {
			page = page.substring(1);
		}
		return page;
	}

	private static String buildShareIdScene(String shareId) {
		return URLEncoder.encode("share_id", StandardCharsets.UTF_8)
				+ "="
				+ URLEncoder.encode(shareId, StandardCharsets.UTF_8);
	}

	private static String firstParamValue(HttpServletRequest request, String name) {
		return firstNonNullValue(request.getParameterValues(name));
	}

	private static String firstNonNullValue(String[] values) {
		if (values == null || values.length == 0) {
			return null;
		}
		return values[0];
	}

	private static boolean shouldDropParamValue(String value) {
		if (value == null) {
			return true;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return true;
		}
		return "undefined".equals(trimmed) || "0".equals(trimmed);
	}

	private static long parsePositiveLongOrZero(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
