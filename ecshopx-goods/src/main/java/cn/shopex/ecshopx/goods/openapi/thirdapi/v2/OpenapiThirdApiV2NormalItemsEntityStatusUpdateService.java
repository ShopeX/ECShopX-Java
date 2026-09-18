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
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsBatchApproveStatusService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2NormalItemsEntityStatusUpdateService {

	private static final String MSG_ITEM_BN_REQUIRED = "商品货号必填";
	private static final String MSG_STATUS_REQUIRED = "状态必填,且必须是 onsale 或 instock ";
	private static final String MSG_ITEM_BN_FORMAT = "请填写正确的货号格式";
	private static final String MSG_ITEM_BN_MAX = "商品状态可更改的最大数量为1000";
	private static final String MSG_GOODS_NOT_FOUND = "商品找不到";

	private final ItemsRepository itemsRepository;
	private final GoodsItemsBatchApproveStatusService goodsItemsBatchApproveStatusService;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2NormalItemsEntityStatusUpdateService(
			ItemsRepository itemsRepository,
			GoodsItemsBatchApproveStatusService goodsItemsBatchApproveStatusService,
			ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.goodsItemsBatchApproveStatusService = goodsItemsBatchApproveStatusService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeBatchUpdateItemsStatus(long companyId, String itemBnRaw, String statusRaw) {
		validateRequiredParams(itemBnRaw, statusRaw);

		List<String> itemBnList = parseItemBnJsonArray(itemBnRaw.trim());
		String status = statusRaw.trim();

		List<Map<String, Object>> goodsIdRows =
				itemsRepository.listGoodsIdRowsByDefaultItemBnIn(companyId, itemBnList);
		if (goodsIdRows.isEmpty()) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_NOT_FOUND, MSG_GOODS_NOT_FOUND);
		}

		try {
			goodsItemsBatchApproveStatusService.updateItemsStatusForOpenapi(companyId, goodsIdRows, status);
		} catch (ResourceException ex) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.SUCCESS, ex.getMessage());
		}

		return Map.of("status", true);
	}

	private static void validateRequiredParams(String itemBnRaw, String statusRaw) {
		if (itemBnRaw == null || itemBnRaw.trim().isEmpty()) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_ITEM_BN_REQUIRED);
		}
		if (statusRaw == null || statusRaw.trim().isEmpty()
				|| (!"onsale".equals(statusRaw.trim()) && !"instock".equals(statusRaw.trim()))) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_STATUS_REQUIRED);
		}
	}

	private List<String> parseItemBnJsonArray(String itemBnRaw) {
		try {
			JsonNode node = objectMapper.readTree(itemBnRaw);
			if (node == null || !node.isArray()) {
				throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_ERROR, MSG_ITEM_BN_FORMAT);
			}
			List<String> out = new ArrayList<>();
			for (JsonNode el : node) {
				if (el.isNull()) {
					out.add("");
				} else if (el.isTextual()) {
					out.add(el.asText());
				} else {
					out.add(el.asText());
				}
			}
			if (out.size() > 1000) {
				throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_ERROR, MSG_ITEM_BN_MAX);
			}
			return out;
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_ERROR, MSG_ITEM_BN_FORMAT);
		}
	}
}
