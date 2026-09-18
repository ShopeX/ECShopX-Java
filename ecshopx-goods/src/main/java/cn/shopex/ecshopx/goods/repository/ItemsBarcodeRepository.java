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

package cn.shopex.ecshopx.goods.repository;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsBarcode;
import cn.shopex.ecshopx.goods.mapper.ItemsBarcodeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class ItemsBarcodeRepository {

	private final ItemsBarcodeMapper mapper;

	public ItemsBarcodeRepository(ItemsBarcodeMapper mapper) {
		this.mapper = mapper;
	}

	public void deleteByItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<ItemsBarcode> w = new LambdaQueryWrapper<>();
		w.eq(ItemsBarcode::getCompanyId, companyId).in(ItemsBarcode::getItemId, itemIds);
		mapper.delete(w);
	}

	/** 店铺维度扩展条码：主键 (company_id, distributor_id, barcode)。 */
	public List<ItemsBarcode> listByCompanyIdAndDistributorIdAndBarcode(
			long companyId, long distributorId, String barcode) {
		if (barcode == null) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsBarcode> w = new LambdaQueryWrapper<>();
		w.eq(ItemsBarcode::getCompanyId, companyId)
				.eq(ItemsBarcode::getDistributorId, distributorId)
				.eq(ItemsBarcode::getBarcode, barcode);
		return mapper.selectList(w);
	}

	public ItemsBarcode findFirstByCompanyIdAndDistributorIdAndBarcode(
			long companyId, long distributorId, String barcode) {
		if (barcode == null) {
			return null;
		}
		LambdaQueryWrapper<ItemsBarcode> w = new LambdaQueryWrapper<>();
		w.eq(ItemsBarcode::getCompanyId, companyId)
				.eq(ItemsBarcode::getDistributorId, distributorId)
				.eq(ItemsBarcode::getBarcode, barcode)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	/**
	 * 覆盖写入 SKU 条码行：先按 item_id 删除旧行，再按店铺维度校验唯一后插入。
	 * 同一店铺内 barcode 不可与其它 SKU 重复；不同店铺可共用。
	 */
	public void saveBarcode(
			long companyId, long distributorId, long itemId, long defaultItemId, String barcodeCsv) {
		deleteByItemIds(companyId, List.of(itemId));
		if (barcodeCsv == null || barcodeCsv.isBlank()) {
			return;
		}
		String norm = barcodeCsv.replace('，', ',');
		Set<String> uniq = new LinkedHashSet<>();
		for (String p : norm.split(",")) {
			String t = p.trim();
			if (!t.isEmpty()) {
				uniq.add(t);
			}
		}
		for (String code : uniq) {
			LambdaQueryWrapper<ItemsBarcode> w = new LambdaQueryWrapper<>();
			w.eq(ItemsBarcode::getCompanyId, companyId)
					.eq(ItemsBarcode::getDistributorId, distributorId)
					.eq(ItemsBarcode::getBarcode, code)
					.ne(ItemsBarcode::getItemId, itemId);
			if (mapper.selectCount(w) > 0) {
				throw new ResourceException("条形码已经存在");
			}
			ItemsBarcode row = new ItemsBarcode();
			row.setItemId(itemId);
			row.setDefaultItemId(defaultItemId);
			row.setCompanyId(companyId);
			row.setDistributorId(distributorId);
			row.setBarcode(code);
			mapper.insert(row);
		}
	}
}
