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

package cn.shopex.ecshopx.community.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.domain.CommunityChiefZiti;
import cn.shopex.ecshopx.community.mapper.CommunityChiefZitiMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CommunityChiefZitiService {

	private final CommunityChiefZitiMapper communityChiefZitiMapper;
	private final ObjectMapper objectMapper;

	public CommunityChiefZitiService(
			CommunityChiefZitiMapper communityChiefZitiMapper, ObjectMapper objectMapper) {
		this.communityChiefZitiMapper = communityChiefZitiMapper;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> listChiefZitiForH5(long chiefId) {
		List<CommunityChiefZiti> rows =
				communityChiefZitiMapper.selectList(
						Wrappers.<CommunityChiefZiti>lambdaQuery()
								.eq(CommunityChiefZiti::getChiefId, chiefId));
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (CommunityChiefZiti row : rows) {
			out.add(entityToSnakeCaseMap(row, false));
		}
		return out;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createChiefZiti(long chiefId, Map<String, Object> params) {
		String zitiName = stringVal(params.get("ziti_name"));
		String address = stringVal(params.get("address"));
		if (!StringUtils.hasText(zitiName)) {
			throw new BadRequestException("自提点名称不能为空");
		}
		if (!StringUtils.hasText(address)) {
			throw new BadRequestException("地址不能为空");
		}

		CommunityChiefZiti entity = new CommunityChiefZiti();
		entity.setChiefId(chiefId);
		entity.setZitiName(zitiName);
		entity.setProvince(nullIfBlank(stringVal(params.get("province"))));
		entity.setCity(nullIfBlank(stringVal(params.get("city"))));
		entity.setArea(nullIfBlank(stringVal(params.get("area"))));
		entity.setRegionsId(toJsonColumn(params.get("regions_id")));
		entity.setRegions(toJsonColumn(params.get("regions")));
		entity.setAddress(address);
		entity.setLat(objectToStringOrNull(params.get("lat")));
		entity.setLng(objectToStringOrNull(params.get("lng")));
		entity.setZitiContactUser(nullIfBlank(stringVal(params.get("ziti_contact_user"))));
		entity.setZitiContactMobile(nullIfBlank(stringVal(params.get("ziti_contact_mobile"))));
		entity.setZitiPics(nullIfBlank(stringVal(params.get("ziti_pics"))));
		entity.setIsDefault(parseIsDefault(params.get("is_default")));
		entity.setZitiStatus(parseZitiStatus(params.get("ziti_status")));

		int now = (int) Instant.now().getEpochSecond();
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);

		communityChiefZitiMapper.insert(entity);
		Long id = entity.getZitiId();
		CommunityChiefZiti row =
				id != null ? communityChiefZitiMapper.selectById(id) : entity;
		return entityToSnakeCaseMap(row != null ? row : entity, false);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateChiefZiti(long zitiId, long chiefId, Map<String, Object> params) {
		CommunityChiefZiti existing = communityChiefZitiMapper.selectById(zitiId);
		if (existing == null) {
			throw new ResourceException("无效的自提点");
		}
		Long rowChiefId = existing.getChiefId();
		if (rowChiefId == null || !rowChiefId.equals(chiefId)) {
			throw new ForbiddenException("只能修改自己的自提点");
		}

		LambdaUpdateWrapper<CommunityChiefZiti> w =
				Wrappers.<CommunityChiefZiti>lambdaUpdate()
						.eq(CommunityChiefZiti::getZitiId, zitiId)
						.eq(CommunityChiefZiti::getChiefId, chiefId);

		boolean anyBusiness = false;
		if (isset(params, "ziti_name")) {
			w.set(CommunityChiefZiti::getZitiName, stringVal(params.get("ziti_name")));
			anyBusiness = true;
		}
		if (isset(params, "province")) {
			w.set(CommunityChiefZiti::getProvince, stringVal(params.get("province")));
			anyBusiness = true;
		}
		if (isset(params, "city")) {
			w.set(CommunityChiefZiti::getCity, stringVal(params.get("city")));
			anyBusiness = true;
		}
		if (isset(params, "area")) {
			w.set(CommunityChiefZiti::getArea, stringVal(params.get("area")));
			anyBusiness = true;
		}
		if (isset(params, "regions_id")) {
			w.set(CommunityChiefZiti::getRegionsId, toJsonColumn(params.get("regions_id")));
			anyBusiness = true;
		}
		if (isset(params, "regions")) {
			w.set(CommunityChiefZiti::getRegions, toJsonColumn(params.get("regions")));
			anyBusiness = true;
		}
		if (isset(params, "address")) {
			w.set(CommunityChiefZiti::getAddress, stringVal(params.get("address")));
			anyBusiness = true;
		}
		if (isset(params, "lat")) {
			w.set(CommunityChiefZiti::getLat, objectToStringOrNull(params.get("lat")));
			anyBusiness = true;
		}
		if (isset(params, "lng")) {
			w.set(CommunityChiefZiti::getLng, objectToStringOrNull(params.get("lng")));
			anyBusiness = true;
		}
		if (isset(params, "ziti_contact_user")) {
			w.set(CommunityChiefZiti::getZitiContactUser, stringVal(params.get("ziti_contact_user")));
			anyBusiness = true;
		}
		if (isset(params, "ziti_contact_mobile")) {
			w.set(CommunityChiefZiti::getZitiContactMobile, stringVal(params.get("ziti_contact_mobile")));
			anyBusiness = true;
		}
		if (isset(params, "ziti_pics")) {
			w.set(CommunityChiefZiti::getZitiPics, stringVal(params.get("ziti_pics")));
			anyBusiness = true;
		}
		if (isset(params, "is_default")) {
			w.set(CommunityChiefZiti::getIsDefault, parseIsDefault(params.get("is_default")));
			anyBusiness = true;
		}
		if (isset(params, "ziti_status")) {
			w.set(CommunityChiefZiti::getZitiStatus, parseZitiStatus(params.get("ziti_status")));
			anyBusiness = true;
		}

		if (anyBusiness) {
			int now = (int) Instant.now().getEpochSecond();
			w.set(CommunityChiefZiti::getUpdatedAt, now);
			int rows = communityChiefZitiMapper.update(null, w);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		}

		CommunityChiefZiti row = communityChiefZitiMapper.selectById(zitiId);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return entityToSnakeCaseMap(row, true);
	}

	private static boolean isset(Map<String, Object> params, String key) {
		return params.containsKey(key) && params.get(key) != null;
	}

	private String toJsonColumn(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() ? null : t;
		}
		if (raw instanceof Collection<?> || raw instanceof Map<?, ?> || raw.getClass().isArray()) {
			try {
				return objectMapper.writeValueAsString(raw);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("参数类型错误");
			}
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("参数类型错误");
		}
	}

	private static Boolean parseIsDefault(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = raw.toString().trim().toLowerCase();
		if ("1".equals(s) || "true".equals(s)) {
			return true;
		}
		if ("0".equals(s) || "false".equals(s)) {
			return false;
		}
		return false;
	}

	private static String parseZitiStatus(Object raw) {
		String s = stringVal(raw);
		if (!StringUtils.hasText(s)) {
			return "success";
		}
		return s;
	}

	private static String objectToStringOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.toString().trim();
		return s.isEmpty() ? null : s;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static String nullIfBlank(String s) {
		return StringUtils.hasText(s) ? s : null;
	}

	private Map<String, Object> entityToSnakeCaseMap(
			CommunityChiefZiti z, boolean regionsJsonEmptyAsArray) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		Long zitiId = z.getZitiId();
		m.put("ziti_id", zitiId != null ? String.valueOf(zitiId) : null);
		Long chiefId = z.getChiefId();
		m.put("chief_id", chiefId != null ? String.valueOf(chiefId) : null);
		m.put("ziti_name", z.getZitiName());
		m.put("province", z.getProvince());
		m.put("city", z.getCity());
		m.put("area", z.getArea());
		m.put(
				"regions_id",
				jsonStoredColumnToResponseValue(z.getRegionsId(), regionsJsonEmptyAsArray));
		m.put("regions", jsonStoredColumnToResponseValue(z.getRegions(), regionsJsonEmptyAsArray));
		m.put("address", z.getAddress());
		m.put("lat", z.getLat());
		m.put("lng", z.getLng());
		m.put("ziti_contact_user", z.getZitiContactUser());
		m.put("ziti_contact_mobile", z.getZitiContactMobile());
		m.put("ziti_pics", z.getZitiPics());
		m.put("is_default", Boolean.TRUE.equals(z.getIsDefault()) ? 1 : 0);
		m.put("ziti_status", z.getZitiStatus());
		m.put("created_at", z.getCreatedAt());
		m.put("updated_at", z.getUpdatedAt());
		return m;
	}

	private Object jsonStoredColumnToResponseValue(String column, boolean emptyAsArray) {
		if (!StringUtils.hasText(column)) {
			return emptyAsArray ? new ArrayList<>() : null;
		}
		try {
			return objectMapper.readValue(column.trim(), Object.class);
		} catch (JsonProcessingException e) {
			return emptyAsArray ? new ArrayList<>() : null;
		}
	}
}
