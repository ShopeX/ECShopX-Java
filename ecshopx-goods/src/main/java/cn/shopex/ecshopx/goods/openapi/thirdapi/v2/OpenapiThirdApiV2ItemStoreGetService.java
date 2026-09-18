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

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2ItemStoreGetService {

	private static final String MSG_DISTRIBUTOR_NOT_FOUND = "店铺找不到";
	private static final String MSG_GOODS_NOT_FOUND = "商品找不到";
	private static final String MSG_UNKNOWN_ERROR = "未知错误";

	private final ItemsRepository itemsRepository;
	private final DistributorItemsRepository distributorItemsRepository;
	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;

	public OpenapiThirdApiV2ItemStoreGetService(
			ItemsRepository itemsRepository,
			DistributorItemsRepository distributorItemsRepository,
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver) {
		this.itemsRepository = itemsRepository;
		this.distributorItemsRepository = distributorItemsRepository;
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
	}

	public Map<String, Object> executeGetStoreDetail(long companyId, Map<String, Object> mergedParams) {
		try {
			String itemCodeRaw = mergedParams.containsKey("item_code")
					? String.valueOf(mergedParams.get("item_code")) : null;
			String distributorCodeRaw = mergedParams.containsKey("distributor_code")
					? String.valueOf(mergedParams.get("distributor_code")) : null;

			Items itemInfo = resolveItemInfo(companyId, itemCodeRaw);
			if (itemInfo == null || itemInfo.getItemId() == null) {
				throw new OpenapiItemsV2FailException(OpenapiErrorCode.GOODS_NOT_FOUND, MSG_GOODS_NOT_FOUND);
			}
			long itemId = itemInfo.getItemId();

			LinkedHashMap<String, Object> result = new LinkedHashMap<>();
			result.put("item_code", String.valueOf(itemInfo.getItemBn()));
			result.put("item_name", String.valueOf(itemInfo.getItemName()));
			result.put("store", toIntStore(itemInfo.getStore()));

			String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
			if (StringUtils.hasText(distributorCodeRaw) && "standard".equals(productModel)) {
				appendDistributorBranch(result, companyId, distributorCodeRaw, itemId);
			}
			return result;
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw toGetFail(ex);
		}
	}

	private Items resolveItemInfo(long companyId, String itemCodeRaw) {
		if (StringUtils.hasText(itemCodeRaw)) {
			return itemsRepository.findByItemBnAndCompany(itemCodeRaw, companyId);
		}
		return itemsRepository.findFirstByCompanyId(companyId);
	}

	private void appendDistributorBranch(
			LinkedHashMap<String, Object> result,
			long companyId,
			String distributorCodeRaw,
			long itemId) {
		Map<String, Object> distInfo =
				distributorRepositoryGetInfoSimpleService.getInfoSimpleByShopCode(companyId, distributorCodeRaw);
		if (distInfo == null || distInfo.isEmpty()) {
			throw new OpenapiItemsV2FailException(OpenapiErrorCode.DISTRIBUTOR_NOT_FOUND, MSG_DISTRIBUTOR_NOT_FOUND);
		}

		Optional<DistributorItems> distributorItemOpt =
				distributorItemsRepository.findByDistributorIdIsNullAndCompanyIdAndItemId(companyId, itemId);
		if (distributorItemOpt.isPresent()) {
			DistributorItems row = distributorItemOpt.get();
			Long storeVal = row.getStore();
			result.put("store", toIntStore(storeVal != null ? storeVal.intValue() : null));
		}

		Object shopCode = distInfo.get("shop_code");
		Object name = distInfo.get("name");
		result.put("distributor_code", shopCode != null ? String.valueOf(shopCode) : "");
		result.put("distributor_name", name != null ? String.valueOf(name) : "");
	}

	private static int toIntStore(Integer store) {
		return store != null ? store : 0;
	}

	private static OpenapiItemsV2FailException toGetFail(Exception ex) {
		String msg = ex.getMessage();
		if (msg == null || msg.isBlank()) {
			msg = MSG_UNKNOWN_ERROR;
		}
		return new OpenapiItemsV2FailException("E5000", msg);
	}
}
