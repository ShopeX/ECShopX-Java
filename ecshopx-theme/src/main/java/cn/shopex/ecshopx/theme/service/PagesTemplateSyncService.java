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

import cn.shopex.ecshopx.common.dispatch.PagesTemplateSyncDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesTemplateSyncRequest;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PagesTemplateSyncService {

	private final PagesTemplateSyncDispatchPublisher pagesTemplateSyncDispatchPublisher;

	public PagesTemplateSyncService(PagesTemplateSyncDispatchPublisher pagesTemplateSyncDispatchPublisher) {
		this.pagesTemplateSyncDispatchPublisher = pagesTemplateSyncDispatchPublisher;
	}

	public Map<String, Object> sync(long companyId, PagesTemplateSyncRequest body, String localeTag) {
		if (body == null) {
			throw new BadRequestException("pages_template_id 无效");
		}
		Long pid = body.getPagesTemplateId();
		if (pid == null || pid <= 0L) {
			throw new BadRequestException("pages_template_id 无效");
		}
		int isAllDistributor = Integer.valueOf(1).equals(body.getIsAllDistributor()) ? 1 : 2;
		pagesTemplateSyncDispatchPublisher.publish(
				companyId, pid, isAllDistributor, body.getDistributorIds(), localeTag);
		return Map.of("status", Boolean.TRUE);
	}
}
