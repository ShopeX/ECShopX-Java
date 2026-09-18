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

package cn.shopex.ecshopx.companys.service.merchant;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.merchant.port.MerchantWxappSettlementApplyLoginInfoPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanysMerchantWxappSettlementApplyLoginInfoService implements MerchantWxappSettlementApplyLoginInfoPort {

	private final OperatorsMapper operatorsMapper;
	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	public CompanysMerchantWxappSettlementApplyLoginInfoService(OperatorsMapper operatorsMapper) {
		this.operatorsMapper = operatorsMapper;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> buildMobileAndMaybePassword(long companyId, long merchantId) {
		LambdaQueryWrapper<Operators> q = new LambdaQueryWrapper<>();
		q.eq(Operators::getCompanyId, companyId)
				.eq(Operators::getOperatorType, "merchant")
				.eq(Operators::getMerchantId, merchantId);
		Operators row = operatorsMapper.selectOne(q);
		if (row == null) {
			throw new ResourceException("操作员信息不完整");
		}
		String pwd = row.getPassword();
		if (isPasswordEmpty(pwd)) {
			String plain = String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
			String hash = bcrypt.encode(plain);
			LambdaUpdateWrapper<Operators> w = new LambdaUpdateWrapper<>();
			w.eq(Operators::getOperatorId, row.getOperatorId())
					.eq(Operators::getCompanyId, companyId)
					.set(Operators::getPassword, hash);
			int n = operatorsMapper.update(null, w);
			if (n == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("mobile", row.getMobile());
			out.put("password", plain);
			return out;
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("mobile", row.getMobile());
		return out;
	}

	private static boolean isPasswordEmpty(String password) {
		if (password == null || password.isEmpty()) {
			return true;
		}
		return password.trim().isEmpty();
	}
}
