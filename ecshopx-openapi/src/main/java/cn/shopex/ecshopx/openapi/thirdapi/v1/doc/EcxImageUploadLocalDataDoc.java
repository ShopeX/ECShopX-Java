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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源 {@code ImageController#uploadeImage}：
 * 上传成功后返回对象键（{@code OpenapiImageUploadTokenPortImpl#uploadLocalImage} 取 token.key）。
 */
@Schema(name = "EcxImageUploadLocalData", description = "上传结果", example = "{\"image_url\":\"path/key.jpg\"}")
public class EcxImageUploadLocalDataDoc {

	@Schema(
			description =
					"存储对象键（非完整 URL），形如 {project}/{filetype}/{companyId}/{yyyy/MM/dd}[/{group}]/{md5(时间)}{文件名}；"
							+ "后续可作为 ecx.image.save 的 image_url 登记元数据",
			example = "path/key.jpg")
	public String image_url;
}