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

package cn.shopex.ecshopx.common.openapi;

/**
 * OpenAPI {@code exc.operator.resetpwd}：按 shopexid（mobile）查找 admin 账号并使 JWT/Session 失效。
 */
public interface OpenapiOperatorResetPasswordPort {

	/**
	 * 按 shopexid（mobile）查找 admin 账号并使 JWT/Session 失效。
	 * 账号不存在时抛 {@link cn.shopex.ecshopx.common.exception.ResourceException}（message=账号信息不存在）。
	 */
	void invalidateAdminSession(String shopexId);
}
