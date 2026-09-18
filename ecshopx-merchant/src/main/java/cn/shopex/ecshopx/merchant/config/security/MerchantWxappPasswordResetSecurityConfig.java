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

package cn.shopex.ecshopx.merchant.config.security;

/**
 * 商户端 H5 认证已迁移到 {@code @FrontMerchantAuth} 注解 + {@link FrontMerchantAuthInterceptor}，
 * 不再需要独立的 SecurityFilterChain。
 *
 * @deprecated 保留空壳，待确认无残留引用后删除。
 */
@Deprecated(forRemoval = true)
public class MerchantWxappPasswordResetSecurityConfig {
}
