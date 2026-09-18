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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OmeItemSpecBatchPersistService {

	private static final String OME_SPEC_COUNTRY_CODE = "zh-CN";

	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributesCreateService itemsAttributesCreateService;
	private final ItemsAttributesUpdateService itemsAttributesUpdateService;

	public OmeItemSpecBatchPersistService(ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributesCreateService itemsAttributesCreateService,
			ItemsAttributesUpdateService itemsAttributesUpdateService) {
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributesCreateService = itemsAttributesCreateService;
		this.itemsAttributesUpdateService = itemsAttributesUpdateService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void saveSpecs(long companyId, List<Map<String, Object>> specs) {
		if (specs == null || specs.isEmpty()) {
			return;
		}
		try {
			for (Map<String, Object> spec : specs) {
				persistOneSpec(companyId, spec);
			}
		} catch (ResourceException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResourceException(e.getMessage());
		}
	}

	private void persistOneSpec(long companyId, Map<String, Object> spec) {
		Object codeObj = spec.get("spec_code");
		Object nameObj = spec.get("spec_name");
		String specCode = codeObj == null ? "" : codeObj.toString().trim();
		String specName = nameObj == null ? "" : nameObj.toString();

		Map<String, Object> input = new LinkedHashMap<>();
		input.put("attribute_code", specCode);
		input.put("attribute_name", specName);
		input.put("attribute_type", "item_spec");
		input.put("from_oms", Boolean.TRUE);

		List<Map<String, Object>> attributeValues = new ArrayList<>();
		Object specValueRaw = spec.get("spec_value");
		if (specValueRaw instanceof List<?> svList) {
			int key = 0;
			for (Object o : svList) {
				if (o instanceof Map<?, ?> m) {
					Map<String, Object> row = new LinkedHashMap<>();
					Object sv = m.get("spec_value");
					row.put("attribute_value", sv == null ? "" : sv.toString());
					Object img = m.get("spec_image");
					if (img != null) {
						row.put("image_url", img.toString());
					}
					Object omsId = m.get("spec_value_id");
					if (omsId != null) {
						row.put("oms_value_id", omsId);
					}
					row.put("sort", String.valueOf(key));
					key++;
					attributeValues.add(row);
				}
			}
		}
		input.put("attribute_values", attributeValues);

		ItemsAttributes info = itemsAttributesRepository.selectByCompanyAndAttributeTypeAndAttributeCode(companyId, "item_spec", specCode);
		if (info != null) {
			Long attributeId = info.getAttributeId();
			if (attributeId == null) {
				throw new ResourceException("更新的数据不存在");
			}
			itemsAttributesUpdateService.updateAttr(companyId, attributeId, input, OME_SPEC_COUNTRY_CODE);
		} else {
			itemsAttributesCreateService.createAttr(companyId, input, OME_SPEC_COUNTRY_CODE);
		}
	}
}
