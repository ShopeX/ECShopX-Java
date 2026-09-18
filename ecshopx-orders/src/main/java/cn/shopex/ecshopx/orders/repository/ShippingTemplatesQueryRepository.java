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

package cn.shopex.ecshopx.orders.repository;

import cn.shopex.ecshopx.orders.domain.ShippingTemplates;
import cn.shopex.ecshopx.orders.domain.dto.ShippingTemplateRow;
import cn.shopex.ecshopx.orders.mapper.ShippingTemplatesMapper;
import cn.shopex.ecshopx.orders.service.shippingtemplate.OpenapiShippingTemplateListCriteria;
import cn.shopex.ecshopx.orders.service.shippingtemplate.ShippingTemplateAdminListCriteria;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ShippingTemplatesQueryRepository {

	private final ShippingTemplatesMapper mapper;

	public ShippingTemplatesQueryRepository(ShippingTemplatesMapper mapper) {
		this.mapper = mapper;
	}

	public Optional<ShippingTemplates> findByTemplateBusinessKey(
			String templateId, long companyId, long distributorId, long supplierId) {
		LambdaQueryWrapper<ShippingTemplates> w = new LambdaQueryWrapper<>();
		w.eq(ShippingTemplates::getTemplateId, templateId)
				.eq(ShippingTemplates::getCompanyId, companyId)
				.eq(ShippingTemplates::getDistributorId, distributorId)
				.eq(ShippingTemplates::getSupplierId, supplierId)
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public boolean existsByTemplateIdAndCompanyId(long templateId, long companyId) {
		if (templateId <= 0) {
			return false;
		}
		LambdaQueryWrapper<ShippingTemplates> w = new LambdaQueryWrapper<>();
		w.eq(ShippingTemplates::getTemplateId, templateId).eq(ShippingTemplates::getCompanyId, companyId);
		return mapper.selectCount(w) > 0;
	}

	public Optional<String> findTemplateName(long templateId, long companyId) {
		if (templateId <= 0) {
			return Optional.empty();
		}
		LambdaQueryWrapper<ShippingTemplates> w = new LambdaQueryWrapper<>();
		w.eq(ShippingTemplates::getTemplateId, templateId).eq(ShippingTemplates::getCompanyId, companyId).last("LIMIT 1");
		ShippingTemplates row = mapper.selectOne(w);
		if (row == null || row.getName() == null) {
			return Optional.empty();
		}
		return Optional.of(row.getName());
	}

	public Optional<Long> findTemplateIdByNameCompanySupplierAndDistributor(
			long companyId, String name, long supplierId, long distributorId) {
		if (name == null || name.isBlank()) {
			return Optional.empty();
		}
		LambdaQueryWrapper<ShippingTemplates> w = new LambdaQueryWrapper<>();
		w.eq(ShippingTemplates::getCompanyId, companyId)
				.eq(ShippingTemplates::getName, name.trim())
				.eq(ShippingTemplates::getSupplierId, supplierId)
				.eq(ShippingTemplates::getDistributorId, distributorId)
				.last("LIMIT 1");
		ShippingTemplates t = mapper.selectOne(w);
		if (t == null || t.getTemplateId() == null) {
			return Optional.empty();
		}
		return Optional.of(t.getTemplateId());
	}

	public Optional<ShippingTemplates> findByTemplateIdAndCompanyId(long companyId, long templateId) {
		LambdaQueryWrapper<ShippingTemplates> w = new LambdaQueryWrapper<>();
		w.eq(ShippingTemplates::getTemplateId, templateId)
				.eq(ShippingTemplates::getCompanyId, companyId)
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public long countByCriteria(ShippingTemplateAdminListCriteria c) {
		LambdaQueryWrapper<ShippingTemplates> w = buildListCriteriaWrapper(c);
		Long cnt = mapper.selectCount(w);
		return cnt == null ? 0L : cnt.longValue();
	}

	public List<ShippingTemplates> selectPageByCriteria(
			ShippingTemplateAdminListCriteria c, long offset, int limit) {
		LambdaQueryWrapper<ShippingTemplates> w = buildListCriteriaWrapper(c);
		w.orderByDesc(ShippingTemplates::getCreateTime);
		return selectListWithOffsetLimit(w, offset, limit);
	}

	public long countByOpenApiCriteria(OpenapiShippingTemplateListCriteria c) {
		LambdaQueryWrapper<ShippingTemplates> w = buildOpenApiListWrapper(c);
		Long cnt = mapper.selectCount(w);
		return cnt == null ? 0L : cnt.longValue();
	}

	public List<ShippingTemplates> selectPageByOpenApiCriteria(
			OpenapiShippingTemplateListCriteria c, long offset, int limit) {
		LambdaQueryWrapper<ShippingTemplates> w = buildOpenApiListWrapper(c);
		w.orderByDesc(ShippingTemplates::getCreateTime);
		return selectListWithOffsetLimit(w, offset, limit);
	}

	private List<ShippingTemplates> selectListWithOffsetLimit(
			LambdaQueryWrapper<ShippingTemplates> w, long offset, int limit) {
		if (limit < 1) {
			w.last("LIMIT 0");
		} else if (offset < 0L) {
			w.last("LIMIT " + limit);
		} else {
			w.last("LIMIT " + offset + "," + limit);
		}
		return mapper.selectList(w);
	}

	private static LambdaQueryWrapper<ShippingTemplates> buildOpenApiListWrapper(
			OpenapiShippingTemplateListCriteria c) {
		LambdaQueryWrapper<ShippingTemplates> w = new LambdaQueryWrapper<>();
		w.eq(ShippingTemplates::getCompanyId, c.companyId())
				.eq(ShippingTemplates::getDistributorId, c.distributorId());
		return w;
	}

	private static LambdaQueryWrapper<ShippingTemplates> buildListCriteriaWrapper(
			ShippingTemplateAdminListCriteria c) {
		LambdaQueryWrapper<ShippingTemplates> w = new LambdaQueryWrapper<>();
		w.eq(ShippingTemplates::getCompanyId, c.companyId());
		if (c.restrictStatusEnabled()) {
			w.eq(ShippingTemplates::getStatus, Boolean.TRUE);
		}
		c.isFreeEq().ifPresent(v -> w.eq(ShippingTemplates::getIsFree, v));
		c.valuationEq().ifPresent(v -> w.eq(ShippingTemplates::getValuation, v));
		w.eq(ShippingTemplates::getSupplierId, c.supplierId());
		if (c.applyDistributorEq()) {
			w.eq(ShippingTemplates::getDistributorId, c.distributorEq());
		}
		return w;
	}

	public Optional<ShippingTemplateRow> selectTemplateById(long companyId, long templatesId) {
		if (templatesId <= 0L) {
			return Optional.empty();
		}
		LambdaQueryWrapper<ShippingTemplates> w = new LambdaQueryWrapper<>();
		w.eq(ShippingTemplates::getTemplateId, templatesId)
				.eq(ShippingTemplates::getCompanyId, companyId)
				.last("LIMIT 1");
		ShippingTemplates t = mapper.selectOne(w);
		if (t == null) {
			return Optional.empty();
		}
		ShippingTemplateRow row = new ShippingTemplateRow();
		row.setTemplateId(t.getTemplateId());
		row.setCompanyId(t.getCompanyId());
		row.setSupplierId(t.getSupplierId());
		row.setIsFree(t.getIsFree());
		row.setValuation(t.getValuation());
		row.setStatus(t.getStatus());
		row.setNopostConf(t.getNopostConf());
		row.setFreeConf(t.getFreeConf());
		row.setFeeConf(t.getFeeConf());
		return Optional.of(row);
	}
}
