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

package cn.shopex.ecshopx.goods.service.ome;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.service.ItemsAttributesCreateService;
import cn.shopex.ecshopx.goods.service.ItemsAttributesUpdateService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class OmeBrandBatchPersistService {

	private static final String OME_SYNC_COUNTRY_CODE = "";

	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributesCreateService itemsAttributesCreateService;
	private final ItemsAttributesUpdateService itemsAttributesUpdateService;

	public OmeBrandBatchPersistService(ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributesCreateService itemsAttributesCreateService,
			ItemsAttributesUpdateService itemsAttributesUpdateService) {
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributesCreateService = itemsAttributesCreateService;
		this.itemsAttributesUpdateService = itemsAttributesUpdateService;
	}

	public void saveBrands(long companyId, List<Map<String, Object>> brandRows) {
		if (brandRows == null || brandRows.isEmpty()) {
			return;
		}
		for (Map<String, Object> brand : brandRows) {
			try {
				persistOneBrand(companyId, brand);
			} catch (ResourceException e) {
				throw e;
			} catch (RuntimeException e) {
				throw new ResourceException(e.getMessage());
			}
		}
	}

	private void persistOneBrand(long companyId, Map<String, Object> brand) {
		Object nameObj = brand.get("brand_name");
		Object codeObj = brand.get("brand_code");
		String attributeName = nameObj == null ? "" : nameObj.toString();
		String brandCode = codeObj == null ? "" : codeObj.toString();

		Map<String, Object> saveInput = new LinkedHashMap<>();
		saveInput.put("attribute_type", "brand");
		saveInput.put("attribute_name", attributeName);
		saveInput.put("attribute_code", brandCode);

		ItemsAttributes existing = itemsAttributesRepository.selectByCompanyAndAttributeTypeAndAttributeCode(companyId, "brand", brandCode);
		if (existing != null) {
			Long attributeId = existing.getAttributeId();
			if (attributeId == null) {
				throw new ResourceException("更新的数据不存在");
			}
			itemsAttributesUpdateService.updateAttr(companyId, attributeId, saveInput, OME_SYNC_COUNTRY_CODE);
		} else {
			itemsAttributesCreateService.createAttr(companyId, saveInput, OME_SYNC_COUNTRY_CODE);
		}
	}
}
