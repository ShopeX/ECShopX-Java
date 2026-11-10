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

package cn.shopex.ecshopx.goods.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.search.synonym.SearchSynonymService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@RestController("goodsAdminV1SearchSynonym")
@RequestMapping("/api/v1/goods")
public class SearchSynonymController {

	private final SearchSynonymService searchSynonymService;

	public SearchSynonymController(SearchSynonymService searchSynonymService) {
		this.searchSynonymService = searchSynonymService;
	}

	@GetMapping(value = "/search/synonyms", name = "商品搜索同义词列表")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> list(HttpServletRequest request) {
		long companyId = readCompanyId(request);
		List<Map<String, Object>> data = new ArrayList<>();
		for (SearchSynonymService.SynonymGroupView group : searchSynonymService.list(companyId)) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", group.id());
			row.put("group_key", group.groupKey());
			row.put("words", group.words());
			data.add(row);
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PutMapping(value = "/search/synonyms", name = "保存商品搜索同义词组")
	public ResponseEntity<ApiResult<Map<String, Object>>> replace(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		long companyId = readCompanyId(request);
		Map<String, Object> merged = body != null ? body : Map.of();
		Object groupKeyObj = merged.get("group_key");
		if (groupKeyObj == null) {
			groupKeyObj = merged.get("groupKey");
		}
		if (!(groupKeyObj instanceof String groupKey) || !StringUtils.hasText(groupKey)) {
			throw new BadRequestException("group_key 必填");
		}
		List<String> words = readWordsList(merged.get("words"));
		SearchSynonymService.SynonymGroupView saved =
				searchSynonymService.replaceGroup(companyId, groupKey, words);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("id", saved.id());
		data.put("group_key", saved.groupKey());
		data.put("words", saved.words());
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DeleteMapping(value = "/search/synonyms/{id}", name = "删除商品搜索同义词组")
	public ResponseEntity<ApiResult<Map<String, Object>>> delete(
			HttpServletRequest request, @PathVariable("id") String idStr) {
		long companyId = readCompanyId(request);
		long id = parsePositiveId(idStr);
		searchSynonymService.deleteGroup(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static long readCompanyId(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		return readRequiredLong(ud, "company_id");
	}

	private static long readRequiredLong(Map<?, ?> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new BadRequestException(key + " 缺失或无效");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException(key + " 缺失或无效");
		}
	}

	private static long parsePositiveId(String idStr) {
		if (!StringUtils.hasText(idStr)) {
			throw new BadRequestException("id 无效");
		}
		try {
			long id = Long.parseLong(idStr.trim());
			if (id <= 0L) {
				throw new BadRequestException("id 无效");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("id 无效");
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> readWordsList(Object raw) {
		if (raw == null) {
			throw new BadRequestException("words 必填");
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object item : list) {
				if (item != null) {
					out.add(item.toString());
				}
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s)) {
			return List.of(s);
		}
		throw new BadRequestException("words 格式无效");
	}
}
