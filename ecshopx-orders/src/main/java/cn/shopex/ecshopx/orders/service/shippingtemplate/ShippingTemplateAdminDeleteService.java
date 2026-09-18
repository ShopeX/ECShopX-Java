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

package cn.shopex.ecshopx.orders.service.shippingtemplate;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import cn.shopex.ecshopx.orders.domain.ShippingTemplates;
import cn.shopex.ecshopx.orders.mapper.ShippingTemplateItemsRelationMapper;
import cn.shopex.ecshopx.orders.mapper.ShippingTemplatesMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ShippingTemplateAdminDeleteService {

	private final ShippingTemplateItemsRelationMapper shippingTemplateItemsRelationMapper;
	private final ShippingTemplatesMapper shippingTemplatesMapper;
	private final CommonLangModWriteService commonLangModWriteService;
	private final LangueProperties langueProperties;

	public ShippingTemplateAdminDeleteService(
			ShippingTemplateItemsRelationMapper shippingTemplateItemsRelationMapper,
			ShippingTemplatesMapper shippingTemplatesMapper,
			CommonLangModWriteService commonLangModWriteService,
			LangueProperties langueProperties) {
		this.shippingTemplateItemsRelationMapper = shippingTemplateItemsRelationMapper;
		this.shippingTemplatesMapper = shippingTemplatesMapper;
		this.commonLangModWriteService = commonLangModWriteService;
		this.langueProperties = langueProperties;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteShippingTemplates(
			String templateIdPath, long companyId, long distributorId, long supplierId) {
		String id = templateIdPath == null ? "" : templateIdPath.trim();

		long c = shippingTemplateItemsRelationMapper.countByTemplatesIdAndCompanyId(id, companyId);
		if (c > 0) {
			throw new ResourceException("有商品关联, 不能删除");
		}

		int pageNo = 1;
		for (;;) {
			Page<ShippingTemplates> page = new Page<>(pageNo, 500, false);
			LambdaQueryWrapper<ShippingTemplates> w = buildDeleteFilter(id, companyId, distributorId, supplierId);
			Page<ShippingTemplates> result = shippingTemplatesMapper.selectPage(page, w);
			List<ShippingTemplates> records = result.getRecords();
			if (records.isEmpty()) {
				break;
			}
			for (ShippingTemplates row : records) {
				Long tid = row.getTemplateId();
				if (tid == null) {
					throw new ResourceException("删除的数据不存在");
				}
				for (String lang : langueProperties.getList()) {
					if (StringUtils.hasText(lang)) {
						commonLangModWriteService.deleteLang(
								(int) companyId,
								"shipping_templates",
								tid.longValue(),
								"shipping_templates",
								lang);
					}
				}
			}
			pageNo++;
		}

		LambdaQueryWrapper<ShippingTemplates> del = buildDeleteFilter(id, companyId, distributorId, supplierId);
		int removed = shippingTemplatesMapper.delete(del);
		if (removed == 0) {
			throw new ResourceException("删除的数据不存在");
		}
	}

	private static LambdaQueryWrapper<ShippingTemplates> buildDeleteFilter(
			String id, long companyId, long distributorId, long supplierId) {
		LambdaQueryWrapper<ShippingTemplates> w = new LambdaQueryWrapper<>();
		w.eq(ShippingTemplates::getTemplateId, id)
				.eq(ShippingTemplates::getCompanyId, companyId)
				.eq(ShippingTemplates::getDistributorId, distributorId)
				.eq(ShippingTemplates::getSupplierId, supplierId);
		return w;
	}
}
