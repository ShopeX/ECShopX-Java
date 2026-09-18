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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceLevelUpdateService {

	private final ResourceLevelMapper resourceLevelMapper;
	private final ResourceLevelRelServiceMapper resourceLevelRelServiceMapper;

	public ResourceLevelUpdateService(
			ResourceLevelMapper resourceLevelMapper,
			ResourceLevelRelServiceMapper resourceLevelRelServiceMapper) {
		this.resourceLevelMapper = resourceLevelMapper;
		this.resourceLevelRelServiceMapper = resourceLevelRelServiceMapper;
	}

	/**
	 * @return true 业务成功；false 表示主记录不存在（SELECT 无行），不抛异常。
	 */
	@Transactional(rollbackFor = Exception.class)
	public boolean updateResourceLevel(
			long companyId,
			Long resourceLevelId,
			String shopIdFromRequest,
			Map<String, Object> postData,
			List<Long> materialIds) {
		if (resourceLevelId == null) {
			return false;
		}
		String companyIdStr = String.valueOf(companyId);
		LambdaQueryWrapper<ResourceLevel> exist = new LambdaQueryWrapper<>();
		exist.eq(ResourceLevel::getResourceLevelId, resourceLevelId).eq(ResourceLevel::getCompanyId, companyIdStr);
		ResourceLevel existing = resourceLevelMapper.selectOne(exist);
		if (existing == null) {
			return false;
		}

		int now = (int) Instant.now().getEpochSecond();
		String name = Objects.toString(postData.get("name"), "");
		String description = Objects.toString(postData.get("description"), "");
		String status = Objects.toString(postData.get("status"), "active");
		Object imageUrlObj = postData.get("image_url");
		String imageUrl = null;
		if (imageUrlObj != null) {
			String s = imageUrlObj.toString().trim();
			imageUrl = s.isEmpty() ? null : s;
		}

		LambdaUpdateWrapper<ResourceLevel> uw = new LambdaUpdateWrapper<>();
		uw.eq(ResourceLevel::getResourceLevelId, resourceLevelId)
				.eq(ResourceLevel::getCompanyId, companyIdStr)
				.eq(ResourceLevel::getShopId, shopIdFromRequest)
				.set(ResourceLevel::getCompanyId, companyIdStr)
				.set(ResourceLevel::getName, name)
				.set(ResourceLevel::getDescription, description)
				.set(ResourceLevel::getStatus, status)
				.set(ResourceLevel::getImageUrl, imageUrl)
				.set(ResourceLevel::getUpdated, now);
		resourceLevelMapper.update(null, uw);

		LambdaQueryWrapper<ResourceLevelRelService> dq = new LambdaQueryWrapper<>();
		dq.eq(ResourceLevelRelService::getCompanyId, companyId)
				.eq(ResourceLevelRelService::getResourceLevelId, resourceLevelId);
		resourceLevelRelServiceMapper.delete(dq);

		if (!materialIds.isEmpty()) {
			String shopIdTrim = Objects.toString(postData.get("shopId"), "").trim();
			long shopIdLong = Long.parseLong(shopIdTrim);
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
		return true;
	}
}
