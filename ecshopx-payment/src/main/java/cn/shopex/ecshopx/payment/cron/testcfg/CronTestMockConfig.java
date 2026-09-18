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

package cn.shopex.ecshopx.payment.cron.testcfg;

import cn.shopex.ecshopx.common.cron.mock.NoopAdapayPaymentSettingsReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopBspayPaymentSettingsReadPort;
import cn.shopex.ecshopx.common.cron.mock.NoopWechatBatchTransferQueryPort;
import cn.shopex.ecshopx.common.cron.mock.NoopWechatPayMerchantContextPort;
import cn.shopex.ecshopx.common.port.payment.AdapayPaymentSettingsReadPort;
import cn.shopex.ecshopx.common.port.payment.BspayPaymentSettingsReadPort;
import cn.shopex.ecshopx.common.port.weixin.WechatBatchTransferQueryPort;
import cn.shopex.ecshopx.common.port.weixin.WechatPayMerchantContextPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 定时任务 test-cron 覆盖；仅 spring.profiles.active 含 test-cron 时生效。替换本模块对微信/Redis 的副作用，避免外呼与读实库证书。
 * 斗拱支付设置读、汇付/Ada 读端口与微信相关 Noop 在本配置注册。
 * 斗拱确认 HTTP（{@code BspayPaymentConfirmHttpGateway}）的 Noop 由 <strong>aftersales</strong> 模块
 * {@code cn.shopex.ecshopx.aftersales.cron.testcfg.CronTestMockConfig} 提供，避免与全量 test-cron 启动时重复注册同名 Bean。
 */
@Profile("test-cron")
@Configuration("paymentCronTestMockConfig")
public class CronTestMockConfig {

	/**
	 * 覆盖 {@code WechatBatchTransferQueryHttp}，阶段 4 可 grep <code>[cron-mock][wechat-http]</code>。
	 */
	@Bean
	@Primary
	public WechatBatchTransferQueryPort wechatBatchTransferQueryPort(ObjectMapper objectMapper) {
		return new NoopWechatBatchTransferQueryPort(objectMapper);
	}

	/**
	 * 覆盖 {@code WechatPayMerchantContextService}，阶段 4 可 grep
	 * <code>[cron-mock][wxpay-merchant-config]</code>。
	 */
	@Bean
	@Primary
	public WechatPayMerchantContextPort wechatPayMerchantContextPort() {
		return new NoopWechatPayMerchantContextPort();
	}

	/**
	 * 替换 {@code AdapayPaymentSettingRedisReader} 对应接口，避免阶段 4 读业务 Redis。
	 */
	@Bean
	@Primary
	public AdapayPaymentSettingsReadPort adapayPaymentSettingsReadPort() {
		return new NoopAdapayPaymentSettingsReadPort();
	}

	/**
	 * 替换从 Redis 读取的斗拱支付设置，避免阶段 4 真读业务 Redis。
	 */
	@Bean
	@Primary
	public BspayPaymentSettingsReadPort bspayPaymentSettingsReadPort() {
		return new NoopBspayPaymentSettingsReadPort();
	}
}
