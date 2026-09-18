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
import cn.shopex.ecshopx.merchant.port.OperatorAccountPasswordUpdatePort;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CompanysOperatorAccountPasswordUpdateService implements OperatorAccountPasswordUpdatePort {

	private final OperatorsMapper operatorsMapper;
	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	public CompanysOperatorAccountPasswordUpdateService(OperatorsMapper operatorsMapper) {
		this.operatorsMapper = operatorsMapper;
	}

	@Override
	public void updatePasswordByOperatorId(long operatorId, String plainPassword) {
		Operators row = operatorsMapper.selectById(operatorId);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}
		String hash = bcrypt.encode(plainPassword);
		LambdaUpdateWrapper<Operators> w = new LambdaUpdateWrapper<>();
		w.eq(Operators::getOperatorId, operatorId).set(Operators::getPassword, hash);
		operatorsMapper.update(null, w);
	}
}
