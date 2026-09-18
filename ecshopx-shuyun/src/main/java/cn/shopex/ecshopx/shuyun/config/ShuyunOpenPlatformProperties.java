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

package cn.shopex.ecshopx.shuyun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数云开放平台配置（与遗留 UAPI {@code ecshopx.thirdparty.shuyun} 隔离）。
 * 对齐 PHP {@code config/shuyun_open_platform.php}。
 */
@Data
@ConfigurationProperties(prefix = "ecshopx.shuyun.open-platform")
public class ShuyunOpenPlatformProperties {

	/** open-api 出站基址 */
	private String baseUri = "http://open-api.shuyun.com";

	private double timeoutSeconds = 30d;

	/** open-client Token 刷新基址 */
	private String tokenRefreshBaseUri = "http://open-client.shuyun.com";

	/**
	 * DELTA-001：默认空（关闭）。DB 无 token 时不回退出站。
	 * 仅本地联调可临时填写，勿提交真实 token。
	 */
	private String fallbackGatewayAccessToken = "";

	/** 入站回调验签密匙（≠ DB app_secret） */
	private String callbackIdentitySecret = "";

	/** Token 回调匹配 auth_value 冷启动写入 */
	private String authValue = "";

	private boolean callbackDebugLog = false;

	private int tokenCallbackSaveMaxAttempts = 6;

	private long tokenCallbackSaveRetryBaseUsleep = 50_000L;

	private long tokenCallbackSaveRetryMaxUsleep = 800_000L;

	/** shop_id 后缀，默认 -off */
	private String offlinePlatIdSuffix = "-off";

	/** bind.push body.partner，对齐 PHP gateway_partner */
	private String gatewayPartner = "nnormal";

	/**
	 * 线下权益发券实现：{@code kaquan}（默认）或 {@code stub}。
	 * 对齐 PHP {@code offline_benefit_issuer}。
	 */
	private String offlineBenefitIssuer = "kaquan";

	/** customerId 解析策略，默认 numeric_user_id */
	private String offlineBenefitMemberResolveMode = "numeric_user_id";

	/** report/detail 推送 platform，默认 offline */
	private String offlineBenefitGatewayPlatform = "offline";

	/** report/detail 推送最大重试轮次 */
	private int offlineBenefitReportPushMaxCycles = 3;

	/** merge_dispatch TTL 秒；0=关闭 */
	private int mergeDispatchTtlSeconds = 3;

	/**
	 * order_class → trade_source（默认与 PHP config 一致）。
	 * 可用配置覆盖：ecshopx.shuyun.open-platform.order-class-trade-source-map.normal=11
	 */
	private java.util.Map<String, String> orderClassTradeSourceMap = defaultTradeSourceMap();

	private static java.util.Map<String, String> defaultTradeSourceMap() {
		java.util.LinkedHashMap<String, String> m = new java.util.LinkedHashMap<>();
		m.put("normal", "11");
		m.put("pointsmall", "12");
		m.put("shopadmin", "13");
		m.put("shopguide", "14");
		m.put("employee_purchase", "15");
		m.put("groups", "16");
		m.put("seckill", "17");
		m.put("community", "18");
		m.put("bargain", "19");
		m.put("excard", "20");
		m.put("drug", "21");
		return m;
	}
}
