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

package cn.shopex.ecshopx.deposit.service;

import cn.shopex.ecshopx.deposit.domain.RechargeAgreement;
import cn.shopex.ecshopx.deposit.mapper.RechargeAgreementMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

@Service
public class RechargeAgreementWriteService {

	private final RechargeAgreementMapper rechargeAgreementMapper;

	public RechargeAgreementWriteService(RechargeAgreementMapper rechargeAgreementMapper) {
		this.rechargeAgreementMapper = rechargeAgreementMapper;
	}

	public void setAgreement(long companyId, String content) {
		String companyIdStr = String.valueOf(companyId);
		RechargeAgreement row = rechargeAgreementMapper.selectById(companyIdStr);
		long nowSec = System.currentTimeMillis() / 1000L;
		String createTimeStr = String.valueOf(nowSec);
		if (row != null) {
			LambdaUpdateWrapper<RechargeAgreement> wrapper = new LambdaUpdateWrapper<>();
			wrapper
					.set(RechargeAgreement::getContent, content)
					.set(RechargeAgreement::getCreateTime, createTimeStr)
					.eq(RechargeAgreement::getCompanyId, companyIdStr);
			rechargeAgreementMapper.update(null, wrapper);
		} else {
			RechargeAgreement entity = new RechargeAgreement();
			entity.setCompanyId(companyIdStr);
			entity.setContent(content);
			entity.setCreateTime(createTimeStr);
			rechargeAgreementMapper.insert(entity);
		}
	}
}
