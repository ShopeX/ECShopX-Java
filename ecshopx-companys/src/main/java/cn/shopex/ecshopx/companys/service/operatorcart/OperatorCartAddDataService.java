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

package cn.shopex.ecshopx.companys.service.operatorcart;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartSkuLoadFacade;
import cn.shopex.ecshopx.common.operatorcart.dto.OperatorCartSkuRowDto;
import cn.shopex.ecshopx.companys.domain.OperatorCart;
import cn.shopex.ecshopx.companys.mapper.OperatorCartMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatorCartAddDataService {

	private final OperatorCartMapper operatorCartMapper;
	private final OperatorCartSkuLoadFacade operatorCartSkuLoadFacade;
	private final OperatorCartCompanyProductModelReader productModelReader;

	public OperatorCartAddDataService(OperatorCartMapper operatorCartMapper,
			OperatorCartSkuLoadFacade operatorCartSkuLoadFacade,
			OperatorCartCompanyProductModelReader productModelReader) {
		this.operatorCartMapper = operatorCartMapper;
		this.operatorCartSkuLoadFacade = operatorCartSkuLoadFacade;
		this.productModelReader = productModelReader;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> addCartData(long companyId, long operatorId, long distributorId, long itemId, long num,
			boolean isChecked, boolean accumulate, boolean itemIdPresent, boolean distributorIdPresent) {
		validateMergedCartWriteParams(operatorId, companyId, distributorIdPresent, distributorId, itemIdPresent, itemId);

		String productModel = productModelReader.getProductModel(companyId);
		OperatorCartSkuRowDto sku =
				operatorCartSkuLoadFacade.resolveSkuForOperatorCartAdd(companyId, distributorId, itemId, productModel);
		if (sku.getStore() < num) {
			throw new ResourceException("库存不足");
		}
		String specialType = sku.getSpecialType() != null ? sku.getSpecialType() : "normal";

		LambdaQueryWrapper<OperatorCart> cw = new LambdaQueryWrapper<>();
		cw.eq(OperatorCart::getCompanyId, companyId).eq(OperatorCart::getOperatorId, operatorId)
				.eq(OperatorCart::getDistributorId, distributorId).eq(OperatorCart::getItemId, itemId);
		OperatorCart cart = operatorCartMapper.selectOne(cw);

		if (cart == null && num <= 0L) {
			throw new ResourceException("加入购物车的数据有误");
		}
		if (cart != null && num <= 0L) {
			operatorCartMapper.deleteById(cart.getCartId());
			return new LinkedHashMap<>();
		}
		if (cart != null) {
			long existingNum = cart.getNum() == null ? 0L : cart.getNum();
			long newNum = accumulate ? (num + existingNum) : num;
			cart.setNum(newNum);
			cart.setIsChecked(isChecked);
			cart.setSpecialType(specialType);
			int updated = operatorCartMapper.updateById(cart);
			if (updated == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			return toColumnMap(cart);
		}
		OperatorCart row = new OperatorCart();
		row.setCompanyId(companyId);
		row.setDistributorId(distributorId);
		row.setOperatorId(operatorId);
		row.setItemId(itemId);
		row.setNum(num);
		row.setIsChecked(isChecked);
		row.setSpecialType(specialType);
		operatorCartMapper.insert(row);
		return toColumnMap(row);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateCartData(long companyId, long operatorId, long distributorId, long cartId,
			long itemId, long num, boolean isChecked, boolean itemIdPresent, boolean distributorIdPresent) {
		validateMergedCartWriteParams(operatorId, companyId, distributorIdPresent, distributorId, itemIdPresent, itemId);

		String productModel = productModelReader.getProductModel(companyId);
		OperatorCartSkuRowDto sku =
				operatorCartSkuLoadFacade.resolveSkuForOperatorCartAdd(companyId, distributorId, itemId, productModel);
		if (sku.getStore() < num) {
			throw new ResourceException("库存不足");
		}
		String specialType = sku.getSpecialType() != null ? sku.getSpecialType() : "normal";

		LambdaQueryWrapper<OperatorCart> cw = new LambdaQueryWrapper<>();
		cw.eq(OperatorCart::getCartId, cartId).eq(OperatorCart::getCompanyId, companyId)
				.eq(OperatorCart::getOperatorId, operatorId).eq(OperatorCart::getDistributorId, distributorId)
				.eq(OperatorCart::getItemId, itemId);
		OperatorCart cart = operatorCartMapper.selectOne(cw);

		if (cart == null || num <= 0L) {
			throw new ResourceException("更新购物车的数据有误");
		}

		cart.setNum(num);
		cart.setIsChecked(isChecked);
		cart.setSpecialType(specialType);
		int updated = operatorCartMapper.updateById(cart);
		if (updated == 0) {
			throw new ResourceException("未查询到更新数据");
		}
		return toColumnMap(cart);
	}

	private void validateMergedCartWriteParams(long operatorId, long companyId, boolean distributorIdPresent,
			long distributorId, boolean itemIdPresent, long itemId) {
		if (operatorId <= 0L) {
			throw new BadRequestException("管理员信息有误");
		}
		if (!distributorIdPresent) {
			throw new BadRequestException("店铺信息有误");
		}
		if (companyId <= 0L) {
			throw new BadRequestException("企业信息有误");
		}
		if (!itemIdPresent) {
			throw new BadRequestException("购物车商品有误");
		}
	}

	private static Map<String, Object> toColumnMap(OperatorCart row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("cart_id", row.getCartId());
		m.put("company_id", row.getCompanyId());
		m.put("distributor_id", row.getDistributorId());
		m.put("operator_id", row.getOperatorId());
		m.put("item_id", row.getItemId());
		m.put("num", row.getNum());
		m.put("is_checked", row.getIsChecked());
		m.put("special_type", row.getSpecialType());
		return m;
	}
}
