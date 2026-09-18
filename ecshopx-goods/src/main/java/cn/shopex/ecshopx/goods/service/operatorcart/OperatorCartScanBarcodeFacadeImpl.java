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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class OperatorCartScanBarcodeFacadeImpl implements OperatorCartScanBarcodeFacade {

	private final ItemsBarcodeRepository itemsBarcodeRepository;
	private final ItemsMapper itemsMapper;

	public OperatorCartScanBarcodeFacadeImpl(ItemsBarcodeRepository itemsBarcodeRepository, ItemsMapper itemsMapper) {
		this.itemsBarcodeRepository = itemsBarcodeRepository;
		this.itemsMapper = itemsMapper;
	}

	@Override
	public long resolveItemIdForOperatorScan(long companyId, long distributorId, String barcode) {
		if (barcode == null || barcode.isBlank()) {
			throw new ResourceException("商品找不到");
		}
		ItemsBarcode row =
				itemsBarcodeRepository.findFirstByCompanyIdAndDistributorIdAndBarcode(companyId, distributorId, barcode);
		if (row == null) {
			throw new ResourceException("商品找不到");
		}
		Long itemIdObj = row.getItemId();
		if (itemIdObj == null || itemIdObj <= 0L) {
			throw new ResourceException("商品找不到");
		}
		long itemId = itemIdObj;
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).eq(Items::getItemId, itemId);
		Items it = itemsMapper.selectOne(w);
		if (it == null) {
			throw new ResourceException("商品找不到");
		}
		return itemId;
	}
}
