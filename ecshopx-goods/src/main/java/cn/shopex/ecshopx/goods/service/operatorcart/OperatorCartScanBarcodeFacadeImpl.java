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

package cn.shopex.ecshopx.goods.service.operatorcart;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartScanBarcodeFacade;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsBarcode;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperatorCartScanBarcodeFacadeImpl implements OperatorCartScanBarcodeFacade {

	private final ItemsBarcodeRepository itemsBarcodeRepository;
	private final ItemsRepository itemsRepository;
	private final ItemsMapper itemsMapper;

	public OperatorCartScanBarcodeFacadeImpl(
			ItemsBarcodeRepository itemsBarcodeRepository,
			ItemsRepository itemsRepository,
			ItemsMapper itemsMapper) {
		this.itemsBarcodeRepository = itemsBarcodeRepository;
		this.itemsRepository = itemsRepository;
		this.itemsMapper = itemsMapper;
	}

	@Override
	public long resolveItemIdForOperatorScan(long companyId, long distributorId, String barcode) {
		if (barcode == null || barcode.isBlank()) {
			throw new ResourceException("商品找不到");
		}
		Long itemId = resolveFromDistributorScope(companyId, distributorId, barcode);
		if (itemId == null && distributorId > 0L) {
			itemId = resolveFromPlatformCatalog(companyId, barcode);
		}
		if (itemId == null || itemId <= 0L) {
			throw new ResourceException("商品找不到");
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).eq(Items::getItemId, itemId);
		Items it = itemsMapper.selectOne(w);
		if (it == null) {
			throw new ResourceException("商品找不到");
		}
		return itemId;
	}

	/** 门店扩展码 → 门店自有 SKU 主条码。 */
	private Long resolveFromDistributorScope(long companyId, long distributorId, String barcode) {
		Long fromExt = singleItemIdFromBarcodes(
				itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(
						companyId, distributorId, barcode));
		if (fromExt != null) {
			return fromExt;
		}
		return singleItemIdFromItems(
				itemsRepository.listByCompanyIdAndDistributorIdAndBarcodeExact(
						companyId, distributorId, barcode));
	}

	/**
	 * standard 平台建品：条码落在 distributor_id=0 的 items / items_barcode；门店仅挂关联。
	 */
	private Long resolveFromPlatformCatalog(long companyId, String barcode) {
		Long fromExt =
				singleItemIdFromBarcodes(
						itemsBarcodeRepository.listByCompanyIdAndDistributorIdAndBarcode(
								companyId, 0L, barcode));
		if (fromExt != null) {
			return fromExt;
		}
		List<Items> platform = new ArrayList<>();
		for (Items row : nullToEmpty(itemsRepository.listByCompanyIdAndBarcodeExact(companyId, barcode))) {
			Integer did = row.getDistributorId();
			if (did == null || did <= 0) {
				platform.add(row);
			}
		}
		return singleItemIdFromItems(platform);
	}

	private static Long singleItemIdFromBarcodes(List<ItemsBarcode> rows) {
		List<ItemsBarcode> list = nullToEmpty(rows);
		if (list.size() > 1) {
			throw new ResourceException("商品数据异常");
		}
		if (list.isEmpty()) {
			return null;
		}
		Long itemId = list.get(0).getItemId();
		if (itemId == null || itemId <= 0L) {
			throw new ResourceException("商品数据异常");
		}
		return itemId;
	}

	private static Long singleItemIdFromItems(List<Items> rows) {
		List<Items> list = nullToEmpty(rows);
		if (list.size() > 1) {
			throw new ResourceException("商品数据异常");
		}
		if (list.isEmpty()) {
			return null;
		}
		Long itemId = list.get(0).getItemId();
		if (itemId == null || itemId <= 0L) {
			throw new ResourceException("商品数据异常");
		}
		return itemId;
	}

	private static <T> List<T> nullToEmpty(List<T> rows) {
		return rows == null ? List.of() : rows;
	}
}
