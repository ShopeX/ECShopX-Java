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

package cn.shopex.ecshopx.members.service.email;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/**
 * 为邮箱注册会员分配占位手机号：{@code 10} 开头 + 9 位随机数字（共 11 位），
 * 保证 {@code (mobile, company_id)} 唯一。对齐 PHP {@code MemberSyntheticMobileService}。
 */
@Service
public class MemberSyntheticMobileService {

	private static final int MAX_ATTEMPTS = 80;

	private final MemberAccountService memberAccountService;

	public MemberSyntheticMobileService(MemberAccountService memberAccountService) {
		this.memberAccountService = memberAccountService;
	}

	public String allocateUnique(long companyId) {
		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			String suffix = String.format(
					"%09d", ThreadLocalRandom.current().nextInt(0, 1_000_000_000));
			String candidate = "10" + suffix;
			Map<String, Object> existing = memberAccountService.getInfoByMobile(companyId, candidate);
			if (existing == null || existing.isEmpty()) {
				return candidate;
			}
		}
		throw new ResourceException("系统繁忙，请稍后重试");
	}
}
