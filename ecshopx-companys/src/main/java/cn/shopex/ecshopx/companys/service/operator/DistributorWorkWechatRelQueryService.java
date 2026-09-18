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

package cn.shopex.ecshopx.companys.service.operator;

import cn.shopex.ecshopx.companys.domain.DistributorWorkWechatRel;
import cn.shopex.ecshopx.companys.mapper.DistributorWorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DistributorWorkWechatRelQueryService {

	private final DistributorWorkWechatRelMapper distributorWorkWechatRelMapper;

	public DistributorWorkWechatRelQueryService(DistributorWorkWechatRelMapper distributorWorkWechatRelMapper) {
		this.distributorWorkWechatRelMapper = distributorWorkWechatRelMapper;
	}

	public boolean existsByCompanyIdAndOperatorId(long companyId, long operatorId) {
		LambdaQueryWrapper<DistributorWorkWechatRel> w = new LambdaQueryWrapper<>();
		w.eq(DistributorWorkWechatRel::getCompanyId, companyId).eq(DistributorWorkWechatRel::getOperatorId, operatorId);
		Long n = distributorWorkWechatRelMapper.selectCount(w);
		return n != null && n >= 1L;
	}

	public String findWorkUseridByCompanyAndOperator(long companyId, long operatorId) {
		LambdaQueryWrapper<DistributorWorkWechatRel> w = new LambdaQueryWrapper<>();
		w.eq(DistributorWorkWechatRel::getCompanyId, companyId).eq(DistributorWorkWechatRel::getOperatorId, operatorId);
		List<DistributorWorkWechatRel> rows = distributorWorkWechatRelMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return "";
		}
		String uid = rows.get(0).getWorkUserid();
		if (uid == null) {
			return "";
		}
		return uid.trim();
	}
}
