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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code ImageController#getImageList} 经
 * {@code OpenapiImageListParams#build} 读取的全部 key；筛选语义见
 * {@code OpenapiThirdApiV1ImageListService#executeOpenapiImageList}。
 */
@Schema(
		name = "EcxImageListBody",
		description =
				"图片列表查询参数（body 与 query 同名 key 双通道：body 含键时以 body 为准；"
						+ "storage/image_name/image_cat_id 取原始值，disabled/distributor_id 走 mergeString trim 语义）。"
						+ " 注意分页键为 pageSize（驼峰）")
public class EcxImageListBodyDoc {

	@Schema(
			description = "存储类型筛选（upload_images.storage 精确匹配）：image 图片；videos 视频；空/\"0\"/缺省按 image",
			defaultValue = "image",
			example = "image")
	public String storage;

	@Schema(description = "图片名称，模糊匹配；PHP-empty（空/\"0\"/0/false）时不过滤")
	public String image_name;

	@Schema(
			description =
					"删除状态筛选：PHP-truthy（非空且非 \"0\"）时查已删除（disabled=true），否则查未删除（false）；"
							+ "注意任何 truthy 值都查已删除，并非按值匹配",
			example = "0")
	public String disabled;

	@Schema(
			description = "图片分类 ID 精确匹配；PHP-empty 时不过滤；非整数/小数忽略该条件；\"0\" 之外的整数按分类过滤",
			example = "10")
	public String image_cat_id;

	@Schema(description = "店铺 ID 精确匹配；缺省/非整数按 0（平台图片）", defaultValue = "0", example = "0")
	public String distributor_id;

	@Schema(description = "页码，从 1 起；非整数/缺省按 1", defaultValue = "1", example = "1")
	public String page;

	@Schema(description = "每页条数；非整数/缺省按 20；0 时返回空列表（total_count 仍为真实总数）", defaultValue = "20", example = "20")
	public String pageSize;
}