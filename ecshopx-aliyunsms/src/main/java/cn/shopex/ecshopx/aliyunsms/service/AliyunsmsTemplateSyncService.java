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

import cn.shopex.ecshopx.aliyunsms.domain.SceneItem;
import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.domain.Task;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneItemMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TaskMapper;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsGetSmsTemplateClient;
import cn.shopex.ecshopx.common.aliyunsms.AliyunsmsQuerySmsTemplateListClient;
import cn.shopex.ecshopx.common.aliyunsms.GetSmsTemplateResult;
import cn.shopex.ecshopx.common.aliyunsms.QuerySmsTemplateListItem;
import cn.shopex.ecshopx.common.aliyunsms.SyncSmsTemplateResult;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsTemplateSyncService {

	private static final Logger log = LoggerFactory.getLogger(AliyunsmsTemplateSyncService.class);
	private static final int PAGE_SIZE = 50;

	private final TemplateMapper templateMapper;
	private final SceneItemMapper sceneItemMapper;
	private final TaskMapper taskMapper;
	private final AliyunsmsQuerySmsTemplateListClient querySmsTemplateListClient;
	private final AliyunsmsGetSmsTemplateClient getSmsTemplateClient;

	public AliyunsmsTemplateSyncService(
			TemplateMapper templateMapper,
			SceneItemMapper sceneItemMapper,
			TaskMapper taskMapper,
			AliyunsmsQuerySmsTemplateListClient querySmsTemplateListClient,
			AliyunsmsGetSmsTemplateClient getSmsTemplateClient) {
		this.templateMapper = templateMapper;
		this.sceneItemMapper = sceneItemMapper;
		this.taskMapper = taskMapper;
		this.querySmsTemplateListClient = querySmsTemplateListClient;
		this.getSmsTemplateClient = getSmsTemplateClient;
	}

	public SyncSmsTemplateResult sync(long companyId) {
		SyncSmsTemplateResult result = new SyncSmsTemplateResult();
		log.info("SyncSmsTemplates started companyId={}", companyId);
		Set<String> cloudCodes = fetchCloudCodes(companyId);
		List<Template> localTemplates = templateMapper.selectList(Wrappers.<Template>lambdaQuery().eq(Template::getCompanyId, companyId));
		log.info("SyncSmsTemplates snapshot companyId={} cloudTemplateCount={} localBeforeCount={}", companyId, cloudCodes.size(), localTemplates.size());
		Map<String, Template> localByCode = new LinkedHashMap<>();
		for (Template template : localTemplates) {
			if (template.getTemplateCode() != null && !template.getTemplateCode().isBlank()) {
				localByCode.put(template.getTemplateCode(), template);
			}
		}

		for (String templateCode : cloudCodes) {
			try {
				GetSmsTemplateResult cloud = getSmsTemplateClient.getSmsTemplate(companyId, templateCode);
				Template existing = localByCode.get(templateCode);
				if (existing == null) {
					Template created = new Template();
					created.setCompanyId(companyId);
					created.setTemplateCode(templateCode);
					created.setTemplateName(trimToLength(cloud.templateName(), 255));
					created.setTemplateType(trimToLength(defaultIfBlank(cloud.templateType(), "0"), 255));
					created.setRemark(trimToLength(cloud.remark(), 255));
					created.setTemplateContent(defaultIfBlank(cloud.templateContent(), ""));
					created.setSceneId(0);
					created.setRelatedSignName(trimToLength(cloud.relatedSignName(), 20));
					created.setStatus(mapStatus(cloud.templateStatus()));
					created.setReason(trimToLength(cloud.rejectInfo(), 255));
					int now = (int) Instant.now().getEpochSecond();
					created.setCreated(now);
					created.setUpdated(now);
					templateMapper.insert(created);
					result.incCreated();
				} else {
					existing.setTemplateName(trimToLength(defaultIfBlank(cloud.templateName(), existing.getTemplateName()), 255));
					existing.setTemplateType(trimToLength(defaultIfBlank(cloud.templateType(), existing.getTemplateType()), 255));
					existing.setRemark(trimToLength(defaultIfBlank(cloud.remark(), existing.getRemark()), 255));
					existing.setTemplateContent(defaultIfBlank(cloud.templateContent(), existing.getTemplateContent()));
					existing.setRelatedSignName(trimToLength(defaultIfBlank(cloud.relatedSignName(), existing.getRelatedSignName()), 20));
					existing.setStatus(mapStatus(cloud.templateStatus()));
					existing.setReason(trimToLength(cloud.rejectInfo(), 255));
					existing.setUpdated((int) Instant.now().getEpochSecond());
					templateMapper.updateById(existing);
					result.incUpdated();
				}
			} catch (RuntimeException e) {
				result.incFailed();
				result.addError(templateCode, e.getMessage());
				log.warn("SyncSmsTemplates upsert failed: {} => {}", templateCode, e.getMessage());
			}
		}

		cleanupOrphans(companyId, cloudCodes, localTemplates, result);
		log.info("SyncSmsTemplates finished companyId={} created={} updated={} deleted={} skipped={} failed={}", companyId, result.created(), result.updated(), result.deleted(), result.skipped(), result.failed());
		return result;
	}

	private Set<String> fetchCloudCodes(long companyId) {
		Set<String> codes = new LinkedHashSet<>();
		int pageIndex = 1;
		while (true) {
			List<QuerySmsTemplateListItem> page = querySmsTemplateListClient.querySmsTemplateList(companyId, pageIndex, PAGE_SIZE);
			log.info("SyncSmsTemplates list page companyId={} pageIndex={} pageSize={} pageReturnedCount={}", companyId, pageIndex, PAGE_SIZE, page.size());
			for (QuerySmsTemplateListItem item : page) {
				if (item != null && item.templateCode() != null && !item.templateCode().isBlank()) {
					codes.add(item.templateCode());
				}
			}
			if (page.size() < PAGE_SIZE) {
				log.info("SyncSmsTemplates list completed companyId={} totalUniqueCloudTemplates={}", companyId, codes.size());
				return codes;
			}
			pageIndex++;
		}
	}

	private void cleanupOrphans(long companyId, Set<String> cloudCodes, List<Template> localTemplates, SyncSmsTemplateResult result) {
		for (Template template : new ArrayList<>(localTemplates)) {
			if (template.getTemplateCode() == null || template.getTemplateCode().isBlank() || cloudCodes.contains(template.getTemplateCode())) {
				continue;
			}
			long sceneCount = sceneItemMapper.selectCount(Wrappers.<SceneItem>lambdaQuery().eq(SceneItem::getCompanyId, companyId).apply("template_id = {0}", template.getId()));
			if (sceneCount > 0) {
				result.incSkipped();
				result.addError(template.getTemplateCode(), "本地模板仍被短信场景引用，跳过删除");
				continue;
			}
			long taskCount = taskMapper.selectCount(Wrappers.<Task>lambdaQuery().eq(Task::getCompanyId, companyId).apply("template_id = {0}", template.getId()).eq(Task::getStatus, "1"));
			if (taskCount > 0) {
				result.incSkipped();
				result.addError(template.getTemplateCode(), "本地模板仍被进行中的群发任务引用，跳过删除");
				continue;
			}
			templateMapper.deleteById(template.getId());
			result.incDeleted();
		}
	}

	private static String mapStatus(String templateStatus) {
		return templateStatus == null || templateStatus.isBlank() ? "0" : templateStatus;
	}

	private static String trimToLength(String value, int maxLength) {
		if (value == null) {
			return "";
		}
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	private static String defaultIfBlank(String value, String fallback) {
		return value == null || value.isBlank() ? (fallback == null ? "" : fallback) : value;
	}
}
