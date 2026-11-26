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

/**
 * 仅 OpenAPI 文档；data 新建会员行（29 固定键），来源
 * {@code OpenapiThirdApiV2MemberCreateService#toUserDataMap}（创建链完成后回读 members 行）。
 * 数值键 null 归 0；布尔键转整型 0/1；字符串键 null 归空串（third_data/reg_salesperson 除外，可为 null）。
 * 新建行大量字段为固定初始值（见各字段说明）。
 */
@Schema(name = "EcxMemberCreateData")
public class EcxMemberCreateDataDoc {

	@Schema(description = "新建会员 user_id（整型）", example = "50")
	public Long user_id;

	@Schema(description = "租户 company_id（整型）", example = "1")
	public Long company_id;

	@Schema(description = "等级 ID（整型；未指定时为租户默认等级）", example = "1")
	public Long grade_id;

	@Schema(description = "手机号（明文；region_mobile 优先，否则密文解密）", example = "13800138000")
	public String mobile;

	@Schema(description = "明文手机号（region_mobile 原样）", example = "13800138000")
	public String region_mobile;

	@Schema(description = "手机国家码（新建恒 86）", example = "86")
	public String mobile_country_code;

	@Schema(description = "会员卡号（入参或自动生成 12 位随机码）", example = "card")
	public String user_card_code;

	@Schema(description = "线下卡号（新建恒空串）", example = "")
	public String offline_card_code;

	@Schema(description = "推荐人 user_id（整型；无推荐人 0）", example = "0")
	public Long inviter_id;

	@Schema(description = "来源渠道（default/openapi）", example = "openapi")
	public String source_from;

	@Schema(description = "订单来源 id（新建恒 0）", example = "0")
	public Long source_id;

	@Schema(description = "订单监控页面 id（新建恒 0）", example = "0")
	public Long monitor_id;

	@Schema(description = "最近来源 id（新建恒 0）", example = "0")
	public Long latest_source_id;

	@Schema(description = "最近监控 id（新建恒 0）", example = "0")
	public Long latest_monitor_id;

	@Schema(description = "微信授权方 appid（新建恒空串）", example = "")
	public String authorizer_appid;

	@Schema(description = "是否使用积分（整型 0/1；新建恒 0）", example = "0")
	public Integer use_point;

	@Schema(description = "微信小程序 appid（新建恒空串）", example = "")
	public String wxa_appid;

	@Schema(description = "支付宝小程序 appid（新建恒空串）", example = "")
	public String alipay_appid;

	@Schema(description = "创建时间（Unix 秒，整型；示例为格式演示值）", example = "1735689600")
	public Long created;

	@Schema(description = "更新时间（Unix 秒，整型；新建与 created 相同；示例为格式演示值）", example = "1735689600")
	public Long updated;

	@Schema(description = "是否禁用（整型 0/1；status!=1 时 1）", example = "0")
	public Integer disabled;

	@Schema(description = "备注（入参原样，缺省空串）", example = "")
	public String remarks;

	@Schema(description = "第三方数据（字符串；新建恒 null）", nullable = true)
	public String third_data;

	@Schema(description = "注册店铺 id（新建恒 0）", example = "0")
	public Integer reg_distributor;

	@Schema(description = "注册导购（字符串；新建恒 null）", nullable = true)
	public String reg_salesperson;

	@Schema(description = "首绑导购标识（字符串；新建恒 null，本服务链内未回填）", nullable = true)
	public String fp_salesperson;

	@Schema(description = "是否已首绑导购（整型 0/1；新建恒 0）", example = "0")
	public Integer has_fp;

	@Schema(description = "是否已成为好友（整型 0/1；新建恒 0）", example = "0")
	public Integer is_become_friend;

	@Schema(description = "操作店铺 id（新建恒 0）", example = "0")
	public Integer op_distributor;
}