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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WxappItemIntroService {

	private final ItemsRepository itemsRepository;
	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final SupplierItemsDetailCoreService supplierItemsDetailCoreService;
	private final ObjectMapper objectMapper;

	public WxappItemIntroService(
			ItemsRepository itemsRepository,
			PlatformItemsDetailCoreService platformItemsDetailCoreService,
			SupplierItemsDetailCoreService supplierItemsDetailCoreService,
			ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.supplierItemsDetailCoreService = supplierItemsDetailCoreService;
		this.objectMapper = objectMapper;
	}

	public String buildIntroBody(long companyId, long itemId, String authorizerAppId) {
		String appId = authorizerAppId == null ? "" : authorizerAppId;
		Map<String, Object> detail;
		Items platformRow = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (platformRow != null) {
			detail = platformItemsDetailCoreService.build(companyId, itemId, appId);
		} else {
			detail = supplierItemsDetailCoreService.build(companyId, itemId, appId);
		}
		if (detail == null || detail.isEmpty()) {
			throw new ResourceException("商品不存在或已下架");
		}
		Object intro = detail.get("intro");
		if (intro == null) {
			return "";
		}
		if (intro instanceof String s) {
			return s;
		}
		if (intro instanceof Map<?, ?> || intro instanceof Iterable<?>) {
			try {
				return objectMapper.writeValueAsString(intro);
			} catch (JsonProcessingException e) {
				return String.valueOf(intro);
			}
		}
		try {
			return objectMapper.writeValueAsString(intro);
		} catch (JsonProcessingException e) {
			return intro.toString();
		}
	}
}
