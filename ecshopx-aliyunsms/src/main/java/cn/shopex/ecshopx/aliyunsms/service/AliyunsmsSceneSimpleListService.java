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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.aliyunsms.config.AliyunsmsSmsSceneWhitelistProperties;
import cn.shopex.ecshopx.aliyunsms.domain.Scene;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.superadmin.service.ShopMenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsSceneSimpleListService {

	private final SceneMapper sceneMapper;
	private final ShopMenuService shopMenuService;
	private final AliyunsmsSmsSceneWhitelistProperties whitelistProperties;

	public AliyunsmsSceneSimpleListService(
			SceneMapper sceneMapper,
			ShopMenuService shopMenuService,
			AliyunsmsSmsSceneWhitelistProperties whitelistProperties) {
		this.sceneMapper = sceneMapper;
		this.shopMenuService = shopMenuService;
		this.whitelistProperties = whitelistProperties;
	}

	public Map<String, Object> getSimpleList(long companyId, boolean templateTypeKeyPresent, String templateTypeRaw) {
		LambdaQueryWrapper<Scene> countWrapper = new LambdaQueryWrapper<>();
		applySimpleListConditions(countWrapper, companyId, templateTypeKeyPresent, templateTypeRaw);
		long total = sceneMapper.selectCount(countWrapper);

		List<Scene> rows = List.of();
		if (total > 0) {
			LambdaQueryWrapper<Scene> listWrapper = new LambdaQueryWrapper<>();
			applySimpleListConditions(listWrapper, companyId, templateTypeKeyPresent, templateTypeRaw);
			listWrapper.select(Scene::getId, Scene::getSceneName, Scene::getTemplateType);
			listWrapper.orderByAsc(Scene::getId);
			rows = sceneMapper.selectList(listWrapper);
		}

		List<Map<String, Object>> built = new ArrayList<>(rows.size());
		for (Scene scene : rows) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", scene.getId());
			row.put("scene_name", scene.getSceneName());
			row.put("template_type", scene.getTemplateType());
			built.add(row);
		}

		List<String> sceneWhitelist = resolveWhitelistForCompany(companyId);
		List<Map<String, Object>> filtered = built;
		if (sceneWhitelist != null && !sceneWhitelist.isEmpty()) {
			filtered = new ArrayList<>();
			for (Map<String, Object> row : built) {
				Object nameObj = row.get("scene_name");
				if (matchesWhitelistLoosely(nameObj, sceneWhitelist)) {
					filtered.add(row);
				}
			}
			if (filtered.size() < built.size()) {
				throw new ResourceException("场景数据不完整");
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", filtered);
		return result;
	}

	/** Only eq / conditional predicates — no {@code select(...)} or {@code orderBy*}; safe for {@code selectCount}. */
	private static void applySimpleListConditions(
			LambdaQueryWrapper<Scene> w,
			long companyId,
			boolean templateTypeKeyPresent,
			String templateTypeRaw) {
		w.eq(Scene::getCompanyId, companyId);
		if (templateTypeKeyPresent) {
			w.eq(Scene::getTemplateType, templateTypeRaw);
		}
	}

	private List<String> resolveWhitelistForCompany(long companyId) {
		String key = shopMenuService.resolveProductModelKeyForCompany(companyId);
		return whitelistProperties.resolveSceneNames(key);
	}

	/** Name matches whitelist entry if trim-equal as text, or both parse to the same integer. */
	private static boolean matchesWhitelistLoosely(Object sceneNameObj, List<String> sceneList) {
		for (String entry : sceneList) {
			if (looseEqual(sceneNameObj, entry)) {
				return true;
			}
		}
		return false;
	}

	private static boolean looseEqual(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			String sa = a == null ? "" : String.valueOf(a).trim();
			String sb = b == null ? "" : String.valueOf(b).trim();
			return sa.isEmpty() && sb.isEmpty();
		}
		String sa = String.valueOf(a).trim();
		String sb = String.valueOf(b).trim();
		if (sa.equals(sb)) {
			return true;
		}
		Long la = tryParseLong(sa);
		Long lb = tryParseLong(sb);
		if (la != null && lb != null) {
			return la.equals(lb);
		}
		return false;
	}

	private static Long tryParseLong(String s) {
		if (s == null || s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
