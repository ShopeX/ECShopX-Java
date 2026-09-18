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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcTemplateContentSaveRequest;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplate;
import cn.shopex.ecshopx.theme.domain.ThemePcTemplateContent;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateContentMapper;
import cn.shopex.ecshopx.theme.mapper.ThemePcTemplateMapper;
import cn.shopex.ecshopx.theme.support.PcTemplateContentPhysicalIdReader;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PcTemplateContentSaveService {

	private final ThemePcTemplateMapper themePcTemplateMapper;
	private final ThemePcTemplateContentMapper themePcTemplateContentMapper;
	private final PcTemplateContentPhysicalIdReader pcTemplateContentPhysicalIdReader;
	private final ObjectMapper objectMapper;

	@SuppressWarnings("unused")
	public void saveTemplateContent(long companyId, String requestLang, PcTemplateContentSaveRequest req) {
		String rawConfig = req.getConfig();
		if (rawConfig == null || !StringUtils.hasText(rawConfig.trim())) {
			throw new BadRequestException("页面装修内容不合法");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(rawConfig.trim());
		} catch (JsonProcessingException ex) {
			throw new BadRequestException("页面装修内容不合法");
		}
		// JSON object root (including "{}"): no block elements — treat as an empty block list (no inserts; orphan rows removed via diff only).
		final List<JsonNode> blocks;
		if (root.isArray()) {
			blocks = new ArrayList<>(root.size());
			for (int i = 0; i < root.size(); i++) {
				JsonNode el = root.get(i);
				requireConfigArrayElementNullOrObject(el);
				blocks.add(el);
			}
		} else if (root.isObject()) {
			blocks = new ArrayList<>();
		} else {
			throw new BadRequestException("页面装修内容不合法");
		}

		String rawTplId = req.getThemePcTemplateId();
		if (rawTplId == null || !StringUtils.hasText(rawTplId.trim())) {
			throw new BadRequestException("缺少 theme_pc_template_id");
		}
		long themePcTemplateId;
		try {
			themePcTemplateId = Long.parseLong(rawTplId.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("theme_pc_template_id 无效");
		}

		ThemePcTemplate tpl = themePcTemplateMapper.selectById(themePcTemplateId);
		if (tpl == null || tpl.getCompanyId() == null || tpl.getCompanyId() != companyId) {
			throw new ResourceException("页面不存在");
		}

		List<Long> oldIds = pcTemplateContentPhysicalIdReader.listIdsByCompanyAndTemplate(companyId, themePcTemplateId);
		LinkedHashSet<Long> edited = new LinkedHashSet<>();

		int now = (int) (System.currentTimeMillis() / 1000L);
		for (int i = 0; i < blocks.size(); i++) {
			JsonNode row = blocks.get(i);
			boolean nullSlot = row == null || row.isNull();
			int sortBy = i + 1;
			String name = "";
			long blockId = nullSlot ? 0L : resolveBlockId(row);

			if (blockId > 0L) {
				String paramsJson;
				try {
					paramsJson = objectMapper.writeValueAsString(row);
				} catch (JsonProcessingException ex) {
					throw new BadRequestException("页面装修内容不合法");
				}
				LambdaUpdateWrapper<ThemePcTemplateContent> uw =
						new LambdaUpdateWrapper<ThemePcTemplateContent>()
								.eq(ThemePcTemplateContent::getThemePcTemplateContentId, blockId)
								.eq(ThemePcTemplateContent::getCompanyId, companyId)
								.eq(ThemePcTemplateContent::getThemePcTemplateId, themePcTemplateId)
								.set(ThemePcTemplateContent::getName, name)
								.set(ThemePcTemplateContent::getParams, paramsJson)
								.set(ThemePcTemplateContent::getSortBy, sortBy)
								.set(ThemePcTemplateContent::getUpdated, now);
				int affected = themePcTemplateContentMapper.update(null, uw);
				if (affected != 1) {
					throw new ResourceException("未查询到更新数据");
				}
				edited.add(blockId);
			} else {
				String paramsFirst;
				try {
					paramsFirst = nullSlot ? objectMapper.writeValueAsString(objectMapper.nullNode()) : objectMapper.writeValueAsString(row);
				} catch (JsonProcessingException ex) {
					throw new BadRequestException("页面装修内容不合法");
				}
				ThemePcTemplateContent ins = new ThemePcTemplateContent();
				ins.setCompanyId(companyId);
				ins.setThemePcTemplateId(themePcTemplateId);
				ins.setName(name);
				ins.setParams(paramsFirst);
				ins.setSortBy(sortBy);
				ins.setCreated(now);
				ins.setUpdated(now);
				int inserted = themePcTemplateContentMapper.insert(ins);
				if (inserted != 1 || ins.getThemePcTemplateContentId() == null) {
					throw new ResourceException("未查询到更新数据");
				}
				long newId = ins.getThemePcTemplateContentId();
				ObjectNode withId;
				if (nullSlot) {
					withId = objectMapper.createObjectNode();
					withId.put("id", newId);
				} else {
					JsonNode roundTrip;
					try {
						roundTrip = objectMapper.readTree(objectMapper.writeValueAsString(row));
					} catch (JsonProcessingException ex) {
						throw new BadRequestException("页面装修内容不合法");
					}
					if (!roundTrip.isObject()) {
						throw new BadRequestException("页面装修内容不合法");
					}
					withId = (ObjectNode) roundTrip;
					withId.put("id", newId);
				}
				String paramsWithId;
				try {
					paramsWithId = objectMapper.writeValueAsString(withId);
				} catch (JsonProcessingException ex) {
					throw new BadRequestException("页面装修内容不合法");
				}
				LambdaUpdateWrapper<ThemePcTemplateContent> uw2 =
						new LambdaUpdateWrapper<ThemePcTemplateContent>()
								.eq(ThemePcTemplateContent::getThemePcTemplateContentId, newId)
								.eq(ThemePcTemplateContent::getCompanyId, companyId)
								.eq(ThemePcTemplateContent::getThemePcTemplateId, themePcTemplateId)
								.set(ThemePcTemplateContent::getParams, paramsWithId)
								.set(ThemePcTemplateContent::getUpdated, now);
				int affected2 = themePcTemplateContentMapper.update(null, uw2);
				if (affected2 != 1) {
					throw new ResourceException("未查询到更新数据");
				}
				edited.add(newId);
			}
		}

		List<Long> diff = new ArrayList<>();
		for (Long id : oldIds) {
			if (!edited.contains(id)) {
				diff.add(id);
			}
		}
		if (!diff.isEmpty()) {
			LambdaQueryWrapper<ThemePcTemplateContent> del =
					new LambdaQueryWrapper<ThemePcTemplateContent>()
							.eq(ThemePcTemplateContent::getCompanyId, companyId)
							.eq(ThemePcTemplateContent::getThemePcTemplateId, themePcTemplateId)
							.in(ThemePcTemplateContent::getThemePcTemplateContentId, diff);
			themePcTemplateContentMapper.delete(del);
		}
	}

	private static void requireConfigArrayElementNullOrObject(JsonNode n) {
		if (n == null || (!n.isNull() && !n.isObject())) {
			throw new BadRequestException("页面装修内容不合法");
		}
	}

	private static long resolveBlockId(JsonNode row) {
		if (!row.hasNonNull("id")) {
			return 0L;
		}
		JsonNode idNode = row.get("id");
		if (idNode.isIntegralNumber()) {
			return idNode.longValue();
		}
		if (idNode.isTextual()) {
			try {
				long parsed = Long.parseLong(idNode.asText().trim());
				if (parsed > 0L) {
					return parsed;
				}
			} catch (NumberFormatException ex) {
				return 0L;
			}
		}
		return 0L;
	}
}
