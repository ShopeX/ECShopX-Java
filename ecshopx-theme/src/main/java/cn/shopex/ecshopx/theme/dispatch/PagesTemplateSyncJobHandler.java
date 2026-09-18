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

package cn.shopex.ecshopx.theme.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.service.PagesTemplateSyncDistributorQueryService;
import cn.shopex.ecshopx.theme.domain.PagesTemplate;
import cn.shopex.ecshopx.theme.domain.PagesTemplateSet;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateMapper;
import cn.shopex.ecshopx.theme.mapper.PagesTemplateSetMapper;
import cn.shopex.ecshopx.theme.service.PagesTemplateStoreSyncApplyService;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateSyncJobPayload;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PagesTemplateSyncJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(PagesTemplateSyncJobHandler.class);

	private static final String WEAPP_PAGES_FIXED = "distributor_index";

	private final PagesTemplateMapper pagesTemplateMapper;

	private final PagesTemplateSetMapper pagesTemplateSetMapper;

	private final PagesTemplateSyncDistributorQueryService pagesTemplateSyncDistributorQueryService;

	private final PagesTemplateStoreSyncApplyService pagesTemplateStoreSyncApplyService;

	private final ObjectMapper objectMapper;

	public PagesTemplateSyncJobHandler(
			PagesTemplateMapper pagesTemplateMapper,
			PagesTemplateSetMapper pagesTemplateSetMapper,
			PagesTemplateSyncDistributorQueryService pagesTemplateSyncDistributorQueryService,
			PagesTemplateStoreSyncApplyService pagesTemplateStoreSyncApplyService,
			ObjectMapper objectMapper) {
		this.pagesTemplateMapper = pagesTemplateMapper;
		this.pagesTemplateSetMapper = pagesTemplateSetMapper;
		this.pagesTemplateSyncDistributorQueryService = pagesTemplateSyncDistributorQueryService;
		this.pagesTemplateStoreSyncApplyService = pagesTemplateStoreSyncApplyService;
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		PagesTemplateSyncJobPayload jobPayload = toPayload(payload);
		long currentDistributorId = 0L;
		try {
			long companyId = jobPayload.companyId();
			long pagesTemplateId = jobPayload.pagesTemplateId();
			int isAllDistributor = jobPayload.isAllDistributor();
			String distributorIdsJson = jobPayload.distributorIdsJson();
			String localeTag = jobPayload.localeTag();

			int isEnforceSync = 2;
			PagesTemplate info =
					pagesTemplateMapper.selectOne(
							new LambdaQueryWrapper<PagesTemplate>()
									.eq(PagesTemplate::getCompanyId, companyId)
									.eq(PagesTemplate::getPagesTemplateId, pagesTemplateId)
									.isNull(PagesTemplate::getDeletedAt)
									.last("LIMIT 1"));
			if (info == null) {
				throw new ResourceException("无效的模板");
			}
			if (info.getTemplateType() == null || info.getTemplateType() != 0) {
				throw new ResourceException("无效的模板");
			}

			String templateTitle = info.getTemplateTitle();
			String templatePic = info.getTemplatePic();
			Integer elementEditStatus = info.getElementEditStatus();
			String templateName = info.getTemplateName();
			Long regionauthIdObj = info.getRegionauthId();
			long regionauthIdKey = regionauthIdObj == null ? 0L : regionauthIdObj;

			PagesTemplateSet setRow =
					pagesTemplateSetMapper.selectOne(
							new LambdaQueryWrapper<PagesTemplateSet>()
									.eq(PagesTemplateSet::getCompanyId, companyId)
									.eq(PagesTemplateSet::getRegionauthId, regionauthIdKey)
									.last("LIMIT 1"));
			if (setRow != null && setRow.getIsEnforceSync() != null) {
				isEnforceSync = setRow.getIsEnforceSync();
			}

			List<Long> distributorIds;
			if (isAllDistributor == 1) {
				distributorIds =
						pagesTemplateSyncDistributorQueryService.listDistributorIds(
								companyId, regionauthIdKey);
			} else {
				distributorIds = parseDistributorIdsJson(distributorIdsJson);
			}

			if (distributorIds == null || distributorIds.isEmpty()) {
				log.debug(
						"pages template sync skipped empty distributors companyId={} pagesTemplateId={}",
						companyId,
						pagesTemplateId);
				return;
			}

			String weappPagesFixed = WEAPP_PAGES_FIXED;
			long hqId = pagesTemplateId;

			for (Long distributorId : distributorIds) {
				if (distributorId == null || distributorId <= 0L) {
					continue;
				}
				currentDistributorId = distributorId;
				int enforce = isEnforceSync;
				long cnt =
						pagesTemplateMapper.selectCount(
								new LambdaQueryWrapper<PagesTemplate>()
										.eq(PagesTemplate::getCompanyId, companyId)
										.eq(
												PagesTemplate::getDistributorId,
												(int) Math.min(Integer.MAX_VALUE, distributorId))
										.isNull(PagesTemplate::getDeletedAt));
				if (cnt == 0L) {
					enforce = 1;
				}
				pagesTemplateStoreSyncApplyService.applyOneDistributor(
						companyId,
						distributorId,
						enforce,
						templateTitle,
						templatePic,
						weappPagesFixed,
						elementEditStatus,
						hqId,
						templateName,
						localeTag);
			}
		} catch (Exception e) {
			log.error(
					"pages template sync failed companyId={} pagesTemplateId={} distributorId={} message={}",
					jobPayload.companyId(),
					jobPayload.pagesTemplateId(),
					currentDistributorId,
					e.getMessage(),
					e);
			if (e instanceof RuntimeException re) {
				throw re;
			}
			throw new RuntimeException(e);
		}
	}

	private PagesTemplateSyncJobPayload toPayload(Map<String, Object> payload) {
		long companyId = extractLong(payload, "company_id");
		long pagesTemplateId = extractLong(payload, "pages_template_id");
		int isAllDistributor = extractInt(payload, "is_all_distributor", 2);
		String distributorIdsJson = extractOptionalString(payload, "distributor_ids");
		String localeTag = extractString(payload, "locale_tag");
		return new PagesTemplateSyncJobPayload(
				companyId, pagesTemplateId, isAllDistributor, distributorIdsJson, localeTag);
	}

	private static long extractLong(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static int extractInt(Map<String, Object> payload, String key, int defaultValue) {
		Object raw = payload.get(key);
		if (raw == null) {
			return defaultValue;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(raw).trim());
	}

	private static String extractString(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		return raw == null ? "" : String.valueOf(raw);
	}

	private static String extractOptionalString(Map<String, Object> payload, String key) {
		Object raw = payload.get(key);
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw);
		return s.isEmpty() ? null : s;
	}

	private List<Long> parseDistributorIdsJson(String json) {
		if (json == null || json.trim().isEmpty()) {
			return List.of();
		}
		try {
			List<Long> parsed =
					objectMapper.readValue(json.trim(), new TypeReference<List<Long>>() {});
			if (parsed == null) {
				return List.of();
			}
			List<Long> out = new ArrayList<>();
			for (Long id : parsed) {
				if (id != null && id > 0L) {
					out.add(id);
				}
			}
			return out;
		} catch (JsonProcessingException ex) {
			return List.of();
		}
	}
}
