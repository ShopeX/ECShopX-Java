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

package cn.shopex.ecshopx.goods.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 搜索热词（ECX-11390 / ECX-11411、ECX-11413、ECX-11415）。
 *
 * <p>展示状态不落库，由 {@code start_time}、{@code end_time} 与当前时间实时计算；展示渠道为单选
 * {@code channel}（{@code mobile} 或 {@code pc}）。
 */
@Data
@MpTable(
		value = "search_hotword",
		comment = "搜索热词",
		indexes = {
			@MpIndex(name = "ix_search_hotword_company_start_end", columns = {"company_id", "start_time", "end_time"}),
			@MpIndex(name = "ix_search_hotword_company_sort", columns = {"company_id", "sort"})
		})
public class SearchHotword {

	public static final String TYPE_TEXT = "text";

	public static final String TYPE_IMAGE = "image";

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	/** 热词；{@code type=image} 时仍作为热词名与未配置链接时的搜索词面 */
	@MpField(value = "content", columnType = "string", length = 100, comment = "热词")
	private String content;

	/** 内容形态：text=文案（默认）、image=图片 */
	@MpField(value = "type", columnType = "string", length = 10, comment = "内容形态 text/image", defaultValue = "text")
	private String type = TYPE_TEXT;

	@MpField(value = "text_color", columnType = "string", length = 16, nullable = true, comment = "文案颜色 #RRGGBB")
	private String textColor;

	@MpField(value = "background_color", columnType = "string", length = 16, nullable = true, comment = "文案背景色 #RRGGBB")
	private String backgroundColor;

	@MpField(value = "icon_url", columnType = "string", length = 512, nullable = true, comment = "文案图标地址")
	private String iconUrl;

	@MpField(value = "image_url", columnType = "string", length = 512, nullable = true, comment = "图片地址")
	private String imageUrl;

	/** 排序值，越大越靠前；相同值按 {@code created} 倒序 */
	@MpField(value = "sort", columnType = "integer", comment = "排序值,越大越靠前", defaultValue = "0")
	private Integer sort = 0;

	/** 展示开始时间（epoch 秒，含端点） */
	@MpField(value = "start_time", columnType = "integer", comment = "展示开始时间(epoch秒,含)")
	private Integer startTime;

	/** 展示结束时间（epoch 秒，含端点） */
	@MpField(value = "end_time", columnType = "integer", comment = "展示结束时间(epoch秒,含)")
	private Integer endTime;

	/** 展示渠道：mobile 或 pc（单选） */
	@MpField(value = "channel", columnType = "string", length = 32, comment = "展示渠道 mobile/pc", defaultValue = "mobile")
	private String channel = "mobile";

	/** 跳转配置 JSON，对齐会员中心：{@code {"linkPage":"...","id":"...","title":"..."} } */
	@MpField(value = "jump_page", columnType = "string", length = 512, nullable = true, comment = "跳转配置JSON")
	private String jumpPage;

	@MpField(value = "created", columnType = "integer", comment = "创建时间(epoch秒)")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间(epoch秒)")
	private Integer updated;
}
