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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NormalGoodsStoreImportRowService {

	private static final int STORE_MIN = 0;
	private static final int STORE_MAX = 999999999;

	private final ItemsRepository itemsRepository;
	private final DistributorItemsRepository distributorItemsRepository;
	private final DistributorMapper distributorMapper;
	private final ItemStoreService itemStoreService;
	private final OperatorCartCompanyProductModelReader productModelReader;

	public NormalGoodsStoreImportRowService(
			ItemsRepository itemsRepository,
			DistributorItemsRepository distributorItemsRepository,
			DistributorMapper distributorMapper,
			ItemStoreService itemStoreService,
			OperatorCartCompanyProductModelReader productModelReader) {
		this.itemsRepository = itemsRepository;
		this.distributorItemsRepository = distributorItemsRepository;
		this.distributorMapper = distributorMapper;
		this.itemStoreService = itemStoreService;
		this.productModelReader = productModelReader;
	}

	@Transactional(rollbackFor = Exception.class)
	public void applyRow(long companyId, Map<String, Object> row) {
		Map<String, String> r = trimRow(row);
		String didRaw = r.get("did");
		String itemBn = r.get("item_bn");
		String storeRaw = r.get("store");
		if (!StringUtils.hasText(didRaw)) {
			throw new BadRequestException("请填写店铺ID");
		}
		if (!StringUtils.hasText(itemBn)) {
			throw new BadRequestException("请填写商品编码");
		}
		if (!StringUtils.hasText(storeRaw) || !isStrictIntString(storeRaw)) {
			throw new BadRequestException("库存为0-999999999的整数");
		}
		int store = Integer.parseInt(storeRaw.trim());
		if (store < STORE_MIN || store > STORE_MAX) {
			throw new BadRequestException("库存为0-999999999的整数");
		}

		long did = parseLongOrZero(didRaw);
		long rowDistributorId = parseLongOrZero(r.get("distributor_id"));
		long rowMerchantId = parseLongOrZero(r.get("merchant_id"));
		String productModel = productModelReader.getProductModel(companyId);

		Items itemInfo;
		if (did > 0L && "platform".equals(productModel)) {
			itemInfo = itemsRepository.findByItemBnAndCompanyAndDistributorId(itemBn, companyId, did);
		} else {
			itemInfo = itemsRepository.findByItemBnAndCompany(itemBn, companyId);
		}
		if (itemInfo == null || itemInfo.getItemId() == null || itemInfo.getItemId() <= 0L) {
			throw new BadRequestException("商品不存在");
		}
		if (rowDistributorId > 0L && did != rowDistributorId) {
			throw new BadRequestException("只能导入所属店铺的商品库存");
		}
		if (rowMerchantId > 0L) {
			if (did == 0L) {
				throw new BadRequestException("只能导入所属经销商关联店铺的商品库存");
			}
			Long count = distributorMapper.selectCount(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.eq(Distributor::getMerchantId, rowMerchantId)
					.eq(Distributor::getDistributorId, did));
			if (count == null || count <= 0L) {
				throw new BadRequestException("只能导入所属经销商关联店铺的商品库存");
			}
		}

		long itemId = itemInfo.getItemId();
		if (did > 0L && "standard".equals(productModel)) {
			Optional<DistributorItems> existingOpt =
					distributorItemsRepository.findByDistributorIdAndCompanyIdAndItemId(did, companyId, itemId);
			boolean isTotalStore;
			if (existingOpt.isPresent()) {
				Boolean flag = existingOpt.get().getIsTotalStore();
				isTotalStore = flag == null || Boolean.TRUE.equals(flag);
			} else {
				long itemOwnDid = itemInfo.getDistributorId() != null ? itemInfo.getDistributorId() : 0L;
				if (itemOwnDid == 0L || itemOwnDid != did) {
					throw new BadRequestException("店铺商品不存在");
				}
				isTotalStore = true;
			}
			if (isTotalStore) {
				throw new BadRequestException("门店库存为总部库存");
			}
			distributorItemsRepository.updateColumnsByDistributorCompanyItem(
					did, companyId, itemId, null, null, (long) store, null);
			itemStoreService.saveItemStore(itemId, store, did);
			return;
		}

		itemsRepository.updateSingleItemStoreIfExists(itemId, store);
		itemStoreService.saveItemStore(itemId, store, 0L);
	}

	private static boolean isStrictIntString(String s) {
		String t = s.trim();
		if (t.isEmpty()) {
			return false;
		}
		for (int i = 0; i < t.length(); i++) {
			if (!Character.isDigit(t.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static long parseLongOrZero(String s) {
		if (s == null || s.isBlank()) {
			return 0L;
		}
		try {
			return new BigDecimal(s.trim()).longValue();
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, String> trimRow(Map<String, Object> row) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : row.entrySet()) {
			Object v = e.getValue();
			out.put(e.getKey(), v == null ? "" : String.valueOf(v).trim());
		}
		return out;
	}
}
