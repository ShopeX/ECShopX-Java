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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.WxaSubscribeMessageTemplateAdminPort;
import cn.shopex.ecshopx.promotions.domain.WxaNoticeTemplate;
import cn.shopex.ecshopx.promotions.mapper.WxaNoticeTemplateMapper;
import cn.shopex.ecshopx.promotions.service.wxatemplate.WxaTemplateSceneDefinitions;
import cn.shopex.ecshopx.promotions.service.wxatemplate.WxaTemplateSceneDefinitions.WxaTemplateSceneDefinition;
import cn.shopex.ecshopx.promotions.service.wxatemplate.WxopenTemplateLibraryRedisAccessor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WxaTemplateOpenService {

	private static final Logger log = LoggerFactory.getLogger(WxaTemplateOpenService.class);

	private final WxaNoticeTemplateMapper wxaNoticeTemplateMapper;
	private final WxopenTemplateLibraryRedisAccessor wxopenTemplateLibraryRedisAccessor;
	private final WxaSubscribeMessageTemplateAdminPort wxaSubscribeMessageTemplateAdminPort;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;
	private final WxaTemplateOpenService self;

	public WxaTemplateOpenService(
			WxaNoticeTemplateMapper wxaNoticeTemplateMapper,
			WxopenTemplateLibraryRedisAccessor wxopenTemplateLibraryRedisAccessor,
			WxaSubscribeMessageTemplateAdminPort wxaSubscribeMessageTemplateAdminPort,
			ObjectMapper objectMapper,
			MessageSource messageSource,
			@Lazy WxaTemplateOpenService self) {
		this.wxaNoticeTemplateMapper = wxaNoticeTemplateMapper;
		this.wxopenTemplateLibraryRedisAccessor = wxopenTemplateLibraryRedisAccessor;
		this.wxaSubscribeMessageTemplateAdminPort = wxaSubscribeMessageTemplateAdminPort;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
		this.self = self;
	}

	public LinkedHashMap<String, Map<String, Object>> getWxaTemplateList(
			long companyId, @org.springframework.lang.Nullable String templateName, @org.springframework.lang.Nullable String wxappAppid) {
		List<Map.Entry<String, WxaTemplateSceneDefinition>> ordered =
				WxaTemplateSceneDefinitions.orderedScenesMatchingTemplateName(templateName);
		if (ordered.isEmpty()) {
			return new LinkedHashMap<>();
		}

		LambdaQueryWrapper<WxaNoticeTemplate> w = new LambdaQueryWrapper<>();
		w.eq(WxaNoticeTemplate::getCompanyId, companyId).eq(WxaNoticeTemplate::getTemplateName, templateName);
		long total = wxaNoticeTemplateMapper.selectCount(w);
		Map<String, WxaNoticeTemplate> tmpList;
		if (total > 0) {
			tmpList = new LinkedHashMap<>();
			for (WxaNoticeTemplate row : wxaNoticeTemplateMapper.selectList(w)) {
				tmpList.put(row.getScenesName(), row);
			}
		} else {
			tmpList = Collections.emptyMap();
		}

		LinkedHashMap<String, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map.Entry<String, WxaTemplateSceneDefinition> entry : ordered) {
			String scenesName = entry.getKey();
			WxaTemplateSceneDefinition def = entry.getValue();
			if (tmpList.containsKey(scenesName)) {
				out.put(scenesName, buildListRowFromEntity(tmpList.get(scenesName)));
			} else {
				String templateIdFromRedis = wxopenTemplateLibraryRedisAccessor.hget(wxappAppid, def.getId(), scenesName);
				String templateId =
						(templateIdFromRedis != null && !templateIdFromRedis.isBlank())
								? templateIdFromRedis.trim()
								: "";
				WxaNoticeTemplate entity = new WxaNoticeTemplate();
				entity.setTemplateName(templateName);
				entity.setWxaTemplateId(def.getId());
				entity.setTemplateId(templateId);
				entity.setCompanyId(companyId);
				entity.setNoticeType("wxa");
				entity.setTmplType(def.getTmplType());
				entity.setTitle(def.getListTitle());
				entity.setScenesName(scenesName);
				try {
					entity.setContent(objectMapper.writeValueAsString(def.getValueAsMaps()));
					entity.setSendTimeDesc(objectMapper.writeValueAsString(def.getSendTimeDesc()));
				} catch (JsonProcessingException e) {
					throw new IllegalStateException(e);
				}
				entity.setIsOpen(Boolean.FALSE);
				entity.setCreated((int) (System.currentTimeMillis() / 1000L));
				wxaNoticeTemplateMapper.insert(entity);
				out.put(scenesName, buildListRowAfterInsert(entity, def, scenesName));
			}
		}
		return out;
	}

	private Map<String, Object> buildListRowFromEntity(WxaNoticeTemplate e) {
		String scenesName = e.getScenesName();
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("template_name", e.getTemplateName());
		m.put("wxa_template_id", e.getWxaTemplateId());
		m.put("company_id", e.getCompanyId());
		m.put("notice_type", e.getNoticeType());
		m.put("tmpl_type", e.getTmplType());
		m.put("template_id", e.getTemplateId() != null ? e.getTemplateId() : "");
		m.put("title", e.getTitle());
		m.put("scenes_name", e.getScenesName());
		m.put("content", parseContentArrayForResponse(e.getContent(), scenesName));
		m.put("is_open", Boolean.TRUE.equals(e.getIsOpen()));
		m.put("send_time_desc", parseSendTimeDescObjectForResponse(e.getSendTimeDesc(), scenesName));
		int createdSec = e.getCreated() != null ? e.getCreated() : 0;
		m.put("created", (long) createdSec);
		return m;
	}

	private Map<String, Object> buildListRowAfterInsert(
			WxaNoticeTemplate entity, WxaTemplateSceneDefinition def, String scenesName) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", entity.getId());
		m.put("template_name", entity.getTemplateName());
		m.put("wxa_template_id", entity.getWxaTemplateId());
		m.put("company_id", entity.getCompanyId());
		m.put("notice_type", entity.getNoticeType());
		m.put("tmpl_type", entity.getTmplType());
		m.put("template_id", entity.getTemplateId() != null ? entity.getTemplateId() : "");
		m.put("title", entity.getTitle());
		m.put("scenes_name", entity.getScenesName());
		m.put("content", parseContentArrayForResponse(entity.getContent(), scenesName));
		m.put("is_open", Boolean.FALSE);
		m.put("send_time_desc", parseSendTimeDescObjectForResponse(entity.getSendTimeDesc(), scenesName));
		int createdSec = entity.getCreated() != null ? entity.getCreated() : 0;
		m.put("created", (long) createdSec);
		m.put("scene_desc", def.getListSceneDesc());
		return m;
	}

	private List<Object> parseContentArrayForResponse(String raw, String scenesName) {
		try {
			if (raw == null || raw.isBlank()) {
				return new ArrayList<>();
			}
			JsonNode node = objectMapper.readTree(raw);
			if (node.isArray()) {
				return objectMapper.convertValue(node, new TypeReference<List<Object>>() {});
			}
		} catch (Exception ex) {
			log.warn("Failed to parse promotions_notice_template.content as JSON array, scenes_name={}", scenesName, ex);
		}
		return new ArrayList<>();
	}

	private Map<String, Object> parseSendTimeDescObjectForResponse(String raw, String scenesName) {
		try {
			if (raw == null || raw.isBlank()) {
				return new LinkedHashMap<>();
			}
			JsonNode node = objectMapper.readTree(raw);
			if (node.isObject()) {
				return objectMapper.convertValue(node, new TypeReference<LinkedHashMap<String, Object>>() {});
			}
		} catch (Exception ex) {
			log.warn(
					"Failed to parse promotions_notice_template.send_time_desc as JSON object, scenes_name={}",
					scenesName,
					ex);
		}
		return new LinkedHashMap<>();
	}

	public void openWxaTemplate(
			long companyId,
			String templateName,
			String wxappAppid,
			String scenesName,
			boolean isOpen,
			long sendTime) {
		Locale locale = LocaleContextHolder.getLocale();
		LambdaQueryWrapper<WxaNoticeTemplate> w = new LambdaQueryWrapper<>();
		w.eq(WxaNoticeTemplate::getCompanyId, companyId)
				.eq(WxaNoticeTemplate::getScenesName, scenesName)
				.eq(WxaNoticeTemplate::getTemplateName, templateName);
		WxaNoticeTemplate info = wxaNoticeTemplateMapper.selectOne(w);
		if (info == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.wxa_template.no_update_data_found", null, locale));
		}

		String templateId = info.getTemplateId() != null ? info.getTemplateId().trim() : "";
		boolean hasTemplateId = StringUtils.hasText(templateId);

		if (!isOpen && hasTemplateId) {
			boolean delOk = wxaSubscribeMessageTemplateAdminPort.deleteTemplate(wxappAppid, templateId);
			if (delOk) {
				if (!StringUtils.hasText(info.getWxaTemplateId())) {
					throw new BadRequestException("wxa_template_id 缺失，无法删除微信侧模版");
				}
				info.setTemplateId("");
				wxopenTemplateLibraryRedisAccessor.hdelIfPresent(wxappAppid, info.getWxaTemplateId(), scenesName);
			}
		} else {
			Optional<WxaTemplateSceneDefinition> defOpt = WxaTemplateSceneDefinitions.findByScenesName(scenesName);
			if (defOpt.isPresent()) {
				try {
					WxaTemplateSceneDefinition def = defOpt.get();
					info.setContent(objectMapper.writeValueAsString(def.getValueAsMaps()));
				} catch (Exception e) {
					throw new ResourceException(
							messageSource.getMessage("promotions.wxa_template.no_update_data_found", null, locale));
				}
			}
			if (!hasTemplateId && isOpen) {
				WxaTemplateSceneDefinition def =
						WxaTemplateSceneDefinitions.findByScenesName(scenesName)
								.orElseThrow(() -> new BadRequestException("scenes_name 无效或不支持"));
				if (!StringUtils.hasText(info.getWxaTemplateId())) {
					throw new BadRequestException("wxa_template_id 缺失，无法通过微信新增模版");
				}
				List<Integer> kidList = def.getKeywordIdList();
				String sceneDesc = def.getSceneDesc();
				String newId =
						wxaSubscribeMessageTemplateAdminPort.addTemplate(
								wxappAppid, info.getWxaTemplateId(), kidList, sceneDesc);
				info.setTemplateId(newId);
				wxopenTemplateLibraryRedisAccessor.hset(wxappAppid, info.getWxaTemplateId(), scenesName, newId);
			} else {
				info.setTemplateId(templateId);
			}

			if (sendTime > 0L) {
				String rawDesc = info.getSendTimeDesc() == null ? "{}" : info.getSendTimeDesc();
				JsonNode root;
				try {
					root = objectMapper.readTree(rawDesc);
				} catch (Exception e) {
					throw new BadRequestException("send_time_desc 格式非法");
				}
				if (!root.isObject()) {
					throw new BadRequestException("send_time_desc 格式非法");
				}
				ObjectNode obj = (ObjectNode) root;
				obj.put("value", sendTime);
				try {
					info.setSendTimeDesc(objectMapper.writeValueAsString(obj));
				} catch (Exception e) {
					throw new BadRequestException("send_time_desc 格式非法");
				}
			}
		}

		info.setIsOpen(isOpen);
		info.setUpdated((int) (System.currentTimeMillis() / 1000L));
		self.updateNoticeTemplateRowTransactional(info);
	}

	@Transactional(rollbackFor = Exception.class)
	public void updateNoticeTemplateRowTransactional(WxaNoticeTemplate row) {
		int affected = wxaNoticeTemplateMapper.updateById(row);
		if (affected == 0) {
			throw new ResourceException(
					messageSource.getMessage(
							"promotions.wxa_template.no_update_data_found",
							null,
							LocaleContextHolder.getLocale()));
		}
	}
}
