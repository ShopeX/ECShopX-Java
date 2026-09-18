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

package cn.shopex.ecshopx.companys.service.auth;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.OperatorSmsVerifyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatorPasswordResetService {

	private static final Pattern PHONE = Pattern.compile("^1\\d{10}$");

	private static final Pattern PASSWORD_COMPLEXITY =
			Pattern.compile(
					"^(?!^[0-9]+$)(?!^[a-z]+$)(?!^[A-Z]+$)(?!^[^A-z0-9]+$)^[^\\s\\x{4e00}-\\x{9fa5}]{8,}$",
					Pattern.UNICODE_CHARACTER_CLASS);

	private final OperatorSmsVerifyService operatorSmsVerifyService;
	private final OperatorsMapper operatorsMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	public OperatorPasswordResetService(
			OperatorSmsVerifyService operatorSmsVerifyService,
			OperatorsMapper operatorsMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor) {
		this.operatorSmsVerifyService = operatorSmsVerifyService;
		this.operatorsMapper = operatorsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
	}

	@Transactional(rollbackFor = Exception.class)
	public void resetPassword(String account, String code, String newPassword) {
		String rawAccount = account != null ? account.trim() : "";
		if (rawAccount.isEmpty()) {
			throw new BadRequestException("手机号码错误");
		}
		if (!PHONE.matcher(rawAccount).matches()) {
			throw new BadRequestException("手机号码错误");
		}
		String phone = rawAccount;
		if (!operatorSmsVerifyService.verifyForgetCodeAndDelete(phone, code)) {
			throw new ResourceException("验证码错误");
		}
		String pwd = newPassword != null ? newPassword : "";
		if (!PASSWORD_COMPLEXITY.matcher(pwd).matches()) {
			throw new BadRequestException("密码至少8位以上，至少由数字、字母或特殊字符中两种及以上方式组成");
		}
		String mobileForDb = sensitiveFieldEncryptor.encrypt(phone);
		LambdaQueryWrapper<Operators> w = new LambdaQueryWrapper<>();
		w.eq(Operators::getMobile, mobileForDb);
		List<Operators> list = operatorsMapper.selectList(w);
		if (list == null || list.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}
		Operators entity = list.get(0);
		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setPassword(BCrypt.hashpw(pwd, BCrypt.gensalt()));
		entity.setUpdated(now);
		operatorsMapper.updateById(entity);
	}
}
