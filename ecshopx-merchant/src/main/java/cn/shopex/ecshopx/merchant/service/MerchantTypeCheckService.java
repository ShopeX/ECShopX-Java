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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.domain.MerchantType;
import cn.shopex.ecshopx.merchant.mapper.MerchantTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

@Service
public class MerchantTypeCheckService {

	private final MerchantTypeMapper merchantTypeMapper;

	public MerchantTypeCheckService(MerchantTypeMapper merchantTypeMapper) {
		this.merchantTypeMapper = merchantTypeMapper;
	}

	public void checkMerchantType(long companyId, long typeId) {
		MerchantType type = merchantTypeMapper.selectOne(new LambdaQueryWrapper<MerchantType>()
				.eq(MerchantType::getCompanyId, companyId)
				.eq(MerchantType::getId, typeId)
				.apply("is_show = {0}", 1));
		if (type == null) {
			throw new ResourceException("经营范围错误，请确认后重新提交");
		}
		Integer level = type.getLevel();
		if (level != null && level == 1) {
			long childCount = merchantTypeMapper.selectCount(new LambdaQueryWrapper<MerchantType>()
					.eq(MerchantType::getCompanyId, companyId)
					.eq(MerchantType::getParentId, type.getId())
					.apply("is_show = {0}", 1));
			if (childCount > 0) {
				throw new ResourceException("经营范围错误，请确认后重新提交");
			}
		}
	}
}
