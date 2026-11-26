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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200），均 data=null：
 * E5401「店铺找不到」（筛选定位查无）/ E5700「没有开通此小程序」（template_name 对应授权记录查无，
 * 含缺省空串场景）——均 {@code OpenapiDistributorV2FailException}；
 * E0001：微信取码失败文案（{@code WxaUnlimitedQrcodeClient}，如「小程序页面尚未发布或未通过审核」/
 * 「获取小程序码失败：响应为空」/ 微信 errmsg 透传）。
 * 鉴权失败：E4004「未授权」（直连 internal 无鉴权上下文）；公网网关签名校验失败返回网关对应业务码。
 */
@Schema(name = "EcxDistributorDownloadFailEnvelope")
public class EcxDistributorDownloadFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E5401 店铺找不到；E5700 没有开通此小程序；E0001 微信取码失败；E4004 未授权", example = "E5700")
	public String code;

	@Schema(description = "错误说明", example = "没有开通此小程序")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}