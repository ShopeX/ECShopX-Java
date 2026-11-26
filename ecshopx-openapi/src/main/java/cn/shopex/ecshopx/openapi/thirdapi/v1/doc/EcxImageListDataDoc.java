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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷，来源
 * {@code OpenapiThirdApiV1ImageListService#executeOpenapiImageList}：total_count + list（created 倒序）。
 */
@Schema(
		name = "EcxImageListData",
		description = "图片列表；pageSize=0 时 list 为空但 total_count 仍为真实总数",
		example =
				"{\"total_count\":1,\"list\":[{\"image_id\":99,\"image_name\":\"示例图片\",\"image_type\":\"item\","
						+ "\"image_url\":\"https://example.test/key.jpg\",\"create_time\":1764115200}]}")
public class EcxImageListDataDoc {

	@Schema(description = "符合条件的总记录数", example = "1")
	public Integer total_count;

	@ArraySchema(arraySchema = @Schema(description = "图片列表；无匹配或 pageSize=0 时为空数组"))
	public List<EcxImageListItemDoc> list;
}