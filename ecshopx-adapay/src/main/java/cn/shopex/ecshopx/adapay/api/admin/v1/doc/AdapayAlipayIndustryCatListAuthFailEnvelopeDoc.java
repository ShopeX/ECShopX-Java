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

package cn.shopex.ecshopx.adapay.api.admin.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；401 Dingo 信封
 * （{@code OperatorJwtAuthenticationFilter} / {@code AdminAuthInterceptor} 直接写出
 * {@code {"data":{"message":...,"status_code":401}}}，无业务 code 键）。
 */
@Schema(name = "AdapayAlipayIndustryCatListAuthFailEnvelope")
public class AdapayAlipayIndustryCatListAuthFailEnvelopeDoc {

	@Schema(description = "错误详情")
	public AdapayAlipayIndustryCatListAuthFailDataDoc data;

	@Schema(name = "AdapayAlipayIndustryCatListAuthFailData")
	public static class AdapayAlipayIndustryCatListAuthFailDataDoc {

		@Schema(
				description =
						"错误文案：缺失/非法 Authorization 时为固定英文句；token 过期/校验失败为「登录验证错误」",
				example =
						"Failed to authenticate because of bad credentials or an invalid authorization header.")
		public String message;

		@Schema(description = "HTTP 状态码（与响应状态一致，固定 401）", example = "401")
		public Integer status_code;
	}
}