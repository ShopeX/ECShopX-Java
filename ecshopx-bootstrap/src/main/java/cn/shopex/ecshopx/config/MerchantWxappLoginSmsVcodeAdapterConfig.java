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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.merchant.port.MerchantWxappLoginSmsVcodePort;
import cn.shopex.ecshopx.members.service.reg.MemberRegSettingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Wxapp 商户登录短信码与 {@code members} Redis 默认库一致：使用 {@link MultiRedisConfig#springDataRedisConnectionFactory}
 * 上的 {@code sharedStringRedisTemplate}（{@code spring.data.redis.database}，默认 0），而非 {@code @Primary}
 * {@code companysRedisTemplate}（{@code ecshopx.redis.companys.database}）。
 */
@Configuration
public class MerchantWxappLoginSmsVcodeAdapterConfig {

	@Bean
	public MerchantWxappLoginSmsVcodePort merchantWxappLoginSmsVcodePort(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate membersParityRedis) {
		MemberRegSettingService smsVerifier = new MemberRegSettingService(membersParityRedis);
		return (mobile, companyId, vcode) ->
				smsVerifier.checkSmsVcode(mobile, companyId, vcode, "merchant_login");
	}
}
