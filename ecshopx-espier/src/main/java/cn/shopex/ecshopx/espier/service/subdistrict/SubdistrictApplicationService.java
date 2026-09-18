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

package cn.shopex.ecshopx.espier.service.subdistrict;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.espier.domain.Subdistrict;
import cn.shopex.ecshopx.espier.mapper.SubdistrictMapper;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SubdistrictApplicationService {

	private static final ObjectMapper JSON = new ObjectMapper();

	private static final Pattern DECIMAL_LOOSE =
			Pattern.compile("^-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?$");

	private static final Pattern INTEGER_DIGITS_ONLY = Pattern.compile("^[+-]?[0-9]+$");

	private final SubdistrictMapper subdistrictMapper;
	private final StringRedisTemplate espierStringRedisTemplate;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;

	public SubdistrictApplicationService(
			SubdistrictMapper subdistrictMapper,
			@Qualifier("espierStringRedisTemplate") StringRedisTemplate espierStringRedisTemplate,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper) {
		this.subdistrictMapper = subdistrictMapper;
		this.espierStringRedisTemplate = espierStringRedisTemplate;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> get(HttpServletRequest request, String label) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		Map<String, Object> user =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		String operatorType =
				EspierAdminJwtControllerSupport.optionalTrimmedString(user != null ? user.get("operator_type") : null);
		boolean isDistributor = Objects.equals(operatorType, "distributor");
		String raw = isDistributor ? request.getParameter("distributor_id") : null;

		String field =
				isDistributor ? (companyId + "_" + redisFieldSuffixForCacheKey(raw)) : (companyId + "_" + "0");

		String cached = espierStringRedisTemplate.<String, String>opsForHash().get("subdistrict", field);
		List<Map<String, Object>> sub = null;
		if (cached != null && !cached.isEmpty()) {
			try {
				sub = JSON.readValue(cached, new TypeReference<List<Map<String, Object>>>() {});
			} catch (Exception e) {
				sub = null;
			}
		}

		boolean applyDistributorContains = isDistributor && appliesDistributorContainsFilter(raw);
		String containsTokenOrNull =
				applyDistributorContains ? containsLikeTokenFromRaw(raw) : null;

		if (sub == null) {
			sub = buildSubdistrictTreeFromDb(companyId, applyDistributorContains, containsTokenOrNull);
			try {
				espierStringRedisTemplate
						.<String, String>opsForHash()
						.put("subdistrict", field, JSON.writeValueAsString(sub));
			} catch (Exception ignored) {
				// cache write is best-effort
			}
		}

		if (label != null && !label.isEmpty()) {
			sub = filterSubdistrictByLabel(sub, label);
		}

		List<Object> mergedIds = collectSubdistrictDistributorIdElements(sub);
		List<Long> idQuery = normalizeDistributorIdsToLongList(mergedIds);
		Map<Long, String> nameMap = new LinkedHashMap<>();
		if (!idQuery.isEmpty()) {
			List<Map<String, Object>> rows =
					distributionDistributorSelfReadMapper.listDistributorNamesByCompanyAndIds(companyId, idQuery);
			for (Map<String, Object> row : rows) {
				Long did = longFromDbCell(row.get("distributor_id"));
				if (did != null) {
					Object nameObj = row.get("name");
					nameMap.put(did, nameObj == null ? null : String.valueOf(nameObj));
				}
			}
		}

		if (!idQuery.isEmpty()) {
			for (Map<String, Object> val : sub) {
				enrichDistributorOutputRow(val, operatorType, raw, nameMap);
			}
		}

		return sub;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getInfo(HttpServletRequest request, String idPath) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		Long idLong = EspierAdminJwtControllerSupport.parseLongOrNull(idPath);
		LambdaQueryWrapper<Subdistrict> wrapper =
				new LambdaQueryWrapper<Subdistrict>().eq(Subdistrict::getCompanyId, companyId);
		if (idLong != null) {
			wrapper.eq(Subdistrict::getId, idLong);
		} else {
			wrapper.eq(Subdistrict::getId, -1L);
		}
		Subdistrict row = subdistrictMapper.selectOne(wrapper);
		if (row == null) {
			return null;
		}
		Map<String, Object> sub = new LinkedHashMap<>(entityRowToApiMap(row));
		Map<String, Object> user =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		String operatorType =
				EspierAdminJwtControllerSupport.optionalTrimmedString(user != null ? user.get("operator_type") : null);
		if (!Objects.equals(operatorType, "distributor")) {
			return sub;
		}
		String rawDistributorId = request.getParameter("distributor_id");
		if (!sub.isEmpty()) {
			if (!inArrayLooseOnRow(rawDistributorId, sub)) {
				throw new ForbiddenException("没有权限查看");
			}
		}
		sub.put("distributor_id", List.of(rawDistributorId == null ? "" : String.valueOf(rawDistributorId)));
		return sub;
	}

	@SuppressWarnings("unchecked")
	public void delete(HttpServletRequest request, String idPath) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		Map<String, Object> user =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		String operatorType =
				EspierAdminJwtControllerSupport.optionalTrimmedString(user != null ? user.get("operator_type") : null);
		Long parsedId = EspierAdminJwtControllerSupport.parseLongOrNull(idPath);
		long rowId = parsedId != null ? parsedId.longValue() : -1L;
		boolean isDistributor = Objects.equals(operatorType, "distributor");

		if (isDistributor) {
			String rawDistributorId = request.getParameter("distributor_id");
			Subdistrict row = subdistrictMapper.selectOne(new LambdaQueryWrapper<Subdistrict>()
					.eq(Subdistrict::getCompanyId, companyId)
					.eq(Subdistrict::getId, rowId));
			if (row == null) {
				throw new ResourceException("数据不存在");
			}
			List<Object> distIds = (List<Object>) entityRowToApiMap(row).get("distributor_id");
			ArrayList<Object> filtered = new ArrayList<>(distIds);
			filtered.removeIf(e -> looseEquals(e, rawDistributorId));
			if (!filtered.isEmpty()) {
				row.setDistributorId(encodeDistributorList(filtered));
				int n = subdistrictMapper.updateById(row);
				if (n == 0) {
					throw new ResourceException("未查询到更新数据");
				}
			} else {
				subdistrictMapper.delete(new LambdaQueryWrapper<Subdistrict>()
						.eq(Subdistrict::getCompanyId, companyId)
						.eq(Subdistrict::getId, rowId));
				subdistrictMapper.delete(new LambdaQueryWrapper<Subdistrict>()
						.eq(Subdistrict::getCompanyId, companyId)
						.eq(Subdistrict::getParentId, rowId));
			}
		} else {
			subdistrictMapper.delete(new LambdaQueryWrapper<Subdistrict>()
					.eq(Subdistrict::getCompanyId, companyId)
					.eq(Subdistrict::getId, rowId));
			subdistrictMapper.delete(new LambdaQueryWrapper<Subdistrict>()
					.eq(Subdistrict::getCompanyId, companyId)
					.eq(Subdistrict::getParentId, rowId));
		}

		espierStringRedisTemplate.delete("subdistrict");
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> save(HttpServletRequest request, Map<String, Object> mergedOnlyParams) {
		long companyId = EspierAdminJwtControllerSupport.extractCompanyId(request);
		Map<String, Object> user =
				(Map<String, Object>) request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		String operatorType =
				user != null ? EspierAdminJwtControllerSupport.optionalTrimmedString(user.get("operator_type")) : null;

		Object parentRaw = mergedOnlyParams.get("parent_id");
		long parentId;
		if (parentRaw == null) {
			throw new ResourceException("上级ID必填");
		} else if (parentRaw instanceof Integer i) {
			long v = i.longValue();
			if (v < 0L) {
				throw new ResourceException("上级ID必填");
			}
			parentId = v;
		} else if (parentRaw instanceof Long l) {
			if (l < 0L) {
				throw new ResourceException("上级ID必填");
			}
			parentId = l;
		} else {
			String s = String.valueOf(parentRaw).trim();
			if (!s.matches("[0-9]+")) {
				throw new ResourceException("上级ID必填");
			}
			try {
				long v = Long.parseLong(s);
				if (v < 0L) {
					throw new ResourceException("上级ID必填");
				}
				parentId = v;
			} catch (NumberFormatException e) {
				throw new ResourceException("上级ID必填");
			}
		}

		Object labelRaw = mergedOnlyParams.get("label");
		if (labelRaw == null || String.valueOf(labelRaw).trim().isEmpty()) {
			throw new ResourceException("街道/社区名称必填");
		}

		if (mergedOnlyParams.containsKey("distributor_id")) {
			Object dv = mergedOnlyParams.get("distributor_id");
			if (dv != null && !(dv instanceof Collection<?>) && !dv.getClass().isArray()) {
				mergedOnlyParams.put("distributor_id", new ArrayList<>(List.of(dv)));
			}
		}

		Long id = EspierAdminJwtControllerSupport.parseLongOrNull(mergedOnlyParams.get("id"));
		boolean isNew = id == null || id == 0L;

		if (parentId > 0L) {
			Subdistrict parent = subdistrictMapper.selectOne(new LambdaQueryWrapper<Subdistrict>()
					.eq(Subdistrict::getId, parentId)
					.eq(Subdistrict::getCompanyId, companyId));
			if (parent == null) {
				throw new ResourceException("上级ID错误");
			}
			if (!"distributor".equals(operatorType) && isNew) {
				@SuppressWarnings("unchecked")
				List<Object> inherited =
						(List<Object>) entityRowToApiMap(parent).get("distributor_id");
				mergedOnlyParams.put("distributor_id", new ArrayList<>(inherited));
			}
		}

		if (!"distributor".equals(operatorType)) {
			if (mergedOnlyParams.containsKey("distributor_id") && mergedOnlyParams.get("distributor_id") != null) {
				List<Object> list = readDistributorIdListFromMergedParams(mergedOnlyParams);
				boolean hasGlobalZero = list.stream()
						.anyMatch(e -> "0".equals(String.valueOf(e).trim())
								|| (e instanceof Number n && n.longValue() == 0L));
				if (!hasGlobalZero) {
					ArrayList<Object> withZero = new ArrayList<>();
					withZero.add(Integer.valueOf(0));
					withZero.addAll(list);
					mergedOnlyParams.put("distributor_id", withZero);
				} else {
					mergedOnlyParams.put("distributor_id", list);
				}
			} else {
				mergedOnlyParams.put("distributor_id", List.of(0));
			}
		}

		Map<String, Object> result;
		if (id != null && id > 0L) {
			Subdistrict current = subdistrictMapper.selectOne(new LambdaQueryWrapper<Subdistrict>()
					.eq(Subdistrict::getId, id)
					.eq(Subdistrict::getCompanyId, companyId));
			if (current == null) {
				throw new ResourceException("ID错误");
			}
			@SuppressWarnings("unchecked")
			List<Object> oldDist = (List<Object>) entityRowToApiMap(current).get("distributor_id");
			List<Object> oldDistributorIds = new ArrayList<>(oldDist);
			applyParamsToEntity(current, mergedOnlyParams, companyId);
			int rows = subdistrictMapper.updateById(current);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			Subdistrict refreshed = subdistrictMapper.selectById(id);
			if (refreshed == null) {
				throw new ResourceException("未查询到更新数据");
			}
			result = entityRowToApiMap(refreshed);
			if (!"distributor".equals(operatorType) && parentId == 0L) {
				List<Object> newDistributorIds =
						orderedArrayDiff(readDistributorIdListFromMergedParams(mergedOnlyParams), oldDistributorIds);
				if (!newDistributorIds.isEmpty()) {
					List<Subdistrict> children = subdistrictMapper.selectList(new LambdaQueryWrapper<Subdistrict>()
							.eq(Subdistrict::getParentId, id)
							.like(Subdistrict::getDistributorId, ",0,"));
					for (Subdistrict child : children) {
						List<Object> mergedForChild = mergeChildRowDistributorIds(child, newDistributorIds);
						updateRowDistributorEncoded(child.getId(), mergedForChild);
					}
				}
			}
		} else {
			Subdistrict entity = new Subdistrict();
			applyParamsToEntity(entity, mergedOnlyParams, companyId);
			subdistrictMapper.insert(entity);
			if (entity.getId() == null) {
				throw new ResourceException("未查询到更新数据");
			}
			Subdistrict inserted = subdistrictMapper.selectById(entity.getId());
			if (inserted == null) {
				throw new ResourceException("未查询到更新数据");
			}
			result = entityRowToApiMap(inserted);
		}

		espierStringRedisTemplate.delete("subdistrict");
		return result;
	}

	private Map<String, Object> entityRowToApiMap(Subdistrict e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("parent_id", e.getParentId());
		m.put("label", e.getLabel());
		m.put("distributor_id", splitCommaWrappedToList(e.getDistributorId()));
		m.put("regions_id", jsonDecodeRegionsFromColumn(e.getRegionsId()));
		m.put("province", e.getProvince());
		m.put("city", e.getCity());
		m.put("area", e.getArea());
		return m;
	}

	private List<Object> splitCommaWrappedToList(String raw) {
		String s = (raw == null || raw.isEmpty()) ? "," : raw;
		String core = trimLeadingTrailingChars(s, ',');
		if (core.isEmpty()) {
			ArrayList<Object> singleEmpty = new ArrayList<>(1);
			singleEmpty.add("");
			return singleEmpty;
		}
		return new ArrayList<>(Arrays.asList(core.split(",", -1)));
	}

	private static String trimLeadingTrailingChars(String s, char ch) {
		int start = 0;
		int end = s.length();
		while (start < end && s.charAt(start) == ch) {
			start++;
		}
		while (end > start && s.charAt(end - 1) == ch) {
			end--;
		}
		return s.substring(start, end);
	}

	private Object jsonDecodeRegionsFromColumn(String regionsJson) {
		if (regionsJson == null || regionsJson.isBlank()) {
			return null;
		}
		try {
			return JSON.readValue(regionsJson, new TypeReference<Object>() {});
		} catch (Exception e) {
			return null;
		}
	}

	private String encodeDistributorList(List<?> ids) {
		if (ids == null || ids.isEmpty()) {
			return ",";
		}
		StringBuilder sb = new StringBuilder(",");
		boolean first = true;
		for (Object id : ids) {
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append(normalizeIdToken(id));
		}
		sb.append(',');
		return sb.toString();
	}

	private void applyParamsToEntity(Subdistrict target, Map<String, Object> params, long companyId) {
		target.setCompanyId(companyId);
		Object labelObj = params.get("label");
		target.setLabel(labelObj == null ? null : String.valueOf(labelObj));
		Object pid = params.get("parent_id");
		if (pid instanceof Number n) {
			target.setParentId(n.longValue());
		} else if (pid != null) {
			target.setParentId(Long.parseLong(String.valueOf(pid).trim()));
		}
		List<Object> distList = readDistributorIdListFromMergedParams(params);
		target.setDistributorId(encodeDistributorList(distList));

		Object prov = params.get("province");
		if (prov != null) {
			String ps = String.valueOf(prov).trim();
			if (!ps.isEmpty()) {
				target.setProvince(ps);
			}
		}
		Object city = params.get("city");
		if (city != null) {
			String cs = String.valueOf(city).trim();
			if (!cs.isEmpty()) {
				target.setCity(cs);
			}
		}
		Object area = params.get("area");
		if (area != null) {
			String as = String.valueOf(area).trim();
			if (!as.isEmpty()) {
				target.setArea(as);
			}
		}

		if (!params.containsKey("regions_id")) {
			return;
		}
		Object regions = params.get("regions_id");
		if (regions == null) {
			target.setRegionsId("[]");
			return;
		}
		if (regions instanceof String str) {
			if (str.trim().isEmpty()) {
				target.setRegionsId("[]");
			} else {
				target.setRegionsId(str);
			}
			return;
		}
		if (regions instanceof Collection<?> c) {
			if (c.isEmpty()) {
				target.setRegionsId("[]");
			} else {
				try {
					target.setRegionsId(JSON.writeValueAsString(c));
				} catch (Exception e) {
					target.setRegionsId("[]");
				}
			}
			return;
		}
		if (regions.getClass().isArray()) {
			final List<Object> elems;
			if (regions instanceof Object[] arr) {
				elems = new ArrayList<>(Arrays.asList(arr));
			} else if (regions instanceof int[] arr) {
				elems = new ArrayList<>(arr.length);
				for (int x : arr) {
					elems.add(x);
				}
			} else if (regions instanceof long[] arr) {
				elems = new ArrayList<>(arr.length);
				for (long x : arr) {
					elems.add(x);
				}
			} else if (regions instanceof short[] arr) {
				elems = new ArrayList<>(arr.length);
				for (short x : arr) {
					elems.add((int) x);
				}
			} else if (regions instanceof byte[] arr) {
				elems = new ArrayList<>(arr.length);
				for (byte x : arr) {
					elems.add((int) x);
				}
			} else if (regions instanceof double[] arr) {
				elems = new ArrayList<>(arr.length);
				for (double x : arr) {
					elems.add(x);
				}
			} else if (regions instanceof float[] arr) {
				elems = new ArrayList<>(arr.length);
				for (float x : arr) {
					elems.add(x);
				}
			} else if (regions instanceof boolean[] arr) {
				elems = new ArrayList<>(arr.length);
				for (boolean x : arr) {
					elems.add(x);
				}
			} else if (regions instanceof char[] arr) {
				elems = new ArrayList<>(arr.length);
				for (char x : arr) {
					elems.add(String.valueOf(x));
				}
			} else {
				elems = new ArrayList<>();
			}
			if (elems.isEmpty()) {
				target.setRegionsId("[]");
			} else {
				try {
					target.setRegionsId(JSON.writeValueAsString(elems));
				} catch (Exception e) {
					target.setRegionsId("[]");
				}
			}
			return;
		}
		if (regions instanceof Map<?, ?> map) {
			if (map.isEmpty()) {
				target.setRegionsId("[]");
			} else {
				try {
					target.setRegionsId(JSON.writeValueAsString(map));
				} catch (Exception e) {
					target.setRegionsId("[]");
				}
			}
			return;
		}
		target.setRegionsId(String.valueOf(regions));
	}

	private List<Object> orderedArrayDiff(List<?> a, List<?> b) {
		Set<String> bNorm = new HashSet<>();
		for (Object o : b) {
			bNorm.add(normalizeIdToken(o));
		}
		List<Object> out = new ArrayList<>();
		for (Object o : a) {
			if (!bNorm.contains(normalizeIdToken(o))) {
				out.add(o);
			}
		}
		return out;
	}

	private String normalizeIdToken(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof Number n) {
			return Long.toString(n.longValue());
		}
		return String.valueOf(o).trim();
	}

	private List<Object> readDistributorIdListFromMergedParams(Map<String, Object> mergedOnlyParams) {
		if (!mergedOnlyParams.containsKey("distributor_id")) {
			return new ArrayList<>();
		}
		Object v = mergedOnlyParams.get("distributor_id");
		if (v == null) {
			return new ArrayList<>();
		}
		if (v instanceof Collection<?> c) {
			return new ArrayList<>(c);
		}
		if (!v.getClass().isArray()) {
			return new ArrayList<>();
		}
		if (v instanceof Object[] arr) {
			return new ArrayList<>(Arrays.asList(arr));
		}
		if (v instanceof int[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (int x : arr) {
				out.add(x);
			}
			return out;
		}
		if (v instanceof long[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (long x : arr) {
				out.add(x);
			}
			return out;
		}
		if (v instanceof short[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (short x : arr) {
				out.add((int) x);
			}
			return out;
		}
		if (v instanceof byte[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (byte x : arr) {
				out.add((int) x);
			}
			return out;
		}
		if (v instanceof double[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (double x : arr) {
				out.add(x);
			}
			return out;
		}
		if (v instanceof float[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (float x : arr) {
				out.add(x);
			}
			return out;
		}
		if (v instanceof boolean[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (boolean x : arr) {
				out.add(x);
			}
			return out;
		}
		if (v instanceof char[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (char x : arr) {
				out.add(String.valueOf(x));
			}
			return out;
		}
		return new ArrayList<>();
	}

	private List<Object> mergeChildRowDistributorIds(Subdistrict row, List<Object> newDistributorIds) {
		@SuppressWarnings("unchecked")
		List<Object> existing = (List<Object>) entityRowToApiMap(row).get("distributor_id");
		List<Object> base = new ArrayList<>(existing);
		Set<String> seen = new HashSet<>();
		for (Object o : base) {
			seen.add(normalizeIdToken(o));
		}
		for (Object n : newDistributorIds) {
			String norm = normalizeIdToken(n);
			if (!seen.contains(norm)) {
				base.add(n);
				seen.add(norm);
			}
		}
		return base;
	}

	private void updateRowDistributorEncoded(Long rowId, List<Object> mergedTokens) {
		Subdistrict row = subdistrictMapper.selectById(rowId);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		row.setDistributorId(encodeDistributorList(mergedTokens));
		if (subdistrictMapper.updateById(row) == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	private static String redisFieldSuffixForCacheKey(String rawForDistributorBranch) {
		return rawForDistributorBranch == null ? "" : rawForDistributorBranch;
	}

	private static boolean appliesDistributorContainsFilter(String raw) {
		if (raw == null) {
			return false;
		}
		return leadingDigitsAsLong(raw) >= 0L;
	}

	private static long leadingDigitsAsLong(String raw) {
		if (raw == null) {
			return 0L;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0L;
		}
		int i = 0;
		boolean neg = false;
		if (s.charAt(0) == '-') {
			neg = true;
			i++;
		} else if (s.charAt(0) == '+') {
			i++;
		}
		long acc = 0L;
		boolean any = false;
		while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
			any = true;
			acc = acc * 10L + (s.charAt(i) - '0');
			i++;
		}
		if (!any) {
			return 0L;
		}
		return neg ? -acc : acc;
	}

	private static String containsLikeTokenFromRaw(String raw) {
		if (raw == null) {
			return "0";
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return "0";
		}
		if (t.charAt(0) == '+') {
			t = t.substring(1).trim();
		}
		if (!t.matches("[0-9]+")) {
			return "0";
		}
		try {
			long v = Long.parseLong(t);
			if (v < 0L) {
				return "0";
			}
			return Long.toString(v);
		} catch (NumberFormatException e) {
			return "0";
		}
	}

	private List<Map<String, Object>> buildSubdistrictTreeFromDb(
			long companyId, boolean applyDistributorContains, String containsTokenOrNull) {
		LambdaQueryWrapper<Subdistrict> top = new LambdaQueryWrapper<Subdistrict>()
				.eq(Subdistrict::getCompanyId, companyId)
				.eq(Subdistrict::getParentId, 0L);
		if (applyDistributorContains && containsTokenOrNull != null) {
			top.like(Subdistrict::getDistributorId, "," + containsTokenOrNull + ",");
		}
		top.orderByAsc(Subdistrict::getLabel);
		List<Subdistrict> tops = subdistrictMapper.selectList(top);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Subdistrict row : tops) {
			Map<String, Object> map = entityRowToApiMap(row);
			LambdaQueryWrapper<Subdistrict> ch = new LambdaQueryWrapper<Subdistrict>()
					.eq(Subdistrict::getCompanyId, companyId)
					.eq(Subdistrict::getParentId, row.getId());
			if (applyDistributorContains && containsTokenOrNull != null) {
				ch.like(Subdistrict::getDistributorId, "," + containsTokenOrNull + ",");
			}
			ch.orderByAsc(Subdistrict::getLabel);
			List<Subdistrict> cs = subdistrictMapper.selectList(ch);
			List<Map<String, Object>> childMaps = new ArrayList<>();
			for (Subdistrict c : cs) {
				childMaps.add(entityRowToApiMap(c));
			}
			map.put("children", childMaps);
			out.add(map);
		}
		return out;
	}

	private static List<Map<String, Object>> filterSubdistrictByLabel(List<Map<String, Object>> sub, String label) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> item : sub) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> children = (List<Map<String, Object>>) item.get("children");
			if (children == null) {
				children = List.of();
			}
			List<Map<String, Object>> newChildren = new ArrayList<>();
			for (Map<String, Object> child : children) {
				Object cl = child.get("label");
				if (cl != null && String.valueOf(cl).contains(label)) {
					newChildren.add(child);
				}
			}
			Object il = item.get("label");
			boolean keep = !newChildren.isEmpty()
					|| (il != null && String.valueOf(il).contains(label));
			if (keep) {
				item.put("children", new ArrayList<>(newChildren));
				out.add(item);
			}
		}
		return new ArrayList<>(out);
	}

	private static List<Object> collectSubdistrictDistributorIdElements(List<Map<String, Object>> subdistrict) {
		ArrayList<Object> acc = new ArrayList<>();
		for (Map<String, Object> row : subdistrict) {
			@SuppressWarnings("unchecked")
			List<Object> dids = (List<Object>) row.get("distributor_id");
			if (dids != null) {
				acc.addAll(dids);
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> children = (List<Map<String, Object>>) row.get("children");
			if (children != null) {
				for (Map<String, Object> child : children) {
					@SuppressWarnings("unchecked")
					List<Object> cdids = (List<Object>) child.get("distributor_id");
					if (cdids != null) {
						acc.addAll(cdids);
					}
				}
			}
		}
		return acc;
	}

	private static List<Long> normalizeDistributorIdsToLongList(List<Object> merged) {
		List<Long> out = new ArrayList<>();
		for (Object o : merged) {
			if (o instanceof Number n) {
				out.add(n.longValue());
			} else {
				String s = String.valueOf(o).trim();
				if (s.matches("[0-9]+")) {
					try {
						out.add(Long.parseLong(s));
					} catch (NumberFormatException ignored) {
						// skip
					}
				}
			}
		}
		return out;
	}

	private static Long longFromDbCell(Object idObj) {
		if (idObj == null) {
			return null;
		}
		if (idObj instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(idObj).trim();
		if (!s.matches("[0-9]+")) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void enrichDistributorOutputRow(
			Map<String, Object> val, String operatorType, String raw, Map<Long, String> nameMap) {
		if (!Objects.equals(operatorType, "distributor")) {
			applyNonDistributorNameFields(val, nameMap);
		} else {
			long distributorIdScalar = distributorIdScalarForDistributorResponse(raw);
			List<Object> singleName = new ArrayList<>(1);
			singleName.add(nameMap.get(distributorIdScalar));
			val.put("distributor", singleName);
			val.put("distributor_id", new ArrayList<>(List.of(String.valueOf(distributorIdScalar))));
		}
	}

	private void applyNonDistributorNameFields(Map<String, Object> val, Map<Long, String> nameMap) {
		@SuppressWarnings("unchecked")
		List<Object> distIds = (List<Object>) val.get("distributor_id");
		if (distIds == null) {
			distIds = List.of();
		}
		List<Object> newDistIds = new ArrayList<>();
		List<Object> names = new ArrayList<>();
		for (Object did : distIds) {
			long n;
			if (did instanceof Number num) {
				n = num.longValue();
			} else {
				n = leadingDigitsAsLong(String.valueOf(did));
			}
			if (n <= 0L) {
				continue;
			}
			newDistIds.add(did);
			if (nameMap.containsKey(n)) {
				names.add(nameMap.get(n));
			}
		}
		val.put("distributor_id", new ArrayList<>(newDistIds));
		val.put("distributor", names);
	}

	private static long distributorIdScalarForDistributorResponse(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		if (t.charAt(0) == '+') {
			t = t.substring(1).trim();
		}
		if (!t.matches("[0-9]+")) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean inArrayLooseOnRow(Object needle, Map<String, Object> row) {
		for (Object v : row.values()) {
			if (looseEquals(needle, v)) {
				return true;
			}
		}
		return false;
	}

	private static boolean looseEquals(Object a, Object b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a instanceof Map<?, ?> ma && b instanceof Map<?, ?> mb) {
			return looseEqualsMap(ma, mb);
		}
		List<Object> la = toOrderedList(a);
		List<Object> lb = toOrderedList(b);
		if (la != null && lb != null) {
			if (la.size() != lb.size()) {
				return false;
			}
			for (int i = 0; i < la.size(); i++) {
				if (!looseEquals(la.get(i), lb.get(i))) {
					return false;
				}
			}
			return true;
		}
		Boolean e4 = tryLooseEqualsScalarE4(a, b);
		if (e4 != null) {
			return e4;
		}
		if (a instanceof Boolean || b instanceof Boolean) {
			return boolOfLoose(a) == boolOfLoose(b);
		}
		if ((la != null) != (lb != null)) {
			return false;
		}
		return false;
	}

	private static boolean looseEqualsMap(Map<?, ?> a, Map<?, ?> b) {
		Map<String, Object> an = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : a.entrySet()) {
			an.put(normKey(e.getKey()), e.getValue());
		}
		Map<String, Object> bn = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : b.entrySet()) {
			bn.put(normKey(e.getKey()), e.getValue());
		}
		if (!an.keySet().equals(bn.keySet())) {
			return false;
		}
		for (String k : an.keySet()) {
			if (!looseEquals(an.get(k), bn.get(k))) {
				return false;
			}
		}
		return true;
	}

	private static String normKey(Object k) {
		return k == null ? "null" : String.valueOf(k);
	}

	private static List<Object> toOrderedList(Object x) {
		if (x == null) {
			return null;
		}
		if (x instanceof Object[] arr) {
			return new ArrayList<>(Arrays.asList(arr));
		}
		if (x instanceof Collection<?> c && !(x instanceof Map<?, ?>)) {
			return new ArrayList<>(c);
		}
		if (x instanceof Map<?, ?> map) {
			return toOrderedListFromIndexedMapOrNull(map);
		}
		return null;
	}

	private static List<Object> toOrderedListFromIndexedMapOrNull(Map<?, ?> m) {
		if (m.isEmpty()) {
			return new ArrayList<>();
		}
		int n = m.size();
		boolean[] seen = new boolean[n];
		for (Object k : m.keySet()) {
			String nk = normKey(k);
			if ("null".equals(nk)) {
				return null;
			}
			int idx;
			try {
				idx = Integer.parseInt(nk);
			} catch (NumberFormatException e) {
				return null;
			}
			if (idx < 0 || idx >= n) {
				return null;
			}
			if (seen[idx]) {
				return null;
			}
			seen[idx] = true;
		}
		for (int i = 0; i < n; i++) {
			if (!seen[i]) {
				return null;
			}
		}
		List<Object> out = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			Object val = null;
			for (Map.Entry<?, ?> e : m.entrySet()) {
				String nk = normKey(e.getKey());
				try {
					if (Integer.parseInt(nk) == i) {
						val = e.getValue();
						break;
					}
				} catch (NumberFormatException ignored) {
					// unreachable for validated indexed map
				}
			}
			out.add(val);
		}
		return out;
	}

	private static Boolean tryLooseEqualsScalarE4(Object a, Object b) {
		if (a instanceof Number na && b instanceof Number nb) {
			double da = na.doubleValue();
			double db = nb.doubleValue();
			if (Double.isNaN(da) || Double.isNaN(db)) {
				return false;
			}
			return da == db;
		}
		if (a instanceof Number num && isE4ScalarOperand(b)) {
			return numberAgainstStringLikeE4(num, b);
		}
		if (b instanceof Number num && isE4ScalarOperand(a)) {
			return numberAgainstStringLikeE4(num, a);
		}
		if (a instanceof String && b instanceof String) {
			return stringLooseEqualsBothStrings((String) a, (String) b);
		}
		return null;
	}

	private static boolean isE4ScalarOperand(Object x) {
		if (x == null) {
			return false;
		}
		if (x instanceof Map<?, ?> || x instanceof Collection<?>) {
			return false;
		}
		return !x.getClass().isArray();
	}

	private static boolean numberAgainstStringLikeE4(Number num, Object other) {
		String s = String.valueOf(other).trim();
		if (DECIMAL_LOOSE.matcher(s).matches()) {
			try {
				return new BigDecimal(s).compareTo(BigDecimal.valueOf(num.doubleValue())) == 0;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		if (INTEGER_DIGITS_ONLY.matcher(s).matches()) {
			try {
				return Long.parseLong(s) == num.longValue();
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	private static boolean stringLooseEqualsBothStrings(String a, String b) {
		String ta = a.trim();
		String tb = b.trim();
		if (DECIMAL_LOOSE.matcher(ta).matches() && DECIMAL_LOOSE.matcher(tb).matches()) {
			try {
				return new BigDecimal(ta).compareTo(new BigDecimal(tb)) == 0;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return ta.equals(tb);
	}

	private static boolean boolOfLoose(Object x) {
		if (x instanceof Boolean b) {
			return b.booleanValue();
		}
		if (x instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		if (x instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return false;
			}
			if ("0".equals(t) || "0.0".equalsIgnoreCase(t)) {
				return false;
			}
			if ("false".equalsIgnoreCase(t)) {
				return false;
			}
			return true;
		}
		if (x == null) {
			return false;
		}
		return false;
	}
}
