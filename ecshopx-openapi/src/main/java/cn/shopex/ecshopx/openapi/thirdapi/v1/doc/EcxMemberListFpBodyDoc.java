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
 * 仅 OpenAPI 文档；POST 请求可选 JSON body，字段来源 {@code MemberController#memberList} 经
 * {@code OpenapiMemberListParams#buildMergedParams} 读取的全部 key（15 个）；校验见
 * {@code OpenapiThirdApiV1MemberListService#validateMemberListParams}。
 */
@Schema(
		name = "EcxMemberListFpBody",
		description =
				"会员列表查询参数（body 与 query 同名 key 双通道：body 含键时取原始值，否则用 query）。"
						+ " 各键仅在提供（键存在）时校验；PHP-empty（空/\"0\"/0/false）值视为未提供筛选")
public class EcxMemberListFpBodyDoc {

	@Schema(
			description = "查询范围：fp 会员分页列表（默认）；customer 导购客户 unionid 数组（此时 salesperson_code 必填）；其他值报错",
			defaultValue = "fp",
			allowableValues = {"fp", "customer"},
			example = "fp")
	public String scope;

	@Schema(description = "页码；提供时须为正整数（否则「页码必须为正整数」）；缺省 1", defaultValue = "1", example = "1")
	public String page;

	@Schema(
			description = "每页条数；提供时须为整数（否则「每页数量必须在1-500之间」，代码未强制上限）；缺省/小于 1 按 10",
			defaultValue = "10",
			example = "10")
	public String page_size;

	@Schema(description = "生日开始日期 YYYY-MM-DD；格式非法报「生日开始日期格式错误，应为YYYY-MM-DD」", example = "2000-01-01")
	public String birthday_start;

	@Schema(description = "生日结束日期 YYYY-MM-DD；格式非法报「生日结束日期格式错误，应为YYYY-MM-DD」", example = "2000-12-31")
	public String birthday_end;

	@ArraySchema(
			arraySchema = @Schema(description = "会员标签 ID 数组（body 须为数组，元素整数；query 可重复传参）；命中任一标签"),
			schema = @Schema(example = "1"))
	public List<Long> tag_id;

	@Schema(
			description = "分配状态：noassign 未分配导购（has_fp=0）；assign 已分配且未加好友（has_fp=1 且 is_become_friend=0）；其他值报错",
			allowableValues = {"noassign", "assign"},
			example = "assign")
	public String type;

	@Schema(
			description = "导购编号（fp_salesperson 精确匹配）；须为字符串或数字；scope=customer 时必填",
			example = "E9")
	public String salesperson_code;

	@Schema(description = "门店编号（shop_code → op_distributor 精确匹配）；须为字符串或数字；查无门店返回空列表", example = "S001")
	public String store_bn;

	@Schema(description = "积分下限（含）；提供时须为非负整数", example = "0")
	public String point_start;

	@Schema(description = "积分上限（含）；提供时须为非负整数", example = "1000")
	public String point_end;

	@Schema(description = "会员卡等级 ID；提供时须为正整数", example = "1")
	public String grade_id;

	@Schema(
			description = "累计购买金额下限（含，单位分）；提供时须为非负整数；customer 范围按会员累计消费过滤",
			example = "0")
	public String buy_start;

	@Schema(description = "累计购买金额上限（含，单位分）；提供时须为非负整数", example = "100000")
	public String buy_end;

	@Schema(
			description = "关键词（按手机号模糊查询）；须为字符串或数字，否则报「关键词（用于按手机号查询）」",
			example = "13800000000")
	public String keyword;
}