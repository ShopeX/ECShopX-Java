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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；GET 请求可选 JSON body，字段来源 {@code ItemsCategoryController#getItemMainCategoryDetail}
 * 经 {@code OpenapiRequestParams#originalString} 读取的全部 key。
 */
@Schema(
		name = "EcxItemMaincategoryDetailGetBody",
		description =
				"主类目详情参数（body 与 query 同名 key 双通道：body 含键取原始值不 trim，否则用 query）")
public class EcxItemMaincategoryDetailGetBodyDoc {

	@Schema(
			description =
					"三级主类目名称路径（一级->二级->三级，恰好 3 段）；必填非空否则 E5001「主类目名称必填」；"
							+ "段数≠3 报 E5130「主类目名称格式错误」；逐级按名称查（is_main_category=1、distributor_id=0），"
							+ "任一级查无报 E5131「{一/二/三}级类目名称未查询到相关数据」；复核失败返回 data=[]",
			requiredMode = Schema.RequiredMode.REQUIRED,
			example = "HP->上衣->HP")
	public String category;
}