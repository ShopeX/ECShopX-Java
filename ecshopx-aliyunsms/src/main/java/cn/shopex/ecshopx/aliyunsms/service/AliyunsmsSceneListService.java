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

import cn.shopex.ecshopx.aliyunsms.domain.Scene;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsSceneListService {

	private static final String[] TEMPLATE_TYPE_LABELS = {"验证码", "短信通知", "推广短信"};

	private final SceneMapper sceneMapper;
	private final AliyunsmsSceneItemService aliyunsmsSceneItemService;

	public AliyunsmsSceneListService(SceneMapper sceneMapper, AliyunsmsSceneItemService aliyunsmsSceneItemService) {
		this.sceneMapper = sceneMapper;
		this.aliyunsmsSceneItemService = aliyunsmsSceneItemService;
	}

	public Map<String, Object> getList(long companyId, SceneListQuery query) {
		LambdaQueryWrapper<Scene> w = new LambdaQueryWrapper<>();
		w.eq(Scene::getCompanyId, companyId);
		if (query.isSceneNameFilterActive()) {
			String escaped =
					query.getSceneNameContains()
							.replace("\\", "\\\\")
							.replace("%", "\\%")
							.replace("_", "\\_");
			w.like(Scene::getSceneName, "%" + escaped + "%");
		}
		w.select(Scene::getId, Scene::getCompanyId, Scene::getSceneName, Scene::getTemplateType);

		Page<Scene> page = new Page<>(query.getPage(), query.getPageSize());
		sceneMapper.selectPage(page, w);

		long total = page.getTotal();
		List<Scene> records = page.getRecords();
		List<Long> ids = records.stream().map(Scene::getId).toList();
		Map<Long, List<Map<String, Object>>> itemsByScene =
				aliyunsmsSceneItemService.getItemRowsGroupedBySceneId(companyId, ids);

		List<Map<String, Object>> list = new ArrayList<>(records.size());
		for (Scene scene : records) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", scene.getId());
			row.put("company_id", scene.getCompanyId());
			row.put("scene_name", scene.getSceneName());
			row.put("template_type", mapTemplateTypeDisplay(scene.getTemplateType()));
			row.put(
					"itemList",
					itemsByScene.getOrDefault(scene.getId(), Collections.emptyList()));
			list.add(row);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		return result;
	}

	private static String mapTemplateTypeDisplay(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			return raw;
		}
		try {
			int idx = Integer.parseInt(trimmed);
			if (idx >= 0 && idx <= 2) {
				return TEMPLATE_TYPE_LABELS[idx];
			}
		} catch (NumberFormatException ignored) {
		}
		return raw;
	}

	public static final class SceneListQuery {
		private int page = 1;
		private int pageSize = 10;
		private boolean sceneNameFilterActive;
		private String sceneNameContains;

		public int getPage() {
			return page;
		}

		public void setPage(int page) {
			this.page = page;
		}

		public int getPageSize() {
			return pageSize;
		}

		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		public boolean isSceneNameFilterActive() {
			return sceneNameFilterActive;
		}

		public void setSceneNameFilterActive(boolean sceneNameFilterActive) {
			this.sceneNameFilterActive = sceneNameFilterActive;
		}

		public String getSceneNameContains() {
			return sceneNameContains;
		}

		public void setSceneNameContains(String sceneNameContains) {
			this.sceneNameContains = sceneNameContains;
		}
	}
}
