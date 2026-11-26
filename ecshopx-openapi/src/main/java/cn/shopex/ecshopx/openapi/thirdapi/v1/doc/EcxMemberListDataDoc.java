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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷（对象形态），来源
 * {@code OpenapiThirdApiV1MemberInfoListService#executeOpenapiMemberInfoList}：
 * count 为匹配会员资料总数，list 按 user_id 倒序**固定取前 100 条**（不随入参分页翻页）；
 * 每行 = {@code toMembersInfoOpenapiListMap} 19 键 + total_amount/unionid + 条件性最近浏览 4 键。
 * unionid 值为空或无匹配关联时 data 为空数组 []（非本对象）。
 */
@Schema(
		name = "EcxMemberListData",
		description = "会员列表：count 为匹配总数；list 固定前 100 条（user_id 倒序）；无匹配时 data=[]")
public class EcxMemberListDataDoc {

	@Schema(description = "匹配会员资料总数（可能大于 list 长度）", example = "1")
	public Long count;

	@ArraySchema(arraySchema = @Schema(description = "会员列表，user_id 倒序，最多 100 条"))
	public List<MemberRowDoc> list;

	@Schema(name = "EcxMemberListRow", description = "单个会员（资料 19 键 + 消费/关联/最近浏览）")
	public static class MemberRowDoc {

		@Schema(description = "会员 ID", example = "5")
		public Long user_id;

		@Schema(description = "租户/公司 ID", example = "100")
		public Long company_id;

		@Schema(description = "用户名（库原值，本列表不解密）", nullable = true)
		public String username;

		@Schema(description = "姓名", nullable = true)
		public String name;

		@Schema(description = "头像", nullable = true)
		public String avatar;

		@Schema(description = "性别：0 未知；1 男；2 女（库原值）", nullable = true)
		public Integer sex;

		@Schema(description = "出生日期（库原样字符串）", nullable = true)
		public String birthday;

		@Schema(description = "地址", nullable = true)
		public String address;

		@Schema(description = "邮箱", nullable = true)
		public String email;

		@Schema(description = "行业", nullable = true)
		public String industry;

		@Schema(description = "收入", nullable = true)
		public String income;

		@Schema(description = "学历", nullable = true)
		public String edu_background;

		@ArraySchema(arraySchema = @Schema(description = "爱好列表（JSON 数组解码；非数组/非法为 []）"))
		public List<Object> habbit;

		@Schema(description = "创建时间（Unix 秒；null→0）", example = "1764115200")
		public Long created;

		@Schema(description = "更新时间（Unix 秒；null→0）", example = "1764115200")
		public Long updated;

		@Schema(description = "是否消费过", example = "false")
		public Boolean have_consume;

		@Schema(
				description = "扩展参数（JSON 字符串原样；空/空对象/空数组归一为 \"[]\"）",
				example = "[]")
		public String other_params;

		@Schema(description = "达摩 CRM 会员 ID", nullable = true)
		public String dm_member_id;

		@Schema(description = "达摩 CRM 卡号", nullable = true)
		public String dm_card_no;

		@Schema(description = "累计消费金额（分；该会员全部订单 SUM(total_fee)，无订单为 0）", example = "9900")
		public Long total_amount;

		@Schema(description = "匹配到的微信 unionid", example = "union-test")
		public String unionid;

		@Schema(description = "【条件】浏览足迹总数（仅有足迹时存在）", example = "3")
		public Long browse_count;

		@Schema(description = "【条件】最近浏览时间 yyyy-MM-dd HH:mm:ss（仅有足迹时存在）", example = "2026-11-26 12:00:00")
		public String browse_time;

		@Schema(description = "【条件】最近浏览商品名称（仅有足迹时存在；商品缺失为 null）", nullable = true)
		public String browse_item_name;

		@Schema(description = "【条件】最近浏览商品 ID（仅有足迹时存在）", example = "1001")
		public Long browse_item_id;
	}
}