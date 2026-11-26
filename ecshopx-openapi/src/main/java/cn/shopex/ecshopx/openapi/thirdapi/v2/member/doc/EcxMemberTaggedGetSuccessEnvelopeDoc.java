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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；成功信封。Handler 返回 List，{@code OpenapiResponseBodyAdvice} 包装为
 * {@code OpenapiEnvelope.success(list)}（message 固定「成功」）。
 * **会员存在但无任何标签时服务返回 null → data=null**（非空数组）。
 */
@Schema(name = "EcxMemberTaggedGetSuccessEnvelope")
public class EcxMemberTaggedGetSuccessEnvelopeDoc {

	@Schema(description = "业务状态", example = "success")
	public String status;

	@Schema(description = "业务码", example = "E0000")
	public String code;

	@Schema(description = "提示文案（Advice 包装固定「成功」）", example = "成功")
	public String message;

	@Schema(description = "已打标签行数组（每行 8 键）；无标签时为 null", nullable = true)
	public List<RowDoc> data;

	/** 已打标签行（8 固定键），来源 executeOpenapiGetMemberTagged（selectUserRelTagList JOIN）。 */
	@Schema(name = "EcxMemberTaggedGetRow")
	public static class RowDoc {

		@Schema(
				description = "会员手机号（**members.mobile 库内原样**——加密启用时为密文，未解密，如实记录）",
				example = "13800138000")
		public String mobile;

		@Schema(description = "会员用户名（members_info.username 解密；无 info 行/空白为 null）", nullable = true, example = "张三")
		public String username;

		@Schema(description = "标签 ID（整型）", example = "1")
		public Long tag_id;

		@Schema(description = "标签名", example = "聚合测试类目")
		public String tag_name;

		@Schema(description = "标签分类 ID（整型）", example = "0")
		public Integer category_id;

		@Schema(description = "标签描述（库值，可为 null）", nullable = true)
		public String description;

		@Schema(description = "标签背景色", example = "rgba(255, 25, 57, 1)")
		public String tag_color;

		@Schema(description = "标签字体色", example = "rgba(255, 255, 255, 1)")
		public String font_color;
	}
}