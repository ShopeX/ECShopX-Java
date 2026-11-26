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
 * 仅 OpenAPI 文档；失败信封（HTTP 仍为 200）。
 *
 * <p>业务失败：filetype 缺失/空白 → {@code ResourceException}「不支持的文件存储类型」；
 * 非法值 → 「不支持的文件存储类型{t}」；qiniu 区域未配置 → 「{fileType}配置文件错误」；均 E0001、data=null。
 * 鉴权失败：E4004「未授权」（直连 internal）；公网网关签名失败另有 E4001/E4002/E4003。
 */
@Schema(name = "EcxImageUploadTokenFailEnvelope")
public class EcxImageUploadTokenFailEnvelopeDoc {

	@Schema(description = "业务状态", example = "fail")
	public String status;

	@Schema(description = "业务码：E0001 业务失败；E4004 未授权", example = "E0001")
	public String code;

	@Schema(description = "错误说明", example = "不支持的文件存储类型")
	public String message;

	@Schema(description = "附加数据，本接口失败恒为 null", nullable = true)
	public Object data;
}