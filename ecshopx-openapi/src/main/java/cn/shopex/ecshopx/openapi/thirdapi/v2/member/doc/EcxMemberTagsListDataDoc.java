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
 * 仅 OpenAPI 文档；data 分页结构（本服务 formatListStruct，4 固定键，
 * 键序 total_count/is_last_page/pager/list）。is_last_page 整型 0/1。
 */
@Schema(name = "EcxMemberTagsListData")
public class EcxMemberTagsListDataDoc {

	@Schema(description = "满足过滤条件的标签总数（整型，含系统标签）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页（整型 0/1，非布尔）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public PagerDoc pager;

	@Schema(description = "标签行数组（created 降序；无结果为空数组）")
	public List<RowDoc> list;

	/** 分页回显（page/page_size）。 */
	@Schema(name = "EcxMemberTagsListPager")
	public static class PagerDoc {

		@Schema(description = "生效页码（≥1）", example = "1")
		public Integer page;

		@Schema(description = "生效每页数量（[20,499]）", example = "20")
		public Integer page_size;
	}

	/** 标签行（9 固定键），来源 formatOpenApiListRow + 语言覆盖（description 还原库值）。 */
	@Schema(name = "EcxMemberTagsListRow")
	public static class RowDoc {

		@Schema(description = "标签 ID（整型，created 降序）", example = "1")
		public Long tag_id;

		@Schema(description = "标签名（语言分表覆盖生效）", example = "聚合测试类目")
		public String tag_name;

		@Schema(description = "标签分类 ID（整型）", example = "0")
		public Integer category_id;

		@Schema(description = "标签描述（**库内原值**——语言覆盖后被还原，不随 country_code 变化；可为 null）", nullable = true)
		public String description;

		@Schema(description = "标签背景色", example = "rgba(255, 25, 57, 1)")
		public String tag_color;

		@Schema(description = "标签字体色", example = "rgba(255, 255, 255, 1)")
		public String font_color;

		@Schema(description = "已打该标签的会员数（self_tag_count，整型）", example = "0")
		public Integer self_tag_count;

		@Schema(
				description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；null 透传 null；示例为格式演示值）",
				nullable = true,
				example = "2026-01-01 00:00:00")
		public String created;

		@Schema(
				description = "更新时间（yyyy-MM-dd HH:mm:ss，系统时区；null 透传 null；示例为格式演示值）",
				nullable = true,
				example = "2026-01-01 00:00:00")
		public String updated;
	}
}