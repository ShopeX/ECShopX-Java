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

package cn.shopex.ecshopx.common.cron.mock;

import cn.shopex.ecshopx.common.port.weixin.WechatMerchantV3ApiMaterial;
import cn.shopex.ecshopx.common.port.weixin.WechatPayMerchantContextPort;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * test-cron 下不读 Redis/证书；与 {@code wxpay-merchant-config} 别名一致供阶段 4 grep 断言。
 */
@Slf4j
public class NoopWechatPayMerchantContextPort implements WechatPayMerchantContextPort {

	private static final WechatMerchantV3ApiMaterial DUMMY;

	static {
		try {
			KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
			g.initialize(2048);
			KeyPair p = g.generateKeyPair();
			DUMMY = new WechatMerchantV3ApiMaterial("noop", "00", p.getPrivate());
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final AtomicInteger callCount = new AtomicInteger();

	@Override
	public WechatMerchantV3ApiMaterial requireForV3Api(long companyId) {
		int n = callCount.incrementAndGet();
		log.info(
				"[cron-mock][wxpay-merchant-config] called#{}, args=companyId={}", n, companyId);
		return DUMMY;
	}
}
