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

package cn.shopex.ecshopx.shopmenuborder.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.superadmin.domain.ShopMenu;
import cn.shopex.ecshopx.superadmin.domain.ShopMenuRelType;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuMapper;
import cn.shopex.ecshopx.superadmin.mapper.ShopMenuRelTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ShopMenuDeleteService {

	private final ShopMenuMapper shopMenuMapper;
	private final ShopMenuRelTypeMapper shopMenuRelTypeMapper;
	private final TransactionTemplate transactionTemplate;

	public ShopMenuDeleteService(
			ShopMenuMapper shopMenuMapper,
			ShopMenuRelTypeMapper shopMenuRelTypeMapper,
			PlatformTransactionManager transactionManager) {
		this.shopMenuMapper = shopMenuMapper;
		this.shopMenuRelTypeMapper = shopMenuRelTypeMapper;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public Map<String, Object> deleteMenus(String shopmenuId) {
		QueryWrapper<ShopMenu> childW = new QueryWrapper<>();
		childW.eq("pid", shopmenuId).eq("disabled", false);
		Long childCount = shopMenuMapper.selectCount(childW);
		if (childCount != null && childCount > 0) {
			throw new ResourceException("当前菜单还有子菜单，不可删除");
		}

		return transactionTemplate.execute(status -> {
			QueryWrapper<ShopMenuRelType> relDel = new QueryWrapper<>();
			relDel.eq("shopmenu_id", shopmenuId);
			shopMenuRelTypeMapper.delete(relDel);

			QueryWrapper<ShopMenu> menuDel = new QueryWrapper<>();
			menuDel.eq("shopmenu_id", shopmenuId);
			shopMenuMapper.delete(menuDel);

			return Map.of("status", Boolean.TRUE);
		});
	}
}
