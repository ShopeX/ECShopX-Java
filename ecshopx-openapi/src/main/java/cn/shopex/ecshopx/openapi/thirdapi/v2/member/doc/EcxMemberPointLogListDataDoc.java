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
 * 仅 OpenAPI 文档；data 分页结构，来源
 * {@code OpenapiThirdApiV2MemberTagListService#formatListStruct}（4 固定键）。
 * is_last_page 为整型 0/1（ceil(total_count/page_size) ≤ page → 1）。
 */
@Schema(name = "EcxMemberPointLogListData")
public class EcxMemberPointLogListDataDoc {

	@Schema(description = "满足过滤条件的积分流水总数（整型）", example = "1")
	public Long total_count;

	@Schema(description = "是否最后一页（整型 0/1，非布尔）", example = "1")
	public Integer is_last_page;

	@Schema(description = "分页回显（生效值：page≥1、page_size∈[20,499]）")
	public PagerDoc pager;

	@Schema(description = "积分流水行数组（id 降序；无结果为空数组）")
	public List<EcxMemberPointLogListRowDoc> list;

	/** 分页回显（page/page_size）。 */
	@Schema(name = "EcxMemberPointLogListPager")
	public static class PagerDoc {

		@Schema(description = "生效页码（≥1）", example = "1")
		public Integer page;

		@Schema(description = "生效每页数量（[20,499]）", example = "20")
		public Integer page_size;
	}

	/** 积分流水行（11 固定键），来源 formatPointLogRows。 */
	@Schema(name = "EcxMemberPointLogListRow")
	public static class EcxMemberPointLogListRowDoc {

		@Schema(description = "流水 ID（point_member_log.id，整型，降序排序键）", example = "1")
		public Long id;

		@Schema(description = "会员当前手机号（按 user_id 回查 members.mobile 解密明文；查无/空为空串）", example = "13800138000")
		public String mobile;

		@Schema(description = "操作人（库内原样；null 为空串）", example = "外部开发者")
		public String operater;

		@Schema(
				description =
						"流水类型（journal_type，整型；null 按 0）：1 注册赠送/2 邀请注册赠送/3 充值赠送/4 储值兑换/"
								+ "5 积分换购/6 消费购物（支出）/7 消费购物（获取）/8 会员等级返佣/9 取消订单返还/10 退款返还/"
								+ "11 大转盘/12 商家手动修改/13 开放接口/14 分销佣金（积分）/15 商家导入修改/"
								+ "16 活动报名送积分/17 会员签到日常积分/18 会员签到活动奖励；未知类型描述按「其他」",
				example = "13")
		public Integer type;

		@Schema(
				description =
						"描述（服务端拼接：「系统 于{created Unix 秒} 给 会员({手机号}) {类型名} {±积分}」；"
								+ "**源码缺陷：支出分支拼的是 -income 而非 -outcome，纯支出流水显示「-0」**；"
								+ "收支均 0 时积分段为空；示例为格式演示值）",
				example = "系统 于1735689600 给 会员(13800138000) 开放接口 +10")
		public String description;

		@Schema(description = "增加积分（income，整型；null 按 0）", example = "10")
		public Integer increase_point;

		@Schema(description = "减少积分（outcome，整型正数；null 按 0）", example = "0")
		public Integer decrease_point;

		@Schema(
				description = "备注：type=13（开放接口）取 operater_remark，其他类型取 point_desc；null 为空串")
		public String record;

		@Schema(description = "关联订单号（字符串；null 为空串）", example = "100")
		public String order_id;

		@Schema(description = "外部唯一标识（开放接口修改积分时传入；null 为空串）", example = "1")
		public String external_id;

		@Schema(
				description = "创建时间（yyyy-MM-dd HH:mm:ss，系统时区；null/≤0 为空串；示例为格式演示值）",
				example = "2026-01-01 00:00:00")
		public String created;
	}
}