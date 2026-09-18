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

package cn.shopex.ecshopx.reservation.service;

import cn.shopex.ecshopx.reservation.domain.ResourceLevel;
import cn.shopex.ecshopx.reservation.domain.ResourceLevelRelService;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelMapper;
import cn.shopex.ecshopx.reservation.mapper.ResourceLevelRelServiceMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceLevelCreateService {

	private final ResourceLevelMapper resourceLevelMapper;
	private final ResourceLevelRelServiceMapper resourceLevelRelServiceMapper;

	public ResourceLevelCreateService(
			ResourceLevelMapper resourceLevelMapper,
			ResourceLevelRelServiceMapper resourceLevelRelServiceMapper) {
		this.resourceLevelMapper = resourceLevelMapper;
		this.resourceLevelRelServiceMapper = resourceLevelRelServiceMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void createResourceLevel(long companyId, Map<String, Object> post, List<Long> materialIds) {
		int now = (int) Instant.now().getEpochSecond();
		ResourceLevel entity = new ResourceLevel();
		entity.setCompanyId(String.valueOf(companyId));
		entity.setShopId(Objects.toString(post.get("shopId"), "").trim());
		entity.setShopName(Objects.toString(post.get("shopName"), ""));
		entity.setName(Objects.toString(post.get("name"), ""));
		entity.setDescription(Objects.toString(post.get("description"), ""));
		entity.setStatus(Objects.toString(post.get("status"), "active"));
		Object imageUrl = post.get("image_url");
		if (imageUrl == null) {
			entity.setImageUrl(null);
		} else {
			String s = imageUrl.toString().trim();
			entity.setImageUrl(s.isEmpty() ? null : s);
		}
		entity.setCreated(now);

		resourceLevelMapper.insert(entity);
		Long resourceLevelId = entity.getResourceLevelId();
		if (resourceLevelId == null) {
			throw new IllegalStateException("resource_level_id not generated");
		}

		long shopIdLong = Long.parseLong(entity.getShopId().trim());
		if (!materialIds.isEmpty()) {
			for (Long materialId : materialIds) {
				ResourceLevelRelService rel = new ResourceLevelRelService();
				rel.setResourceLevelId(resourceLevelId);
				rel.setCompanyId(companyId);
				rel.setShopId(shopIdLong);
				rel.setMaterialId(materialId);
				rel.setCreated(now);
				resourceLevelRelServiceMapper.insert(rel);
			}
		}
	}
}
