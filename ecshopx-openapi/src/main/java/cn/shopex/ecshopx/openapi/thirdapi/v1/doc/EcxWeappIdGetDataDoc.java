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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；成功信封 data。{@code OpenapiWeappIdPortImpl#getWeappIdByTemplate} 组装的 2 键：
 * weappid（wechat_weapp 按租户 + template_name=yykweishop + deleted_at IS NULL 查 LIMIT 1，
 * 查无/appid 空时为 null）与 template_name（恒 "yykweishop"）。
 */
@Schema(name = "EcxWeappIdGetData")
public class EcxWeappIdGetDataDoc {

	@Schema(
			description =
					"授权小程序 appid（wechat_weapp.authorizer_appid）；未绑定/记录已删除/appid 为空时为 null（仍成功返回）",
			nullable = true,
			example = "wxResolved")
	public String weappid;

	@Schema(description = "模板名，本接口恒为 yykweishop", example = "yykweishop")
	public String template_name;
}