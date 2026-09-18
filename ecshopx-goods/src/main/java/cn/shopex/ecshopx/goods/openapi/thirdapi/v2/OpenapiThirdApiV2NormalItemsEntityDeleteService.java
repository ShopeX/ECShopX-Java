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
import cn.shopex.ecshopx.goods.service.items.PlatformItemsDeleteService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2NormalItemsEntityDeleteService {

	private static final String MSG_ITEM_BN_REQUIRED = "商品货号必填";
	private static final String MSG_GOODS_DELETE_ERROR = "商品删除错误";

	private final ItemsRepository itemsRepository;
	private final PlatformItemsDeleteService platformItemsDeleteService;

	public OpenapiThirdApiV2NormalItemsEntityDeleteService(
			ItemsRepository itemsRepository,
			PlatformItemsDeleteService platformItemsDeleteService) {
		this.itemsRepository = itemsRepository;
		this.platformItemsDeleteService = platformItemsDeleteService;
	}

	public Map<String, Object> executeDeleteItems(long companyId, String itemBnRaw) {
		validateItemBn(itemBnRaw);
		String itemBn = itemBnRaw.trim();

		Items defaultSku = itemsRepository.findDefaultByItemBnAndCompany(itemBn, companyId);
		if (defaultSku == null || defaultSku.getItemId() == null) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_DELETE_ERROR, MSG_GOODS_DELETE_ERROR);
		}
		long itemId = defaultSku.getItemId();

		try {
			platformItemsDeleteService.deletePlatformItems(
					Map.of("company_id", companyId),
					itemId,
					0L);
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (ResourceException ex) {
			throw toDeleteFail(ex);
		} catch (Exception ex) {
			throw toDeleteFail(ex);
		}

		return Map.of("status", Boolean.TRUE);
	}

	private static void validateItemBn(String itemBnRaw) {
		if (itemBnRaw == null || itemBnRaw.trim().isEmpty()) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, MSG_ITEM_BN_REQUIRED);
		}
	}

	private static OpenapiItemsV2FailException toDeleteFail(Exception ex) {
		String msg = ex.getMessage();
		if (msg == null || msg.isBlank()) {
			msg = MSG_GOODS_DELETE_ERROR;
		}
		return new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_DELETE_ERROR, msg);
	}
}
