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

package cn.shopex.ecshopx.goods.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemsUpdateOrchestrator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2NormalItemsEntityUpdateService {

	private static final String MSG_OLD_ITEM_BN_REQUIRED = "老的商品货号必填";
	private static final String MSG_MAIN_CAT_REQUIRED = "管理分类必填";
	private static final String MSG_NAME_REQUIRED = "商品名称必填";
	private static final String MSG_PICS_REQUIRED = "请上传商品图片";
	private static final String MSG_TEMPLATES_REQUIRED = "运费模板必填";
	private static final String MSG_BRAND_REQUIRED = "品牌必填";
	private static final String MSG_SALES_CAT_REQUIRED = "商品分类ID必填";
	private static final String MSG_PICS_FORMAT = "商品图片格式错误";
	private static final String MSG_SALES_CAT_FORMAT = "商品分类格式错误";
	private static final String MSG_NOSPEC_SWITCH = "多规格和单规格不能切换";
	private static final String MSG_GOODS_ERROR_FALLBACK = "商品错误";

	private final ItemsUpdateOrchestrator itemsUpdateOrchestrator;
	private final ItemsRepository itemsRepository;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2NormalItemsEntityUpdateService(
			ItemsUpdateOrchestrator itemsUpdateOrchestrator,
			ItemsRepository itemsRepository,
			ObjectMapper objectMapper) {
		this.itemsUpdateOrchestrator = itemsUpdateOrchestrator;
		this.itemsRepository = itemsRepository;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeUpdateItems(long companyId, Map<String, Object> mergedParams) {
		validateRequired(mergedParams.get("old_item_bn"), MSG_OLD_ITEM_BN_REQUIRED);
		validateRequired(mergedParams.get("item_main_cat_id"), MSG_MAIN_CAT_REQUIRED);
		validateRequired(mergedParams.get("item_name"), MSG_NAME_REQUIRED);
		validateRequired(mergedParams.get("pics"), MSG_PICS_REQUIRED);
		validateRequired(mergedParams.get("templates_id"), MSG_TEMPLATES_REQUIRED);
		validateRequired(mergedParams.get("brand_id"), MSG_BRAND_REQUIRED);
		validateRequired(mergedParams.get("item_category"), MSG_SALES_CAT_REQUIRED);

		List<?> picsList = parseNonEmptyIndexedJsonArray(mergedParams.get("pics"), MSG_PICS_FORMAT);
		mergedParams.put("pics", picsList);

		List<?> salesCats =
				parseNonEmptyIndexedJsonArray(mergedParams.get("item_category"), MSG_SALES_CAT_FORMAT);
		mergedParams.put("item_category", salesCats);

		mergedParams.put("company_id", companyId);
		mergedParams.put("item_type", "normal");
		mergedParams.put("special_type", "normal");
		mergedParams.put("item_source", "openapi");

		decodeItemParamsIfPresent(mergedParams);

		String oldItemBn = String.valueOf(mergedParams.get("old_item_bn"));
		try {
			Items defaultSku = itemsRepository.findDefaultByItemBnAndCompany(oldItemBn, companyId);
			if (defaultSku == null) {
				throw new IllegalStateException("");
			}
			if (nospecMismatch(defaultSku.getNospec(), mergedParams.get("nospec"))) {
				throw new IllegalStateException(MSG_NOSPEC_SWITCH);
			}
			long itemId = defaultSku.getItemId();
			mergedParams.put("item_id", itemId);
			mergedParams.remove("old_item_bn");
			itemsUpdateOrchestrator.updateItems(mergedParams, itemId, false);
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (ResourceException ex) {
			throw toUpdateFail(ex);
		} catch (Exception ex) {
			throw toUpdateFail(ex);
		}

		return Map.of("status", Boolean.TRUE);
	}

	private void decodeItemParamsIfPresent(Map<String, Object> mergedParams) {
		Object itemParamsRaw = mergedParams.get("item_params");
		if (itemParamsRaw instanceof String s && StringUtils.hasText(s)) {
			try {
				JsonNode node = objectMapper.readTree(s);
				mergedParams.put("item_params", objectMapper.convertValue(node, new TypeReference<Object>() {}));
			} catch (JsonProcessingException ex) {
				mergedParams.put("item_params", null);
			}
		}
	}

	private static void validateRequired(Object raw, String message) {
		if (raw == null || String.valueOf(raw).isEmpty()) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
		}
	}

	private List<?> parseNonEmptyIndexedJsonArray(Object raw, String formatMessage) {
		List<?> list = decodeToList(raw, formatMessage);
		if (list == null || list.isEmpty()) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_ERROR, formatMessage);
		}
		for (Object item : list) {
			if (item instanceof Map<?, ?> || item instanceof List<?>) {
				throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_ERROR, formatMessage);
			}
		}
		return list;
	}

	private List<?> decodeToList(Object raw, String formatMessage) {
		if (raw == null) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_ERROR, formatMessage);
		}
		if (raw instanceof List<?> list) {
			return list;
		}
		if (raw instanceof String s) {
			try {
				JsonNode node = objectMapper.readTree(s);
				if (!node.isArray()) {
					throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_ERROR, formatMessage);
				}
				return objectMapper.convertValue(node, new TypeReference<List<Object>>() {});
			} catch (JsonProcessingException ex) {
				throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_ERROR, formatMessage);
			}
		}
		throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_ERROR, formatMessage);
	}

	private static boolean nospecMismatch(String dbNospec, Object paramNospec) {
		String db = dbNospec == null ? "" : dbNospec;
		String param = paramNospec == null ? "" : String.valueOf(paramNospec);
		return !db.equals(param);
	}

	private static OpenapiItemsV2FailException toUpdateFail(Exception ex) {
		String msg = ex.getMessage();
		if (msg == null || msg.isEmpty()) {
			msg = MSG_GOODS_ERROR_FALLBACK;
		}
		return new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_ERROR, msg);
	}
}
