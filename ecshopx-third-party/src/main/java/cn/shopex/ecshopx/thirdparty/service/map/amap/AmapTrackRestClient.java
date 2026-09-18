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

package cn.shopex.ecshopx.thirdparty.service.map.amap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AmapTrackRestClient {

	private static final String SUCCESS = "10000";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public AmapTrackRestClient(
			@Qualifier("amapTrackHttpRestClient") RestClient restClient, ObjectMapper objectMapper) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> addService(String appKey, String name, String description) {
		String uri = UriComponentsBuilder.fromPath("/v1/track/service/add")
				.queryParam("key", appKey)
				.queryParam("name", name)
				.queryParam("desc", description == null ? "" : description)
				.encode()
				.build()
				.toUriString();
		String body = restClient.post().uri(uri).retrieve().body(String.class);
		JsonNode root = readJson(body);
		String err = text(root, "errcode");
		if (!SUCCESS.equals(err)) {
			return Collections.emptyMap();
		}
		JsonNode data = root.get("data");
		if (data == null || data.isNull() || !data.isObject()) {
			return Collections.emptyMap();
		}
		Map<String, Object> out = new LinkedHashMap<>();
		putIfPresent(out, data, "sid");
		putIfPresent(out, data, "name");
		putIfPresent(out, data, "desc");
		return out;
	}

	public Map<String, Object> addPolygonGeofence(
			String appKey, String sid, String name, String desc, String points) {
		String uri = UriComponentsBuilder.fromPath("/v1/track/geofence/add/polygon")
				.queryParam("key", appKey)
				.queryParam("sid", sid)
				.queryParam("name", name)
				.queryParam("desc", desc == null ? "" : desc)
				.queryParam("points", points)
				.encode()
				.build()
				.toUriString();
		String body = restClient.post().uri(uri).retrieve().body(String.class);
		JsonNode root = readJson(body);
		String err = text(root, "errcode");
		if (!SUCCESS.equals(err)) {
			return Collections.emptyMap();
		}
		JsonNode data = root.get("data");
		if (data == null || data.isNull() || !data.isObject()) {
			return Collections.emptyMap();
		}
		Map<String, Object> out = new LinkedHashMap<>();
		JsonNode gfid = data.get("gfid");
		if (gfid != null && !gfid.isNull()) {
			out.put("gfid", gfid.asText(""));
		}
		return out;
	}

	public boolean updatePolygonGeofence(
			String appKey, String sid, String gfid, String name, String desc, String points) {
		String uri = UriComponentsBuilder.fromPath("/v1/track/geofence/update/polygon")
				.queryParam("key", appKey)
				.queryParam("sid", sid)
				.queryParam("gfid", gfid)
				.queryParam("name", name)
				.queryParam("desc", desc == null ? "" : desc)
				.queryParam("points", points)
				.encode()
				.build()
				.toUriString();
		String body = restClient.post().uri(uri).retrieve().body(String.class);
		JsonNode root = readJson(body);
		return SUCCESS.equals(text(root, "errcode"));
	}

	/**
	 * POST /v1/track/geofence/delete — 删除指定轨迹围栏（高德轨迹服务围栏删除接口）。
	 *
	 * @return 当且仅当响应 JSON 中 {@code errcode} 为 {@code 10000}、{@code data} 为对象且存在非 null 的 {@code gfids} 时：若
	 *         {@code gfids} 为非空 JSON 数组，或 {@code gfids} 为字符串且经 trim 后非空，则返回 {@code true}；否则（含 errcode 非成功、
	 *         {@code data}/{@code gfids} 缺失或非上述形态）返回 {@code false}
	 */
	public boolean deleteGeofenceByGfid(String appKey, String sid, String gfid) {
		if (!StringUtils.hasText(gfid)) {
			return false;
		}
		String uri = UriComponentsBuilder.fromPath("/v1/track/geofence/delete")
				.queryParam("key", appKey)
				.queryParam("sid", sid)
				.queryParam("gfids", gfid.trim())
				.encode()
				.build()
				.toUriString();
		String body = restClient.post().uri(uri).retrieve().body(String.class);
		JsonNode root = readJson(body);
		if (!SUCCESS.equals(text(root, "errcode"))) {
			return false;
		}
		JsonNode data = root.get("data");
		if (data == null || data.isNull() || !data.isObject() || !data.hasNonNull("gfids")) {
			return false;
		}
		JsonNode gfids = data.get("gfids");
		if (gfids.isArray() && gfids.size() > 0) {
			return true;
		}
		if (gfids.isTextual() && StringUtils.hasText(gfids.asText())) {
			return true;
		}
		return false;
	}

	private JsonNode readJson(String body) {
		try {
			return objectMapper.readTree(body == null ? "{}" : body);
		} catch (Exception e) {
			return objectMapper.createObjectNode();
		}
	}

	private static String text(JsonNode n, String field) {
		if (n == null || n.isMissingNode()) {
			return "";
		}
		JsonNode v = n.get(field);
		return v == null || v.isNull() ? "" : v.asText("");
	}

	private static void putIfPresent(Map<String, Object> out, JsonNode data, String key) {
		JsonNode v = data.get(key);
		if (v != null && !v.isNull()) {
			if (v.isTextual()) {
				out.put(key, v.asText());
			} else if (v.isNumber()) {
				out.put(key, v.numberValue());
			} else {
				out.put(key, v.asText());
			}
		}
	}

}
