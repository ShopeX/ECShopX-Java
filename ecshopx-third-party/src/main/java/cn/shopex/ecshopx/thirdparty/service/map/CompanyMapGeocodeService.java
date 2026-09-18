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

import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.domain.MapConfig;
import cn.shopex.ecshopx.thirdparty.mapper.MapConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
public class CompanyMapGeocodeService implements CompanyMapGeocodePort {

	private static final Logger log = LoggerFactory.getLogger(CompanyMapGeocodeService.class);

	private static final ObjectMapper JSON = new ObjectMapper();

	private static final String TENCENT_SIGN_PATH = "/ws/geocoder/v1";

	private static final String TENCENT_GEOCODE_BASE = "https://apis.map.qq.com/ws/geocoder/v1";

	private static final String AMAP_GEOCODE_GEO = "https://restapi.amap.com/v3/geocode/geo";

	private static final String AMAP_REGEO = "https://restapi.amap.com/v3/geocode/regeo";

	private final MapConfigMapper mapConfigMapper;

	private final RestClient restClient;

	private final MessageSource messageSource;

	public CompanyMapGeocodeService(
			MapConfigMapper mapConfigMapper,
			@Qualifier("mapGeocodeRestClient") RestClient restClient,
			MessageSource messageSource) {
		this.mapConfigMapper = mapConfigMapper;
		this.restClient = restClient;
		this.messageSource = messageSource;
	}

	@Override
	public GeocodeLatLng geocode(long companyId, String city, String address) {
		Locale locale = LocaleContextHolder.getLocale();
		MapConfig config = resolveMapConfig(companyId, locale);
		String type = config.getType();
		String c = city == null ? "" : city.trim();
		String a = address == null ? "" : address.trim();
		String combined = c + a;
		boolean ok = false;
		GeocodeLatLng result = null;
		try {
			result = geocodeResolved(config, type, c, combined, locale, true);
			ok = result != null;
			return result;
		} finally {
			log.info("map geocode companyId={} type={} success={}", companyId, type, ok);
		}
	}

	@Override
	public GeocodeLatLng geocodeAllowEmpty(long companyId, String city, String address) {
		Locale locale = LocaleContextHolder.getLocale();
		MapConfig config = resolveMapConfig(companyId, locale);
		String type = config.getType();
		String c = city == null ? "" : city.trim();
		String a = address == null ? "" : address.trim();
		String combined = c + a;
		boolean ok = false;
		GeocodeLatLng result = null;
		try {
			result = geocodeResolved(config, type, c, combined, locale, false);
			ok = result != null;
			return result;
		} finally {
			log.info("map geocode companyId={} type={} success={}", companyId, type, ok);
		}
	}

	private GeocodeLatLng geocodeResolved(
			MapConfig config, String type, String cityTrim, String combined, Locale locale, boolean strict) {
		GeocodeLatLng r;
		if ("amap".equalsIgnoreCase(type)) {
			String appKey = config.getAppKey();
			r = geocodeAmapInternal(appKey, cityTrim, combined);
		} else if ("tencent".equalsIgnoreCase(type)) {
			r = geocodeTencentInternal(config, cityTrim, combined);
		} else {
			throw mapConfigInvalid(locale);
		}
		if (strict && r == null) {
			throw addressError(locale);
		}
		return r;
	}

	@Override
	public Object getLatAndLngByPositionRaw(long companyId, String address) {
		Locale locale = LocaleContextHolder.getLocale();
		String raw = address == null ? "" : address.trim();
		if (!StringUtils.hasText(raw) || "0".equals(raw)) {
			return Collections.emptyList();
		}
		MapConfig cfg = resolveMapConfig(companyId, locale);
		String type = cfg.getType();
		if ("tencent".equalsIgnoreCase(type)) {
			return forwardTencentPositionRaw(cfg, raw);
		}
		if ("amap".equalsIgnoreCase(type)) {
			return forwardAmapGeocodesRaw(cfg, raw);
		}
		throw mapConfigInvalid(locale);
	}

	@Override
	public Object getPositionByLatAndLngRaw(long companyId, String lat, String lng) {
		if (!isNumericCoordinateString(lat) || !isNumericCoordinateString(lng)) {
			return Collections.emptyList();
		}
		Locale locale = LocaleContextHolder.getLocale();
		try {
			MapConfig cfg = resolveMapConfig(companyId, locale);
			String type = cfg.getType();
			if ("tencent".equalsIgnoreCase(type)) {
				return reverseTencentPositionRaw(cfg, lat, lng);
			}
			if ("amap".equalsIgnoreCase(type)) {
				return reverseAmapPositionRaw(cfg, lat, lng);
			}
			throw mapConfigInvalid(locale);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("map reverse geocode failed companyId={} reason={}", companyId, e.getMessage());
			return Collections.emptyList();
		}
	}

