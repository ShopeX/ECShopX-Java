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
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsActivityDeleteService {

	private final MessageSource messageSource;
	private final PromotionGroupsActivityDeleteWritesService promotionGroupsActivityDeleteWritesService;
	private final MarketingActivityPostCommitJobsService marketingActivityPostCommitJobsService;

	public PromotionGroupsActivityDeleteService(
			MessageSource messageSource,
			PromotionGroupsActivityDeleteWritesService promotionGroupsActivityDeleteWritesService,
			MarketingActivityPostCommitJobsService marketingActivityPostCommitJobsService) {
		this.messageSource = messageSource;
		this.promotionGroupsActivityDeleteWritesService = promotionGroupsActivityDeleteWritesService;
		this.marketingActivityPostCommitJobsService = marketingActivityPostCommitJobsService;
	}

	public void deletePromotionGroupsActivity(long companyId, String groupId) {
		Locale locale =
				Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		if (groupId == null || !StringUtils.hasText(groupId.trim())) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		long groupsActivityId;
		try {
			groupsActivityId = Long.parseLong(groupId.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.no_update_data_found", null, locale));
		}
		long id = promotionGroupsActivityDeleteWritesService.applyDeleteWrites(companyId, groupsActivityId, locale);
		marketingActivityPostCommitJobsService.enqueueGroupSalespersonItemsShelves(companyId, id);
	}
}
