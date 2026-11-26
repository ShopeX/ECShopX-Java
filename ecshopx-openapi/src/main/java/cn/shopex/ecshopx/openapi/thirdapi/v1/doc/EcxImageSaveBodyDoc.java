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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ImageController#saveImage} 经
 * {@code OpenapiImageSaveParams#build} 读取的全部 key。
 */
@Schema(
		name = "EcxImageSaveBody",
		description =
				"保存图片元数据参数（body 与 query 同名 key 双通道：body 含键时以 body 为准；"
						+ "image_name/image_cat_id 取原始值，其余走 mergeString trim 语义）。"
						+ " 校验失败时多条错误文案以「，」拼接一次性返回")
public class EcxImageSaveBodyDoc {

	@Schema(
			description = "图片名称；必填非空白，长度上限 100（错误文案写 50，以代码 100 为准）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "示例图片")
	public String image_name;

	@Schema(
			description = "存储类型；必填（如 image/videos/local 等存储盘标识）",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "image")
	public String storage;

	@Schema(
			description = "图片存储 key/链接；必填",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "path/key.jpg")
	public String image_url;

	@Schema(
			description = "图片分类 ID；可选，缺省/空/\"0\"/非整数按 0",
			defaultValue = "0",
			example = "10")
	public String image_cat_id;

	@Schema(description = "图片简介；可选")
	public String brief;

	@Schema(
			description = "图片业务类型；可选，长度上限 20；缺省落库默认 item",
			example = "item")
	public String image_type;

	@Schema(description = "图片完整访问 URL；可选，仅存档")
	public String image_full_url;
}