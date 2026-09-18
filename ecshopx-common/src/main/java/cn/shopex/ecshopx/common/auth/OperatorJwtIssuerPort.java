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

package cn.shopex.ecshopx.common.auth;

import java.util.Map;

/**
 * 运营端 JWT 颁发抽象，由 espier 模块的 {@code OperatorJwtIssuer} 实现并注册为 Spring Bean。
 */
public interface OperatorJwtIssuerPort {

	/**
	 * 基于 {@code CompanysActivationService#getLoginToken} 产出的载荷签发 JWT。
	 *
	 * @param newOperatorFromGetLoginToken 含 {@code id}、{@code company_id}、{@code operator_type} 等声明字段
	 * @return 紧凑序列化 JWT 字符串
	 */
	String issueToken(Map<String, Object> newOperatorFromGetLoginToken);

	/**
	 * 使用旧紧凑 JWT 在刷新窗口内签发新 Access Token；实现须将旧 Token 加入黑名单（TTL 至旧 Token 的 exp）后再签发。
	 *
	 * @param compactJwt Authorization Bearer 后的完整紧凑 JWT
	 * @return 新紧凑 JWT
	 */
	String refreshAccessToken(String compactJwt);
}
