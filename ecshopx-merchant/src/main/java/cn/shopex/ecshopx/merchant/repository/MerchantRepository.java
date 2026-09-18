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

package cn.shopex.ecshopx.merchant.repository;

import cn.shopex.ecshopx.merchant.domain.Merchant;
import cn.shopex.ecshopx.merchant.mapper.MerchantMapper;
import cn.shopex.ecshopx.merchant.service.MerchantListFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class MerchantRepository {

	private final MerchantMapper merchantMapper;

	public MerchantRepository(MerchantMapper merchantMapper) {
		this.merchantMapper = merchantMapper;
	}

	public long countForList(long companyId, MerchantListFilter filter) {
		return merchantMapper.selectCount(listWhere(companyId, filter));
	}

	public List<Merchant> selectPageForList(long companyId, MerchantListFilter filter, long offset, int limit) {
		if (limit <= 0) {
			return List.of();
		}
		LambdaQueryWrapper<Merchant> w = listWhere(companyId, filter);
		w.orderByDesc(Merchant::getCreated);
		w.last("LIMIT " + limit + " OFFSET " + Math.max(0, offset));
		return merchantMapper.selectList(w);
	}

	private static LambdaQueryWrapper<Merchant> listWhere(long companyId, MerchantListFilter filter) {
		LambdaQueryWrapper<Merchant> w = new LambdaQueryWrapper<>();
		w.eq(Merchant::getCompanyId, companyId);
		String merchantName = filter.getMerchantNameContainsOrNull();
		if (StringUtils.hasText(merchantName)) {
			String escaped = escapeSqlLike(merchantName.trim());
			w.like(Merchant::getMerchantName, "%" + escaped + "%");
		}
		if (StringUtils.hasText(filter.getLegalNameEncryptedOrNull())) {
			w.eq(Merchant::getLegalName, filter.getLegalNameEncryptedOrNull());
		}
		if (StringUtils.hasText(filter.getLegalMobileEncryptedOrNull())) {
			w.eq(Merchant::getLegalMobile, filter.getLegalMobileEncryptedOrNull());
		}
		if (filter.getCreatedGteOrNull() != null) {
			w.ge(Merchant::getCreated, filter.getCreatedGteOrNull());
		}
		if (filter.getCreatedLteOrNull() != null) {
			w.le(Merchant::getCreated, filter.getCreatedLteOrNull());
		}
		return w;
	}

	private static String escapeSqlLike(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	public Merchant getInfo(Long companyId, Long merchantId, boolean disabled) {
		if (companyId == null || merchantId == null || merchantId <= 0) {
			return null;
		}
		LambdaQueryWrapper<Merchant> w = new LambdaQueryWrapper<>();
		w.eq(Merchant::getCompanyId, companyId).eq(Merchant::getId, merchantId).eq(Merchant::isDisabled, disabled);
		return merchantMapper.selectOne(w);
	}

	public Merchant findById(long id) {
		return merchantMapper.selectById(id);
	}

	public Merchant findByCompanyIdAndSettlementApplyId(long companyId, long settlementApplyId) {
		LambdaQueryWrapper<Merchant> w = new LambdaQueryWrapper<>();
		w.eq(Merchant::getCompanyId, companyId).eq(Merchant::getSettlementApplyId, settlementApplyId);
		return merchantMapper.selectOne(w);
	}

	public void updateAuditGoodsById(long id, boolean auditGoods) {
		LambdaUpdateWrapper<Merchant> w = new LambdaUpdateWrapper<>();
		w.eq(Merchant::getId, id).set(Merchant::isAuditGoods, auditGoods);
		merchantMapper.update(null, w);
	}

	public void updateDisabledById(long id, boolean disabled) {
		LambdaUpdateWrapper<Merchant> w = new LambdaUpdateWrapper<>();
		w.eq(Merchant::getId, id).set(Merchant::isDisabled, disabled);
		merchantMapper.update(null, w);
	}

	public boolean existsByCompanyIdAndMerchantTypeIdIn(long companyId, List<Long> merchantTypeIds) {
		if (merchantTypeIds == null || merchantTypeIds.isEmpty()) {
			return false;
		}
		LambdaQueryWrapper<Merchant> w = new LambdaQueryWrapper<>();
		w.eq(Merchant::getCompanyId, companyId).in(Merchant::getMerchantTypeId, merchantTypeIds);
		return merchantMapper.selectCount(w) > 0;
	}
}
