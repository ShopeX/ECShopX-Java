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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.DistributorWorkWechatRel;
import cn.shopex.ecshopx.companys.mapper.DistributorWorkWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistributorWorkWechatMobileBindService {

	private final DistributorWorkWechatRelMapper distributorWorkWechatRelMapper;

	public DistributorWorkWechatMobileBindService(DistributorWorkWechatRelMapper distributorWorkWechatRelMapper) {
		this.distributorWorkWechatRelMapper = distributorWorkWechatRelMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void upsertWorkWechatRelForMobileBind(long companyId, String workUserid, long operatorId) {
		LambdaQueryWrapper<DistributorWorkWechatRel> byOp = new LambdaQueryWrapper<>();
		byOp.eq(DistributorWorkWechatRel::getCompanyId, companyId)
				.eq(DistributorWorkWechatRel::getOperatorId, operatorId)
				.last("LIMIT 1");
		if (distributorWorkWechatRelMapper.selectOne(byOp) != null) {
			throw new ResourceException("该手机号已绑定，请更换对应的企业微信账号");
		}
		LambdaQueryWrapper<DistributorWorkWechatRel> byWx = new LambdaQueryWrapper<>();
		byWx.eq(DistributorWorkWechatRel::getCompanyId, companyId)
				.eq(DistributorWorkWechatRel::getWorkUserid, workUserid)
				.last("LIMIT 1");
		DistributorWorkWechatRel existing = distributorWorkWechatRelMapper.selectOne(byWx);
		long boundTime = System.currentTimeMillis() / 1000L;
		if (existing != null) {
			existing.setOperatorId(operatorId);
			existing.setBoundTime(boundTime);
			int updated = distributorWorkWechatRelMapper.updateById(existing);
			if (updated != 1) {
				throw new ResourceException("未查询到更新数据");
			}
		} else {
			DistributorWorkWechatRel row = new DistributorWorkWechatRel();
			row.setCompanyId(companyId);
			row.setWorkUserid(workUserid);
			row.setOperatorId(operatorId);
			row.setBoundTime(boundTime);
			int inserted = distributorWorkWechatRelMapper.insert(row);
			if (inserted != 1) {
				throw new ResourceException("未查询到更新数据");
			}
		}
	}
}
