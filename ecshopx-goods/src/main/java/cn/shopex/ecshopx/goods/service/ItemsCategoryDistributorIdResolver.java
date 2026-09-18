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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.goods.service.dto.CategoryTreeNode;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 按分类节点与商户经营模式、JWT 上下文解析待写入的 distributor_id。 */
@Service
public class ItemsCategoryDistributorIdResolver {

	private static final Map<Integer, String> MENU_TYPE_TO_STR = new HashMap<>();

	static {
		MENU_TYPE_TO_STR.put(1, "all");
		MENU_TYPE_TO_STR.put(2, "b2c");
		MENU_TYPE_TO_STR.put(3, "platform");
		MENU_TYPE_TO_STR.put(4, "standard");
		MENU_TYPE_TO_STR.put(5, "in_purchase");
	}

	private final CompanysMapper companysMapper;

	@Value("${common.product-model:platform}")
	private String defaultProductModel;

	public ItemsCategoryDistributorIdResolver(CompanysMapper companysMapper) {
		this.companysMapper = companysMapper;
	}

	public long resolveForCategoryNode(CategoryTreeNode node, long companyId, long jwtDistributorId) {
		if (node.isMergedCategoryIdKeyPresent() || node.isMergedParentIdKeyPresent()) {
			return node.getPendingDistributorIdBeforeResolve();
		}
		if (Boolean.TRUE.equals(node.getIsMainCategory())) {
			return 0L;
		}
		long distributorId = 0L;
		if ("platform".equals(privateResolveProductModel(companyId))) {
			if (jwtDistributorId != 0L) {
				distributorId = jwtDistributorId;
			}
		}
		long pending = node.getPendingDistributorIdBeforeResolve();
		return distributorId != 0L ? distributorId : pending;
	}

	public String resolveProductModel(long companyId) {
		return privateResolveProductModel(companyId);
	}

	/**
	 * 平面创建分类时解析写库 {@code distributor_id}，与同模块树形保存中 {@link #resolveForCategoryNode} 在无合并键时的规则一致。
	 */
	public long resolveForClassification(boolean isMainCategory, long companyId, long jwtDistributorId) {
		if (isMainCategory) {
			return 0L;
		}
		long distributorId = 0L;
		if ("platform".equals(privateResolveProductModel(companyId))) {
			if (jwtDistributorId != 0L) {
				distributorId = jwtDistributorId;
			}
		}
		long pending = jwtDistributorId;
		return distributorId != 0L ? distributorId : pending;
	}

	private String privateResolveProductModel(long companyId) {
		Companys c = companysMapper.selectById(companyId);
		if (c == null || c.getMenuType() == null || c.getMenuType() == 0) {
			return defaultProductModel != null ? defaultProductModel : "platform";
		}
		return MENU_TYPE_TO_STR.getOrDefault(c.getMenuType(), defaultProductModel);
	}
}