	private static boolean isNumericCoordinateString(String s) {
		if (s == null) {
			return false;
		}
		try {
			Double.parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private Object reverseTencentPositionRaw(MapConfig cfg, String lat, String lng) {
		try {
			String body = tencentReverseGeocoderV1HttpBody(cfg, lat, lng);
			if (body == null || body.isEmpty()) {
				log.error("tencent reverse geocode empty body companyId={}", cfg.getCompanyId());
				return Collections.emptyList();
			}
			JsonNode root = JSON.readTree(body);
			if (!root.has("status") || root.path("status").asInt(-1) != 0) {
				log.error("tencent reverse geocode non-zero status companyId={} body={}", cfg.getCompanyId(), body);
				return Collections.emptyList();
			}
			JsonNode resultNode = root.path("result");
			if (resultNode.isMissingNode() || resultNode.isNull() || !resultNode.isObject()) {
				return Collections.emptyList();
			}
			return JSON.convertValue(resultNode, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			log.warn("tencent reverse geocode failed companyId={} reason={}", cfg.getCompanyId(), e.getMessage());
			return Collections.emptyList();
		}
	}

	private String tencentReverseGeocoderV1HttpBody(MapConfig cfg, String lat, String lng) {
		String appKey = cfg.getAppKey() == null ? "" : cfg.getAppKey();
		String appSecret = cfg.getAppSecret() == null ? "" : cfg.getAppSecret();
		TreeMap<String, String> signParams = new TreeMap<>();
		signParams.put("get_poi", "0");
		signParams.put("key", appKey);
		signParams.put("location", lat + "," + lng);
		signParams.put("poi_options", "address_format=short");
		String sig = tencentSig(signParams, appSecret);
		signParams.put("sig", sig);
		UriComponentsBuilder ub = UriComponentsBuilder.fromUriString(TENCENT_GEOCODE_BASE);
		for (Map.Entry<String, String> e : signParams.entrySet()) {
			ub.queryParam(e.getKey(), e.getValue());
		}
		String url = ub.encode(StandardCharsets.UTF_8).build().toUriString();
		return restClient.get().uri(url).retrieve().body(String.class);
	}

	private Object reverseAmapPositionRaw(MapConfig cfg, String lat, String lng) {
		String appKey = cfg.getAppKey() == null ? "" : cfg.getAppKey();
		String location = lng + "," + lat;
		String url = UriComponentsBuilder.fromUriString(AMAP_REGEO)
				.queryParam("key", appKey)
				.queryParam("location", location)
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUriString();
		try {
			String body = restClient.get().uri(url).retrieve().body(String.class);
			JsonNode root = readTreeSafe(body);
			if (root == null || !"1".equals(text(root, "status"))) {
				return Collections.emptyList();
			}
			JsonNode regeocode = root.get("regeocode");
			if (regeocode == null || regeocode.isNull() || !regeocode.isObject()) {
				return Collections.emptyList();
			}
			JsonNode ac = regeocode.get("addressComponent");
			if (ac == null || !ac.isObject()) {
				return Collections.emptyList();
			}
			String province = text(ac, "province");
			LinkedHashMap<String, Object> component = new LinkedHashMap<>();
			component.put("nation", text(ac, "country"));
			component.put("province", province);
			component.put("city", resolveAmapCity(ac, province));
			component.put("district", text(ac, "district"));
			String township = text(ac, "township");
			String streetField = text(ac, "street");
			component.put("street", StringUtils.hasText(township) ? township : streetField);
			String streetNumber = "";
			JsonNode sn = ac.get("streetNumber");
			if (sn != null && sn.isObject()) {
				streetNumber = text(sn, "street") + text(sn, "number");
			}
			component.put("street_number", streetNumber);
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("address", text(regeocode, "formatted_address"));
			out.put("address_component", component);
			return out;
		} catch (Exception e) {
			log.warn("amap reverse geocode failed companyId={} reason={}", cfg.getCompanyId(), e.getMessage());
			return Collections.emptyList();
		}
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

	private MapConfig resolveMapConfig(long companyId, Locale locale) {
		List<MapConfig> defaults = mapConfigMapper.selectList(
				new LambdaQueryWrapper<MapConfig>()
						.eq(MapConfig::getCompanyId, companyId)
						.eq(MapConfig::getIsDefault, true)
						.in(MapConfig::getType, "amap", "tencent")
						.orderByAsc(MapConfig::getId));
		MapConfig row = firstWithKey(defaults);
		if (row == null) {
			List<MapConfig> any = mapConfigMapper.selectList(
					new LambdaQueryWrapper<MapConfig>()
							.eq(MapConfig::getCompanyId, companyId)
							.in(MapConfig::getType, "amap", "tencent")
							.orderByAsc(MapConfig::getId));
			row = firstWithKey(any);
		}
		if (row == null) {
			throw mapConfigInvalid(locale);
		}
		return row;
	}

	private static MapConfig firstWithKey(List<MapConfig> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.stream().filter(c -> StringUtils.hasText(c.getAppKey())).findFirst().orElse(null);
	}

	private ResourceException mapConfigInvalid(Locale locale) {
		return new ResourceException(
				messageSource.getMessage("distribution.pickupLocation.mapConfigInvalid", null, locale));
	}

	private ResourceException addressError(Locale locale) {
		return new ResourceException(
				messageSource.getMessage("distribution.pickupLocation.addressRecognitionError", null, locale));
	}

	private GeocodeLatLng geocodeAmapInternal(String appKey, String city, String combinedAddress) {
		if (!StringUtils.hasText(appKey)) {
			return null;
		}
		String url = UriComponentsBuilder.fromUriString("https://restapi.amap.com/v3/geocode/geo")
				.queryParam("key", appKey)
				.queryParam("address", combinedAddress)
				.queryParam("city", city)
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUriString();
		String body = restClient.get().uri(url).retrieve().body(String.class);
		JsonNode root = readTreeSafe(body);
		if (root == null) {
			return null;
		}
		String status = text(root, "status");
		if (!"1".equals(status)) {
			return null;
		}
		JsonNode geocodes = root.get("geocodes");
		if (geocodes == null || !geocodes.isArray() || geocodes.isEmpty()) {
			return null;
		}
		String loc = text(geocodes.get(0), "location");
		if (!StringUtils.hasText(loc)) {
			return null;
		}
		int comma = loc.indexOf(',');
		if (comma <= 0 || comma >= loc.length() - 1) {
			return null;
		}
		String lngStr = loc.substring(0, comma).trim();
		String latStr = loc.substring(comma + 1).trim();
		if (!StringUtils.hasText(latStr) || !StringUtils.hasText(lngStr)) {
			return null;
		}
		return new GeocodeLatLng(latStr, lngStr);
	}

	/**
	 * Tencent forward geocoder (address → location). Shared by raw forward and strict geocode paths.
	 *
	 * @param combinedAddress city+street or full address string (cleaned inside)
	 * @param regionOptional optional city hint (PHP {@code region} query param)
	 * @return response body or {@code null} if address empty after cleaning
	 */
	private String tencentGeocoderV1HttpBody(MapConfig cfg, String combinedAddress, String regionOptional) {
		String cleaned = cleanAddressForTencent(combinedAddress);
		if (!StringUtils.hasText(cleaned)) {
			return null;
		}
		String appKey = cfg.getAppKey() == null ? "" : cfg.getAppKey();
		String appSecret = cfg.getAppSecret() == null ? "" : cfg.getAppSecret();
		TreeMap<String, String> signParams = new TreeMap<>();
		signParams.put("address", cleaned);
		signParams.put("key", appKey);
		if (StringUtils.hasText(regionOptional)) {
			signParams.put("region", regionOptional.trim());
		}
		String sig = tencentSig(signParams, appSecret);
		signParams.put("sig", sig);
		UriComponentsBuilder ub = UriComponentsBuilder.fromUriString(TENCENT_GEOCODE_BASE);
		for (Map.Entry<String, String> e : signParams.entrySet()) {
			ub.queryParam(e.getKey(), e.getValue());
		}
		String url = ub.encode(StandardCharsets.UTF_8).build().toUriString();
		return restClient.get().uri(url).retrieve().body(String.class);
	}

	private Object forwardTencentPositionRaw(MapConfig cfg, String address) {
		try {
			String body = tencentGeocoderV1HttpBody(cfg, address, null);
			if (body == null) {
				return Collections.emptyList();
			}
			if (body.isEmpty()) {
				log.error("tencent forward geocode empty body companyId={}", cfg.getCompanyId());
				return tencentApiErrorShape();
			}
			JsonNode root = JSON.readTree(body);
			if (!root.has("status") || root.path("status").asInt(-1) != 0) {
				log.error("tencent forward geocode non-zero status companyId={} body={}", cfg.getCompanyId(), body);
				return tencentApiErrorShape();
			}
			return JSON.readValue(body, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			log.warn("tencent forward geocode failed companyId={} reason={}", cfg.getCompanyId(), e.getMessage());
			return Collections.emptyList();
		}
	}

	private static Map<String, Object> tencentApiErrorShape() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("status", 0);
		m.put("message", "");
		m.put("result", Collections.emptyList());
		return m;
	}

	private static String cleanAddressForTencent(String raw) {
		String s = raw;
		s = s.replace("\r\n", "");
		s = s.replace("\r", "");
		s = s.replace("\n", "");
		s = s.replace("null", "");
		s = s.replace(" ", "");
		return s;
	}

	private static String tencentSig(TreeMap<String, String> signParamsWithoutSig, String appSecret) {
		StringBuilder qs = new StringBuilder();
		for (Map.Entry<String, String> e : signParamsWithoutSig.entrySet()) {
			if (qs.length() > 0) {
				qs.append('&');
			}
			String v = e.getValue() == null ? "" : e.getValue();
			qs.append(e.getKey()).append('=').append(v);
		}
		String queryWithQuestion = qs.length() == 0 ? "?" : "?" + qs;
		String secret = appSecret == null ? "" : appSecret;
		String toHash = TENCENT_SIGN_PATH + queryWithQuestion + secret;
		return md5LowerHex(toHash);
	}

	private static String md5LowerHex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private Object forwardAmapGeocodesRaw(MapConfig cfg, String address) {
		String appKey = cfg.getAppKey() == null ? "" : cfg.getAppKey();
		String appSecret = cfg.getAppSecret() == null ? "" : cfg.getAppSecret();
		TreeMap<String, String> data = new TreeMap<>();
		data.put("address", address);
		data.put("batch", "false");
		data.put("key", appKey);
		String sig = amapSig(data, appSecret);
		String url = UriComponentsBuilder.fromUriString(AMAP_GEOCODE_GEO)
				.queryParam("key", appKey)
				.queryParam("address", address)
				.queryParam("batch", "false")
				.queryParam("sig", sig)
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUriString();
		try {
			String body = restClient.get().uri(url).retrieve().body(String.class);
			JsonNode root;
			try {
				root = JSON.readTree(body == null ? "{}" : body);
			} catch (Exception e) {
				return Collections.emptyList();
			}
			String status = text(root, "status");
			if (!"1".equals(status)) {
				return Collections.emptyList();
			}
			JsonNode geocodes = root.get("geocodes");
			if (geocodes == null || !geocodes.isArray()) {
				return Collections.emptyList();
			}
			List<Map<String, Object>> out = new ArrayList<>();
			for (JsonNode n : geocodes) {
				out.add(JSON.convertValue(n, new TypeReference<Map<String, Object>>() {}));
			}
			return out;
		} catch (Exception e) {
			log.warn("amap forward geocode failed companyId={} reason={}", cfg.getCompanyId(), e.getMessage());
			return Collections.emptyList();
		}
	}

	private static String amapSig(TreeMap<String, String> sortedParams, String appSecret) {
		StringBuilder qs = new StringBuilder();
		for (Map.Entry<String, String> e : sortedParams.entrySet()) {
			if (qs.length() > 0) {
				qs.append('&');
			}
			qs.append(e.getKey()).append('=').append(e.getValue());
		}
		String secret = appSecret == null ? "" : appSecret;
		return md5LowerHex(qs + secret);
	}

	private GeocodeLatLng geocodeTencentInternal(MapConfig cfg, String city, String combinedAddress) {
		String region = StringUtils.hasText(city) ? city : null;
		String body = tencentGeocoderV1HttpBody(cfg, combinedAddress, region);
		if (body == null) {
			return null;
		}
		JsonNode root = readTreeSafe(body);
		if (root == null) {
			return null;
		}
		int status = root.path("status").asInt(-1);
		if (status != 0) {
			return null;
		}
		JsonNode loc = root.path("result").path("location");
		if (loc.isMissingNode() || !loc.has("lat") || !loc.has("lng")) {
			return null;
		}
		String latStr = loc.get("lat").asText(null);
		String lngStr = loc.get("lng").asText(null);
		if (!StringUtils.hasText(latStr) || !StringUtils.hasText(lngStr)) {
			return null;
		}
		return new GeocodeLatLng(latStr, lngStr);
	}

	private static JsonNode readTreeSafe(String body) {
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
