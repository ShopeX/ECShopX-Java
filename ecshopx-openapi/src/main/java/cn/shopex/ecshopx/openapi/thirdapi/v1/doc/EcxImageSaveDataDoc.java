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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code OpenapiThirdApiV1ImageSaveService#executeOpenapiImageSave}：插入后返回自增 image_id。
 */
@Schema(name = "EcxImageSaveData", description = "保存结果", example = "{\"image_id\":99}")
public class EcxImageSaveDataDoc {

	@Schema(description = "新建图片记录 ID（upload_images 自增主键）", example = "99")
	public Long image_id;
}