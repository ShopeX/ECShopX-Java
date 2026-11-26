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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ImageController#getUploadToken} 经
 * {@code OpenapiRequestParams#mergeString} 读取的全部 key。
 */
@Schema(
		name = "EcxImageUploadTokenBody",
		description =
				"获取上传凭证参数（body 与 query 同名 key 双通道：body 含键时以 body 为准，"
						+ "null/空白视为未提供且不回退 query；body 无该键时读 query 并 trim）")
public class EcxImageUploadTokenBodyDoc {

	@Schema(
			description = "文件类型；必填：image 图片；videos 视频；file 通用文件；其他值报「不支持的文件存储类型{t}」",
			requiredMode = Schema.RequiredMode.REQUIRED,
			allowableValues = {"image", "videos", "file"},
			example = "image")
	public String filetype;

	@Schema(description = "存储分组（对象键中的可选路径段）；可选，缺省空串", example = "item")
	public String group;

	@Schema(
			description = "文件名（参与对象键拼接；缺省用 32 位 UUID）；可选",
			example = "test.png")
	public String filename;
}