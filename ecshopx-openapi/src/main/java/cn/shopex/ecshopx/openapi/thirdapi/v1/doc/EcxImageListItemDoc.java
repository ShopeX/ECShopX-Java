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
 * 仅 OpenAPI 文档；{@code data.list} 单条图片，来源
 * {@code OpenapiThirdApiV1ImageListService#mapOpenApiRows}（5 键精简行）。
 */
@Schema(name = "EcxImageListItem")
public class EcxImageListItemDoc {

	@Schema(description = "图片 ID", example = "99")
	public Long image_id;

	@Schema(description = "图片名称", nullable = true)
	public String image_name;

	@Schema(description = "图片业务类型（库值，如 item）", example = "item")
	public String image_type;

	@Schema(
			description = "公开访问 URL（storage=videos 走视频盘，否则图片盘；local 驱动按请求 Host 生成绝对 URL）",
			example = "https://example.test/key.jpg")
	public String image_url;

	@Schema(description = "创建时间（Unix 秒，库列 created）", nullable = true)
	public Integer create_time;
}