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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ItemsTemplateWriteService {

	private final ItemsRepository itemsRepository;
	private final SupplierItemsRepository supplierItemsRepository;

	public ItemsTemplateWriteService(ItemsRepository itemsRepository, SupplierItemsRepository supplierItemsRepository) {
		this.itemsRepository = itemsRepository;
		this.supplierItemsRepository = supplierItemsRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public void setItemsTemplate(long companyId, boolean supplierOperator, int templatesId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		try {
			if (!supplierOperator) {
				for (long v : itemIds) {
					Items itemsInfo = itemsRepository.findByItemId(v);
					if (itemsInfo == null) {
						throw new ResourceException("请确认您的商品信息后再提交");
					}
					if (itemsInfo.getCompanyId() == null || itemsInfo.getCompanyId() != companyId) {
						throw new ResourceException("请确认您的商品信息后再提交");
					}
					itemsRepository.updateTemplatesIdByDefaultItemIdAndCompany(v, companyId, templatesId);
				}
			} else {
				for (long v : itemIds) {
					List<SupplierItems> rsItems = supplierItemsRepository.listByDefaultItemIdAndCompany(v, companyId);
					if (rsItems.isEmpty()) {
						throw new ResourceException("请确认您的商品信息后再提交");
					}
					if (rsItems.get(0).getCompanyId() == null || rsItems.get(0).getCompanyId() != companyId) {
						throw new ResourceException("没有权限修改该商品");
					}
					supplierItemsRepository.updateTemplatesIdByDefaultItemIdAndCompany(v, companyId, templatesId);
					List<Long> supplierSkuIds = rsItems.stream()
							.map(SupplierItems::getItemId)
							.filter(Objects::nonNull)
							.toList();
					if (supplierSkuIds.isEmpty()) {
						continue;
					}
					long cnt = itemsRepository.countBySupplierItemIds(supplierSkuIds);
					if (cnt > 0) {
						itemsRepository.updateTemplatesIdBySupplierItemIds(supplierSkuIds, templatesId);
					}
				}
			}
		} catch (DataAccessException | PersistenceException e) {
			log.warn("setItemsTemplate persistence error", e);
			throw new ResourceException("运费模板更新失败，请稍后重试");
		}
	}
}
