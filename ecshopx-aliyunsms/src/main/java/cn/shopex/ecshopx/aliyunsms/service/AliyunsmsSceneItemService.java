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
import cn.shopex.ecshopx.aliyunsms.domain.SceneItem;
import cn.shopex.ecshopx.aliyunsms.domain.Sign;
import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneItemMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.SignMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsSceneItemService {

	private final SceneItemMapper sceneItemMapper;
	private final SceneMapper sceneMapper;
	private final SignMapper signMapper;
	private final TemplateMapper templateMapper;

	public AliyunsmsSceneItemService(
			SceneItemMapper sceneItemMapper,
			SceneMapper sceneMapper,
			SignMapper signMapper,
			TemplateMapper templateMapper) {
		this.sceneItemMapper = sceneItemMapper;
		this.sceneMapper = sceneMapper;
		this.signMapper = signMapper;
		this.templateMapper = templateMapper;
	}

	/**
	 * 按企业与场景 ID 列表查询场景短信实例，最多返回第一页的 100 条记录，并按 {@code scene_id} 分组。
	 * 每条记录为蛇形键的字段映射；{@code sceneIds} 为空时不查询数据库，返回空映射。
	 */
	public Map<Long, List<Map<String, Object>>> getItemRowsGroupedBySceneId(long companyId, List<Long> sceneIds) {
		if (sceneIds == null || sceneIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Integer> intIds = sceneIds.stream().map(Long::intValue).toList();
		LambdaQueryWrapper<SceneItem> w = new LambdaQueryWrapper<>();
		w.eq(SceneItem::getCompanyId, companyId).in(SceneItem::getSceneId, intIds);
		Page<SceneItem> page = new Page<>(1, 100);
		sceneItemMapper.selectPage(page, w);

		Map<Long, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
		for (SceneItem item : page.getRecords()) {
			long sid = item.getSceneId().longValue();
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", item.getId());
			row.put("template_content", item.getTemplateContent());
			row.put("sign_name", item.getSignName());
			row.put("scene_id", item.getSceneId());
			row.put("status", item.getStatus());
			grouped.computeIfAbsent(sid, k -> new ArrayList<>()).add(row);
		}
		return grouped;
	}

	public void addItem(long companyId, int sceneId, int signId, int templateId) {
		Sign sign =
				signMapper.selectOne(
						Wrappers.<Sign>lambdaQuery()
								.eq(Sign::getId, (long) signId)
								.eq(Sign::getCompanyId, companyId)
								.eq(Sign::getStatus, "1"));
		if (sign == null) {
			throw new ResourceException("请选择有效的签名");
		}
		Template template =
				templateMapper.selectOne(
						Wrappers.<Template>lambdaQuery()
								.eq(Template::getId, (long) templateId)
								.eq(Template::getCompanyId, companyId)
								.eq(Template::getStatus, "1"));
		if (template == null) {
			throw new ResourceException("请选择有效的模板");
		}
		long cnt =
				sceneItemMapper.selectCount(
						Wrappers.<SceneItem>lambdaQuery()
								.eq(SceneItem::getCompanyId, companyId)
								.eq(SceneItem::getSceneId, sceneId));
		if (cnt >= 3) {
			throw new ResourceException("每个场景最多三条短信");
		}
		int now = (int) Instant.now().getEpochSecond();
		SceneItem row = new SceneItem();
		row.setCompanyId(companyId);
		row.setSceneId(sceneId);
		row.setSignId(signId);
		row.setTemplateId(templateId);
		row.setSignName(sign.getSignName());
		row.setTemplateContent(template.getTemplateContent());
		row.setStatus(0);
		row.setCreated(now);
		row.setUpdated(now);
		sceneItemMapper.insert(row);
	}

	public void deleteItem(long companyId, long itemId) {
		LambdaQueryWrapper<SceneItem> w =
				Wrappers.<SceneItem>lambdaQuery()
						.eq(SceneItem::getCompanyId, companyId)
						.eq(SceneItem::getId, itemId);
		sceneItemMapper.delete(w);
	}

	public void disableItem(long companyId, Long itemId) {
		if (itemId == null) {
			return;
		}
		SceneItem sceneItem = sceneItemMapper.selectOne(
				Wrappers.<SceneItem>lambdaQuery().eq(SceneItem::getId, itemId).eq(SceneItem::getStatus, 1));
		if (sceneItem == null) {
			return;
		}
		SceneItem itemPatch = new SceneItem();
		itemPatch.setStatus(0);
		int n = sceneItemMapper.update(
				itemPatch,
				Wrappers.<SceneItem>lambdaUpdate()
						.eq(SceneItem::getCompanyId, companyId)
						.eq(SceneItem::getId, itemId));
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		Scene scenePatch = new Scene();
		scenePatch.setStatus("disabled");
		int m = sceneMapper.update(
				scenePatch,
				Wrappers.<Scene>lambdaUpdate().eq(Scene::getId, Long.valueOf(sceneItem.getSceneId())));
		if (m == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}

	public void enableItem(long companyId, Long itemId) {
		if (itemId == null) {
			return;
		}
		SceneItem sceneItem = sceneItemMapper.selectOne(
				Wrappers.<SceneItem>lambdaQuery().eq(SceneItem::getId, itemId).eq(SceneItem::getStatus, 0));
		if (sceneItem == null) {
			return;
		}
		long cnt = sceneItemMapper.selectCount(
				Wrappers.<SceneItem>lambdaQuery().eq(SceneItem::getSceneId, sceneItem.getSceneId()));
		if (cnt == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		SceneItem zeroPatch = new SceneItem();
		zeroPatch.setStatus(0);
		sceneItemMapper.update(
				zeroPatch,
				Wrappers.<SceneItem>lambdaUpdate().eq(SceneItem::getSceneId, sceneItem.getSceneId()));

		SceneItem onePatch = new SceneItem();
		onePatch.setStatus(1);
		int n = sceneItemMapper.update(
				onePatch,
				Wrappers.<SceneItem>lambdaUpdate()
						.eq(SceneItem::getCompanyId, companyId)
						.eq(SceneItem::getId, itemId));
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		Scene scenePatch = new Scene();
		scenePatch.setStatus("enabled");
		int m = sceneMapper.update(
				scenePatch,
				Wrappers.<Scene>lambdaUpdate().eq(Scene::getId, Long.valueOf(sceneItem.getSceneId())));
		if (m == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
