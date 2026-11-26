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
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷（scope=fp 形态），来源
 * {@code OpenapiThirdApiV1MemberListService#executeFpMemberList}：
 * count 为符合条件总数，list 为 {@code MembersMapper.xml openapiMemberListSelectColumns}（48 列）
 * 经 mobile/username 解密、nickname 置空、unionid 兜底、total_amount 与浏览 4 键补全（合计 55 键）。
 * scope=customer 时 data 为 unionid 字符串数组（非本对象）。
 */
@Schema(
		name = "EcxMemberListFpData",
		description = "scope=fp：count + list（user_id 倒序分页）；scope=customer 时 data 为 unionid 字符串数组")
public class EcxMemberListFpDataDoc {

	@Schema(description = "符合条件的会员总数", example = "1")
	public Long count;

	@ArraySchema(arraySchema = @Schema(description = "会员列表，user_id 倒序，按 page/page_size 分页"))
	public List<FpMemberRowDoc> list;

	@Schema(name = "EcxMemberListFpRow", description = "单个会员（55 键）")
	public static class FpMemberRowDoc {

		@Schema(description = "会员 ID", example = "5")
		public Long user_id;

		@Schema(description = "租户/公司 ID", example = "100")
		public Long company_id;

		@Schema(description = "会员卡等级 ID", nullable = true)
		public Long grade_id;

		@Schema(description = "手机号（解密后明文）", nullable = true)
		public String mobile;

		@Schema(description = "会员卡号", nullable = true)
		public String user_card_code;

		@Schema(description = "公众号授权 appid", nullable = true)
		public String authorizer_appid;

		@Schema(description = "小程序 appid", nullable = true)
		public String wxa_appid;

		@Schema(description = "来源渠道 ID", nullable = true)
		public Long source_id;

		@Schema(description = "来源监控 ID", nullable = true)
		public Long monitor_id;

		@Schema(description = "最近来源渠道 ID", nullable = true)
		public Long latest_source_id;

		@Schema(description = "最近来源监控 ID", nullable = true)
		public Long latest_monitor_id;

		@Schema(description = "注册时间（Unix 秒）", nullable = true)
		public Long created;

		@Schema(description = "更新时间（Unix 秒）", nullable = true)
		public Long updated;

		@Schema(description = "注册年份", nullable = true)
		public Integer created_year;

		@Schema(description = "注册月份", nullable = true)
		public Integer created_month;

		@Schema(description = "注册日", nullable = true)
		public Integer created_day;

		@Schema(description = "线下卡号", nullable = true)
		public String offline_card_code;

		@Schema(description = "邀请人会员 ID", nullable = true)
		public Long inviter_id;

		@Schema(description = "注册来源", nullable = true)
		public String source_from;

		@Schema(description = "密码（库原值）", nullable = true)
		public String password;

		@Schema(description = "是否禁用", nullable = true)
		public Boolean disabled;

		@Schema(description = "是否使用积分", nullable = true)
		public Boolean use_point;

		@Schema(description = "备注", nullable = true)
		public String remarks;

		@Schema(description = "第三方数据", nullable = true)
		public String third_data;

		@Schema(description = "地区手机号", nullable = true)
		public String region_mobile;

		@Schema(description = "手机国家码", nullable = true)
		public String mobile_country_code;

		@Schema(description = "注册店铺 ID", nullable = true)
		public Integer reg_distributor;

		@Schema(description = "注册导购编号", nullable = true)
		public String reg_salesperson;

		@Schema(description = "归属导购编号", nullable = true)
		public String fp_salesperson;

		@Schema(description = "是否已分配导购", nullable = true)
		public Boolean has_fp;

		@Schema(description = "运营店铺 ID", nullable = true)
		public Integer op_distributor;

		@Schema(description = "用户名（解密后明文）", nullable = true)
		public String username;

		@Schema(description = "姓名", nullable = true)
		public String name;

		@Schema(description = "性别：0 未知；1 男；2 女", nullable = true)
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

		@Schema(description = "爱好（库原样 JSON 字符串，本列表不解码）", nullable = true)
		public String habbit;

		@Schema(description = "头像", nullable = true)
		public String avatar;

		@Schema(description = "微信 unionid（子查询 + 兜底补全）", nullable = true)
		public String unionid;

		@Schema(description = "微信 open_id（关联 members_wechatusers 子查询）", nullable = true)
		public String open_id;

		@Schema(description = "昵称（恒为空串，代码置空）", example = "")
		public String nickname;

		@Schema(description = "累计消费金额（分；全部订单 SUM(total_fee)，无订单为 0）", example = "9900")
		public Long total_amount;

		@Schema(description = "浏览足迹总数（无足迹为 0）", example = "3")
		public Long browse_count;

		@Schema(description = "最近浏览时间 yyyy-MM-dd HH:mm:ss（无足迹为空串）", example = "2026-11-26 12:00:00")
		public String browse_time;

		@Schema(description = "最近浏览商品名称（无足迹/商品缺失为 null）", nullable = true)
		public String browse_item_name;

		@Schema(description = "最近浏览商品 ID（无足迹为 null）", nullable = true, example = "1001")
		public Long browse_item_id;
	}
}