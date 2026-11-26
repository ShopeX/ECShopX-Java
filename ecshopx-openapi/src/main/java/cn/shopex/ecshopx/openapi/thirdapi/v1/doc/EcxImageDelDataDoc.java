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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源 {@code ImageController#deleteImage}
 * 删除成功后写入 {@code {status:true}}（软删除 upload_images.disabled=true）。
 */
@Schema(name = "EcxImageDelData", description = "删除结果", example = "{\"status\":true}")
public class EcxImageDelDataDoc {

	@Schema(description = "删除成功恒为 true（失败走信封 fail，不返回 false）", example = "true")
	public Boolean status;
}