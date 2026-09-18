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
import cn.shopex.ecshopx.companys.domain.DistributorWechatRel;
import cn.shopex.ecshopx.companys.mapper.DistributorWechatRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DistributorWechatMobileBindService {

	private final DistributorWechatRelMapper distributorWechatRelMapper;

	public DistributorWechatMobileBindService(DistributorWechatRelMapper distributorWechatRelMapper) {
		this.distributorWechatRelMapper = distributorWechatRelMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void upsertWechatRelForMobileBind(
			long companyId, String appId, String appType, String openid, String unionid, long operatorId) {
		LambdaQueryWrapper<DistributorWechatRel> boundQ = new LambdaQueryWrapper<>();
		boundQ.eq(DistributorWechatRel::getCompanyId, companyId)
				.eq(DistributorWechatRel::getAppType, appType)
				.eq(DistributorWechatRel::getOperatorId, operatorId)
				.last("LIMIT 1");
		if (distributorWechatRelMapper.selectOne(boundQ) != null) {
			throw new ResourceException("该手机号已绑定，请更换对应的企业微信账号");
		}
		long boundTime = System.currentTimeMillis() / 1000L;
		LambdaQueryWrapper<DistributorWechatRel> wxQ = new LambdaQueryWrapper<>();
		wxQ.eq(DistributorWechatRel::getCompanyId, companyId)
				.eq(DistributorWechatRel::getAppId, appId)
				.eq(DistributorWechatRel::getAppType, appType)
				.eq(DistributorWechatRel::getOpenid, openid)
				.eq(DistributorWechatRel::getUnionid, unionid)
				.last("LIMIT 1");
		DistributorWechatRel wxRow = distributorWechatRelMapper.selectOne(wxQ);
		if (wxRow != null) {
			wxRow.setOperatorId(operatorId);
			wxRow.setBoundTime(boundTime);
			distributorWechatRelMapper.updateById(wxRow);
		} else {
			DistributorWechatRel ins = new DistributorWechatRel();
			ins.setCompanyId(companyId);
			ins.setAppId(appId);
			ins.setAppType(appType);
			ins.setOpenid(openid);
			ins.setUnionid(unionid);
			ins.setOperatorId(operatorId);
			ins.setBoundTime(boundTime);
			distributorWechatRelMapper.insert(ins);
		}
	}
}
