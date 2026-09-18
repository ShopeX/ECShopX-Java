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
import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsTemplateListService {

	private static final Set<String> MEMBER_REL_SCENES =
			Set.of(
					"member_anniversary",
					"member_birthday",
					"member_day",
					"member_upgrade",
					"member_vip_upgrade");

	private final TemplateMapper templateMapper;
	private final SceneMapper sceneMapper;

	public AliyunsmsTemplateListService(TemplateMapper templateMapper, SceneMapper sceneMapper) {
		this.templateMapper = templateMapper;
		this.sceneMapper = sceneMapper;
	}

	public Map<String, Object> getList(long companyId, TemplateListQuery q) {
		Integer sceneEq = null;
		List<Integer> sceneIn = null;
		if (q.isSceneIdFilterActive() && q.getSceneIdInt() != null && q.getSceneIdInt() > 0) {
			int sid = q.getSceneIdInt();
			Scene scene =
					sceneMapper.selectOne(
							Wrappers.<Scene>lambdaQuery()
									.eq(Scene::getCompanyId, companyId)
									.eq(Scene::getId, (long) sid));
			if (scene == null
					|| scene.getSceneTitle() == null
					|| !MEMBER_REL_SCENES.contains(scene.getSceneTitle())) {
				sceneEq = sid;
			} else {
				Scene promo =
						sceneMapper.selectOne(
								Wrappers.<Scene>lambdaQuery()
										.eq(Scene::getCompanyId, companyId)
										.eq(Scene::getTemplateType, "2")
										.last("LIMIT 1"));
				if (promo == null || promo.getId() == null) {
					sceneEq = sid;
				} else {
					sceneIn = List.of(sid, promo.getId().intValue());
				}
			}
		}

		LambdaQueryWrapper<Template> whereOnly = baseWhereWrapper(companyId, q, sceneEq, sceneIn);
		long total = templateMapper.selectCount(whereOnly);
		if (total == 0) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", List.of());
			return empty;
		}

		LambdaQueryWrapper<Template> listW = baseWhereWrapper(companyId, q, sceneEq, sceneIn);
		listW.select(
				Template::getId,
				Template::getCompanyId,
				Template::getTemplateName,
				Template::getTemplateCode,
				Template::getTemplateContent,
				Template::getSceneId,
				Template::getTemplateType,
				Template::getStatus,
				Template::getReason,
				Template::getCreated);
		listW.orderByDesc(Template::getCreated);

		List<Template> rows;
		long totalOut;
		if (q.getPage() > 0) {
			Page<Template> page = new Page<>(q.getPage(), q.getPageSize(), false);
			templateMapper.selectPage(page, listW);
			page.setTotal(total);
			rows = page.getRecords();
			totalOut = page.getTotal();
		} else {
			rows = templateMapper.selectList(listW);
			totalOut = total;
		}

		Map<Long, String> sceneNameById = Map.of();
		if (!rows.isEmpty()) {
			LinkedHashSet<Long> sceneIds = new LinkedHashSet<>();
			for (Template row : rows) {
				if (row.getSceneId() != null) {
					sceneIds.add(row.getSceneId().longValue());
				}
			}
			if (!sceneIds.isEmpty()) {
				List<Scene> scenes =
						sceneMapper.selectList(
								Wrappers.<Scene>lambdaQuery()
										.eq(Scene::getCompanyId, companyId)
										.in(Scene::getId, sceneIds)
										.select(Scene::getId, Scene::getSceneName));
				sceneNameById = new LinkedHashMap<>();
				for (Scene s : scenes) {
					if (s.getId() != null) {
						sceneNameById.put(s.getId(), s.getSceneName() != null ? s.getSceneName() : "");
					}
				}
			}
		}

		List<Map<String, Object>> list = new ArrayList<>(rows.size());
		for (Template row : rows) {
			list.add(toRow(row, sceneNameById));
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalOut);
		result.put("list", list);
		return result;
	}

	private static LambdaQueryWrapper<Template> baseWhereWrapper(
			long companyId, TemplateListQuery q, Integer sceneEq, List<Integer> sceneIn) {
		LambdaQueryWrapper<Template> w = new LambdaQueryWrapper<>();
		w.eq(Template::getCompanyId, companyId);
		if (q.isTemplateNameContainsActive()) {
			String escaped = escapeLikeContains(q.getTemplateNameContains());
			w.like(Template::getTemplateName, "%" + escaped + "%");
		}
		if (q.isStatusKeyPresent()) {
			w.eq(Template::getStatus, q.getStatusValue());
		}
		if (q.isTemplateTypeFilterActive()) {
			w.eq(Template::getTemplateType, q.getTemplateTypeRaw());
		}
		if (sceneEq != null) {
			w.eq(Template::getSceneId, sceneEq);
		} else if (sceneIn != null && !sceneIn.isEmpty()) {
			w.in(Template::getSceneId, sceneIn);
		}
		return w;
	}

	private static String escapeLikeContains(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static Map<String, Object> toRow(Template row, Map<Long, String> sceneNameById) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("template_name", row.getTemplateName());
		m.put("template_code", row.getTemplateCode());
		m.put("template_content", row.getTemplateContent());
		m.put("scene_id", row.getSceneId());
		m.put("template_type", row.getTemplateType());
		m.put("status", row.getStatus());
		m.put("reason", row.getReason());
		m.put("created", row.getCreated());
		String sn = "";
		if (row.getSceneId() != null && row.getSceneId() == 0) {
			sn = "未分配场景";
		} else if (row.getSceneId() != null) {
			sn = sceneNameById.getOrDefault(row.getSceneId().longValue(), "");
		}
		m.put("scene_name", sn);
		return m;
	}

	public static final class TemplateListQuery {
		private int page = 1;
		private int pageSize = 10;
		private boolean templateNameContainsActive;
		private String templateNameContains;
		private boolean statusKeyPresent;
		private String statusValue;
		private boolean templateTypeFilterActive;
		private String templateTypeRaw;
		private boolean sceneIdFilterActive;
		private Integer sceneIdInt;

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

		public boolean isTemplateNameContainsActive() {
			return templateNameContainsActive;
		}

		public void setTemplateNameContainsActive(boolean templateNameContainsActive) {
			this.templateNameContainsActive = templateNameContainsActive;
		}

		public String getTemplateNameContains() {
			return templateNameContains;
		}

		public void setTemplateNameContains(String templateNameContains) {
			this.templateNameContains = templateNameContains;
		}

		public boolean isStatusKeyPresent() {
			return statusKeyPresent;
		}

		public void setStatusKeyPresent(boolean statusKeyPresent) {
			this.statusKeyPresent = statusKeyPresent;
		}

		public String getStatusValue() {
			return statusValue;
		}

		public void setStatusValue(String statusValue) {
			this.statusValue = statusValue;
		}

		public boolean isTemplateTypeFilterActive() {
			return templateTypeFilterActive;
		}

		public void setTemplateTypeFilterActive(boolean templateTypeFilterActive) {
			this.templateTypeFilterActive = templateTypeFilterActive;
		}

		public String getTemplateTypeRaw() {
			return templateTypeRaw;
		}

		public void setTemplateTypeRaw(String templateTypeRaw) {
			this.templateTypeRaw = templateTypeRaw;
		}

		public boolean isSceneIdFilterActive() {
			return sceneIdFilterActive;
		}

		public void setSceneIdFilterActive(boolean sceneIdFilterActive) {
			this.sceneIdFilterActive = sceneIdFilterActive;
		}

		public Integer getSceneIdInt() {
			return sceneIdInt;
		}

		public void setSceneIdInt(Integer sceneIdInt) {
			this.sceneIdInt = sceneIdInt;
		}
	}
}
