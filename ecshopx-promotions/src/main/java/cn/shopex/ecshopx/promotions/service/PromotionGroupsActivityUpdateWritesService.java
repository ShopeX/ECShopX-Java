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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionGroupsActivityMultiLangWriteService;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionGroupsActivityUpdateWritesService {

	private final MessageSource messageSource;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PromotionGroupsActivityMultiLangWriteService promotionGroupsActivityMultiLangWriteService;
	private final PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler;

	public PromotionGroupsActivityUpdateWritesService(
			MessageSource messageSource,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PromotionGroupsActivityMultiLangWriteService promotionGroupsActivityMultiLangWriteService,
			PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler) {
		this.messageSource = messageSource;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.promotionGroupsActivityMultiLangWriteService = promotionGroupsActivityMultiLangWriteService;
		this.promotionGroupsActivityAdminRowAssembler = promotionGroupsActivityAdminRowAssembler;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> applyUpdateWrites(
			PromotionGroupsActivity entity,
			Map<String, Object> requestData,
			String requestLangTag,
			Locale locale) {
		int updatedRows = promotionGroupsActivityMapper.updateById(entity);
		if (updatedRows == 0) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		long id = entity.getGroupsActivityId();
		long companyId = entity.getCompanyId();
		String langTag = StringUtils.hasText(requestLangTag) ? requestLangTag.trim() : "zh-CN";
		promotionGroupsActivityMultiLangWriteService.addForNewGroup(id, companyId, requestData, langTag);
		PromotionGroupsActivity fresh = promotionGroupsActivityMapper.selectById(id);
		if (fresh == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		return promotionGroupsActivityAdminRowAssembler.toRow(fresh, now);
	}
}
