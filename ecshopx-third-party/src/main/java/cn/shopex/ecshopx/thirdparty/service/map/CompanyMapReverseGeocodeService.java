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

package cn.shopex.ecshopx.thirdparty.service.map;

import cn.shopex.ecshopx.common.distribution.CompanyMapReverseGeocodePort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.domain.MapConfig;
import cn.shopex.ecshopx.thirdparty.mapper.MapConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CompanyMapReverseGeocodeService implements CompanyMapReverseGeocodePort {

	private static final Logger log = LoggerFactory.getLogger(CompanyMapReverseGeocodeService.class);

	private static final ObjectMapper JSON = new ObjectMapper();

	private final MapConfigAdminService mapConfigAdminService;

	@SuppressWarnings("unused")
	private final MapConfigMapper mapConfigMapper;

	private final RestClient restClient;

	private final MessageSource messageSource;

	public CompanyMapReverseGeocodeService(
			MapConfigAdminService mapConfigAdminService,
			MapConfigMapper mapConfigMapper,
			@Qualifier("mapGeocodeRestClient") RestClient restClient,
			MessageSource messageSource) {
		this.mapConfigAdminService = mapConfigAdminService;
		this.mapConfigMapper = mapConfigMapper;
		this.restClient = restClient;
		this.messageSource = messageSource;
	}

	@Override
	public Map<String, Object> getAreaInfo(long companyId, String lat, String lng) {
		Locale locale = LocaleContextHolder.getLocale();
		Optional<MapConfig> cfgOpt = mapConfigAdminService.resolveDefaultMapConfigForThirdParty(companyId);
		if (cfgOpt.isEmpty()) {
			throw new ResourceException(
					messageSource.getMessage("distribution.pickupLocation.mapConfigInvalid", null, locale));
		}
		MapConfig cfg = cfgOpt.get();
		String typeRaw = cfg.getType() == null ? "" : cfg.getType().trim();
		String typeLog = typeRaw;
		boolean ok = false;
		try {
			LinkedHashMap<String, Object> out;
			if ("amap".equalsIgnoreCase(typeRaw)) {
				out = reverseAmap(cfg, lat, lng, companyId);
			} else if ("tencent".equalsIgnoreCase(typeRaw)) {
				if (!isNumericCoordinateString(lat) || !isNumericCoordinateString(lng)) {
					throw new BadRequestException("参数有误！经纬度不是数字！");
				}
				out = reverseTencent(cfg, lat, lng, companyId);
			} else {
				throw new ResourceException("参数有误");
			}
			if (out == null || out.isEmpty()) {
				throw new ResourceException("地理位置信息获取失败");
			}
			ok = true;
			return out;
		} finally {
			log.info("map reverse geocode companyId={} type={} success={}", companyId, typeLog, ok);
		}
	}

	private static LinkedHashMap<String, Object> blankAddressComponent() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("nation", "");
		m.put("province", "");
		m.put("city", "");
		m.put("district", "");
		m.put("street", "");
		m.put("street_number", "");
		return m;
	}

	private static boolean isNumericCoordinateString(String s) {
		if (s == null) {
			return false;
		}
		String t = s.trim();
		if (t.isEmpty()) {
			return false;
		}
		try {
			new BigDecimal(t);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private LinkedHashMap<String, Object> reverseAmap(MapConfig cfg, String lat, String lng, long companyId) {
		String appKey = cfg.getAppKey() == null ? "" : cfg.getAppKey();
		String latPart = lat == null ? "" : lat;
		String lngPart = lng == null ? "" : lng;
		String location = lngPart + "," + latPart;
		String url =
				UriComponentsBuilder.fromUriString("https://restapi.amap.com/v3/geocode/regeo")
						.queryParam("key", appKey)
						.queryParam("location", location)
						.encode(StandardCharsets.UTF_8)
						.build()
						.toUriString();
		String body;
		try {
			body = restClient.get().uri(url).retrieve().body(String.class);
		} catch (Exception e) {
			log.warn("reverse amap http failed companyId={} reason={}", companyId, e.getMessage());
			return blankAddressComponent();
		}
		JsonNode root = readTreeSafe(body);
		if (root == null) {
			log.warn("reverse amap invalid json companyId={}", companyId);
			return blankAddressComponent();
		}
		if (!"1".equals(text(root, "status"))) {
			log.warn("reverse amap bad status companyId={} status={}", companyId, text(root, "status"));
			return blankAddressComponent();
		}
		JsonNode regeocode = root.get("regeocode");
		if (regeocode == null || regeocode.isNull() || !regeocode.isObject()) {
			return blankAddressComponent();
		}
		JsonNode ac = regeocode.get("addressComponent");
		if (ac == null || !ac.isObject()) {
			return blankAddressComponent();
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		String nation = text(ac, "country");
		String province = text(ac, "province");
		String cityStr = resolveAmapCity(ac, province);
		String district = text(ac, "district");
		String township = text(ac, "township");
		String streetField = text(ac, "street");
		String street = StringUtils.hasText(township) ? township : streetField;
		String streetNumber = "";
		JsonNode sn = ac.get("streetNumber");
		if (sn != null && sn.isObject()) {
			streetNumber = text(sn, "street") + text(sn, "number");
		}
		out.put("nation", nation);
		out.put("province", province);
		out.put("city", cityStr);
		out.put("district", district);
		out.put("street", street);
		out.put("street_number", streetNumber);
		return out;
	}

	private static String resolveAmapCity(JsonNode ac, String provinceStr) {
		JsonNode cityNode = ac.get("city");
		String cityStr = "";
		if (cityNode != null && !cityNode.isNull()) {
			if (cityNode.isArray()) {
				if (!cityNode.isEmpty()) {
					JsonNode first = cityNode.get(0);
					if (first == null || first.isNull()) {
						cityStr = "";
					} else if (first.isTextual()) {
						cityStr = first.asText("");
					} else {
						cityStr = String.valueOf(first);
					}
				}
			} else if (cityNode.isObject()) {
				cityStr = String.valueOf(cityNode);
			} else {
				cityStr = cityNode.asText("");
			}
		}
		if (cityStr.isEmpty() && StringUtils.hasText(provinceStr)) {
			return provinceStr;
		}
		return cityStr;
	}

	private LinkedHashMap<String, Object> reverseTencent(MapConfig cfg, String lat, String lng, long companyId) {
		String appKey = cfg.getAppKey() == null ? "" : cfg.getAppKey();
		String latPart = lat == null ? "" : lat;
		String lngPart = lng == null ? "" : lng;
		String location = latPart + "," + lngPart;
		String url =
				UriComponentsBuilder.fromUriString("https://apis.map.qq.com/ws/geocoder/v1/")
						.queryParam("key", appKey)
						.queryParam("location", location)
						.encode(StandardCharsets.UTF_8)
						.build()
						.toUriString();
		String body;
		try {
			body = restClient.get().uri(url).retrieve().body(String.class);
		} catch (Exception e) {
			log.warn("reverse tencent http failed companyId={} reason={}", companyId, e.getMessage());
			return blankAddressComponent();
		}
		JsonNode root = readTreeSafe(body);
		if (root == null) {
			log.warn("reverse tencent invalid json companyId={}", companyId);
			return blankAddressComponent();
		}
		if (root.path("status").asInt(-1) != 0) {
			log.warn("reverse tencent bad status companyId={} status={}", companyId, root.path("status").asInt(-1));
			return blankAddressComponent();
		}
		JsonNode ac = root.path("result").path("address_component");
		if (ac.isMissingNode() || !ac.isObject()) {
			return blankAddressComponent();
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("nation", text(ac, "nation"));
		out.put("province", text(ac, "province"));
		out.put("city", text(ac, "city"));
		out.put("district", text(ac, "district"));
		out.put("street", text(ac, "street"));
		out.put("street_number", text(ac, "street_number"));
		return out;
	}

	private JsonNode readTreeSafe(String body) {
		try {
			return JSON.readTree(body == null ? "{}" : body);
		} catch (Exception e) {
			return null;
		}
	}

	private static String text(JsonNode n, String field) {
		if (n == null || n.isMissingNode()) {
			return "";
		}
		JsonNode v = n.get(field);
		return v == null || v.isNull() ? "" : v.asText("");
	}
}
