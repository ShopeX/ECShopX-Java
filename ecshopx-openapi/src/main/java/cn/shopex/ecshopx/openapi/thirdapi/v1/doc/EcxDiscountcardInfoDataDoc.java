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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷。
 *
 * <p>字段来源：{@code DiscountCardKaquanDetailFacadeService#loadDetailInternal}
 *（基础行 {@code DiscountCardsRowMapperService#snakeCaseMapFrom} + 关联聚合），
 * 经 {@code OpenapiThirdApiV1DiscountCardGetDiscountCardDetailService#execute} 移除 user_tag_ids、
 * {@code OpenapiDiscountCardDetailPresentationService#apply} 移除 time_limit 并做 OpenAPI 展示层转换
 *（begin_time/end_time/days、discount 折扣换算、least_cost/reduce_cost 分转元）。
 */
@Schema(name = "EcxDiscountcardInfoData", description = "卡券模板详情（含关联店铺/商品/标签/品牌聚合）")
public class EcxDiscountcardInfoDataDoc {

	@Schema(description = "卡券模板 ID", example = "3")
	public Long card_id;

	@Schema(description = "租户/公司 ID", example = "100")
	public Long company_id;

	@Schema(description = "卡券类型：discount 折扣券；cash 代金券；gift 兑换券；new_gift 兑换券(新)", example = "cash")
	public String card_type;

	@Schema(description = "商户名称", nullable = true)
	public String brand_name;

	@Schema(description = "卡券商户 logo", nullable = true)
	public String logo_url;

	@Schema(description = "卡券名；存在 zh-CN 多语言分表值时被覆盖", example = "满减券")
	public String title;

	@Schema(description = "券颜色值")
	public String color;

	@Schema(description = "卡券使用提醒", nullable = true)
	public String notice;

	@Schema(description = "卡券使用说明；存在 zh-CN 多语言分表值时被覆盖", example = "")
	public String description;

	@Schema(description = "有效期类型：DATE_TYPE_FIX_TERM 固定时间段；DATE_TYPE_LONG 领取后N天生效；DATE_TYPE_SHORT 短期", example = "DATE_TYPE_FIX_TERM")
	public String date_type;

	@Schema(description = "有效期开始时间（Unix 秒；语义随 date_type）", nullable = true, example = "1764115200")
	public Integer begin_date;

	@Schema(description = "有效期结束时间（Unix 秒）", nullable = true, example = "1766707200")
	public Integer end_date;

	@Schema(description = "begin_date 的整型化（null→0），OpenAPI 展示层写入", example = "1764115200")
	public Integer begin_time;

	@Schema(description = "end_date 的整型化（null→0），OpenAPI 展示层写入", example = "1766707200")
	public Integer end_time;

	@Schema(description = "fixed_term 的整型化；fixed_term 为 null 时默认 30，OpenAPI 展示层写入", example = "30")
	public Integer days;

	@Schema(description = "有效期有效天数（领取后生效天数）", nullable = true)
	public Integer fixed_term;

	@Schema(description = "客服电话", nullable = true)
	public String service_phone;

	@Schema(description = "卡券顶部居中按钮文案", nullable = true)
	public String center_title;

	@Schema(description = "入口下方提示语", nullable = true)
	public String center_sub_title;

	@Schema(description = "顶部居中 URL", nullable = true)
	public String center_url;

	@Schema(description = "自定义外链入口名称", nullable = true)
	public String custom_url_name;

	@Schema(description = "自定义跳转 URL", nullable = true)
	public String custom_url;

	@Schema(description = "入口右侧提示语", nullable = true)
	public String custom_url_sub_title;

	@Schema(description = "营销场景入口名称", nullable = true)
	public String promotion_url_name;

	@Schema(description = "营销场景入口 URL", nullable = true)
	public String promotion_url;

	@Schema(description = "营销入口右侧提示语", nullable = true)
	public String promotion_url_sub_title;

	@Schema(description = "每人可领券数量限制", nullable = true)
	public Integer get_limit;

	@Schema(description = "每人可核销数量限制", nullable = true)
	public Integer use_limit;

	@Schema(description = "领取页面是否可分享（库值 \"1\"/\"true\"/1 → true）", example = "false")
	public Boolean can_share;

	@Schema(description = "是否可转赠（同上布尔化）", example = "false")
	public Boolean can_give_friend;

	@JsonProperty("abstract")
	@Schema(description = "封面摘要（库列 abstract）", nullable = true)
	public String coverAbstract;

	@Schema(description = "封面图片（库列 icon_url_list，字符串原样）", nullable = true)
	public String icon_url_list;

	@Schema(
			description =
					"图文列表：库中 JSON/PHP 序列化数组解码后原样透传（元素通常为 {image_url, text}），"
							+ "空或非法时为 null；元素键由创建方数据决定",
			nullable = true)
	public Object text_image_list;

	@Schema(description = "兑换券兑换内容名称", nullable = true)
	public String gift;

	@Schema(description = "优惠券优惠详情", nullable = true)
	public String default_detail;

	@Schema(
			description =
					"折扣：库存百分比值 >0 时展示层换算为 (100-值)/10 的折数（如 20 → 8.0 折）；"
							+ "0/null 保持整型原值",
			example = "0")
	public Object discount;

	@Schema(
			description = "代金券起用金额：有有效值（非 0/null/空）时展示层除以 100 转为元（Double）；否则保持整型原值（分）",
			example = "0")
	public Object least_cost;

	@Schema(
			description = "代金券减免金额/兑换券起用金额：同 least_cost，有效值时除以 100 转为元",
			example = "0")
	public Object reduce_cost;

	@Schema(description = "团购券详情", nullable = true)
	public String deal_detail;

	@Schema(description = "指定可用商品类目（代金券专用）", nullable = true)
	public String accept_category;

	@Schema(description = "指定不可用商品类目（代金券专用）", nullable = true)
	public String reject_category;

	@Schema(description = "购买 xx 可用类型门槛（兑换用）", nullable = true)
	public String object_use_for;

	@Schema(description = "是否可与其他优惠共享：字符串 \"true\"/\"false\"（库值 \"1\"/1/true → \"true\"）", example = "false")
	public String can_use_with_other_discount;

	@Schema(description = "适用平台：mall 线上商城；store 门店", example = "store")
	public String use_platform;

	@Schema(description = "卡券发行数量", example = "100")
	public Integer quantity;

	@Schema(
			description =
					"是否适用所有门店：字符串 \"true\"/\"false\"；注意库存字面 \"true\" 映射为 \"false\"，"
							+ "仅数值 1/\"1\"/布尔 true → \"true\"",
			example = "false")
	public String use_all_shops;

	@ArraySchema(arraySchema = @Schema(description = "适用门店 ID 列表（库逗号串拆分）"))
	public List<String> rel_shops_ids;

	@Schema(
			description =
					"核销场景：ONLINE 线上商城；QUICK 快捷买单；SWEEP 门店支付；SELF 到店支付；"
							+ "库值为多场景逗号串时归一为 QUICK",
			example = "QUICK")
	public String use_scenes;

	@Schema(description = "自助核销验证码", nullable = true)
	public Integer self_consume_code;

	@Schema(description = "是否前台直接领取（库值 \"1\"/1 → true）", example = "false")
	public Boolean receive;

	@Schema(description = "创建时间（Unix 秒）")
	public Integer created;

	@Schema(description = "更新时间（Unix 秒）")
	public Integer updated;

	@Schema(description = "代金券最高消费限额（分）", nullable = true)
	public Integer most_cost;

	@Schema(description = "适用范围：0 全场；1 指定商品；2 指定分类；3 指定商品标签；4 指定商品品牌", example = "0")
	public Integer use_bound;

	@ArraySchema(arraySchema = @Schema(description = "商品标签 ID 列表（字符串）"))
	public List<String> tag_ids;

	@ArraySchema(arraySchema = @Schema(description = "品牌 ID 列表（字符串）"))
	public List<String> brand_ids;

	@Schema(description = "适用范围原始串", nullable = true)
	public String apply_scope;

	@Schema(description = "第三方优惠券模板编码", nullable = true)
	public String card_code;

	@Schema(description = "第三方优惠券规则编码", nullable = true)
	public String card_rule_code;

	@Schema(description = "卡券状态：0 正常；1 暂停；2 关闭；10 初始化（new_gift 创建中）", example = "0")
	public Integer kq_status;

	@Schema(description = "发放开始时间（Unix 秒）", nullable = true)
	public Integer send_begin_time;

	@Schema(description = "发放结束时间（Unix 秒）", nullable = true)
	public Integer send_end_time;

	@Schema(description = "兑换商品后的锁定时间", example = "0")
	public Integer lock_time;

	@Schema(description = "添加者类型（如 distributor）", nullable = true)
	public String source_type;

	@Schema(description = "添加者 ID（如店铺 ID）", example = "0")
	public Long source_id;

	@Schema(description = "达摩 CRM 卡券 ID", nullable = true)
	public String dm_card_id;

	@Schema(description = "达摩 CRM 适用渠道：offlineStore 线下门店；ThirdmicroMall 第三方商城", nullable = true)
	public String dm_use_channel;

	@ArraySchema(arraySchema = @Schema(description = "指定会员等级 ID 列表（字符串，CSV 归一）"))
	public List<String> grade_ids;

	@ArraySchema(arraySchema = @Schema(description = "指定付费会员等级 ID 列表（字符串，CSV 归一）"))
	public List<String> vip_grade_ids;

	@Schema(description = "券类型：mall 商城券；guide 导购专属券", example = "mall")
	public String coupon_type;

	@Schema(description = "导购发放数量", example = "0")
	public Integer guide_issue_quantity;

	@ArraySchema(
			arraySchema = @Schema(description = "适用店铺 ID 列表（distributor_id 逗号串中的纯数字项）；无指定店铺时为 null"),
			schema = @Schema(nullable = true))
	public List<String> rel_distributor_ids;

	@ArraySchema(arraySchema = @Schema(description = "适用店铺信息列表；无指定店铺时为空数组；有 ID 但查无店铺时为 [[]]"))
	public List<EcxDiscountcardInfoDistributorRowDoc> distributor_info;

	@Schema(description = "是否适用全部店铺（distributor_info 为空时 true）", example = "true")
	public Boolean use_all_distributor;

	@ArraySchema(arraySchema = @Schema(description = "指定商品 ID 列表（字符串，rel_items normal 行）；无指定商品时为空数组"))
	public List<String> rel_item_ids;

	@ArraySchema(arraySchema = @Schema(description = "指定商品 SKU 树（仅 rel_item_ids 非空时存在）"))
	public List<EcxDiscountcardInfoItemTreeRowDoc> itemTreeLists;

	@Schema(
			description =
					"商品适用范围标记：\"true\" 全场（无指定商品）；\"false\" 指定商品；\"category\" 指定分类；"
							+ "\"tag\" use_bound=3；\"brand\" use_bound=4",
			example = "true")
	public String use_all_items;

	@ArraySchema(arraySchema = @Schema(description = "指定分类 ID 列表（仅存在 category 关联时返回）"))
	public List<Long> rel_category_ids;

	@ArraySchema(arraySchema = @Schema(description = "指定分类 ID 列表（与 rel_category_ids 同值，仅存在时返回）"))
	public List<Long> item_category;

	@ArraySchema(arraySchema = @Schema(description = "商品标签 ID 列表（与 tag_ids 同值）"))
	public List<String> rel_tag_ids;

	@ArraySchema(arraySchema = @Schema(description = "商品标签明细列表；无标签时空数组"))
	public List<EcxDiscountcardInfoTagRowDoc> tag_list;

	@ArraySchema(arraySchema = @Schema(description = "品牌 ID 列表（与 brand_ids 同值）"))
	public List<String> rel_brand_ids;

	@ArraySchema(arraySchema = @Schema(description = "品牌明细列表；无品牌时空数组"))
	public List<EcxDiscountcardInfoBrandRowDoc> brand_list;

	@Schema(
			description =
					"new_gift 券是否已生效：按 date_type 与 begin_date/send_begin_time 推算已过生效点，"
							+ "且 kq_status ≠ 10（初始化）；其他券类型恒 false",
			example = "false")
	public Boolean is_active;

	@ArraySchema(
			arraySchema = @Schema(description = "使用时段类型去重列表（原 time_limit 各元素 type，如 MONDAY）；仅原 time_limit 非空时存在"))
	public List<String> time_limit_type;

	@Schema(
			description =
					"使用时段：固定键 \"1\"（首个时段）与可选 \"2\"（第二个不同开始时间的时段），"
							+ "值为 {begin_time:\"H:M\", end_time:\"H:M\"}；仅原 time_limit 含完整时分字段时存在",
			example = "{\"1\":{\"begin_time\":\"10:0\",\"end_time\":\"22:0\"}}")
	public Map<String, TimeSlotDoc> time_limit_date;

	@Schema(name = "EcxDiscountcardInfoTimeSlot", description = "单个使用时段")
	public static class TimeSlotDoc {

		@Schema(description = "开始时间，begin_hour:begin_minute 拼接", example = "10:0")
		public String begin_time;

		@Schema(description = "结束时间，end_hour:end_minute 拼接", example = "22:0")
		public String end_time;
	}
}