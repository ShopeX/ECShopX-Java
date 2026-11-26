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
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 仅 OpenAPI 文档；{@code data.list} 单条卡券。
 *
 * <p><b>两种形态</b>（来源 {@code OpenapiThirdApiV1DiscountCardGetDiscountCardListService#execute}
 * + {@code OpenapiDiscountCardListPresentationService#apply}）：
 * <ul>
 *   <li><b>完整行</b>：{@code DiscountCardsRowMapperService#snakeCaseMapFrom} 的全部原始列（多为库原样字符串）
 *       + get_num/use_num/time_limit/date_status，条件键 begin_day_type；grade_ids/vip_grade_ids 空列表→""，
 *       tag_ids/brand_ids 空列表→null。用于 date_type 非 FIX_TERM/FIX_TIME_RANGE，或 FIX_TIME_RANGE 但日期为空。</li>
 *   <li><b>精简展示卡</b>：当 date_type=DATE_TYPE_FIX_TERM（takeEffect 非空）或 DATE_TYPE_FIX_TIME_RANGE
 *       且 begin/end 均可格式化时，整行被替换为仅含以下键：card_id、title、color、card_type、date_type、
 *       card_type_val、discount、least_cost、reduce_cost、quantity、get_num、card_num、takeEffect、begin_time、end_time。</li>
 * </ul>
 * 下列 schema 列出两形态的并集；标注「精简卡」的键仅在精简形态出现，标注「完整行」的键仅在完整形态出现。
 */
@Schema(name = "EcxDiscountcardListItem", description = "卡券列表单条（完整行或精简展示卡，见类说明）")
public class EcxDiscountcardListItemDoc {

	@Schema(description = "卡券模板 ID（两形态均有）", example = "3")
	public Long card_id;

	@Schema(description = "卡券名（两形态均有；zh-CN 分表覆盖）", example = "满减券")
	public String title;

	@Schema(description = "券颜色值（两形态均有）", nullable = true)
	public String color;

	@Schema(description = "卡券类型：cash/discount/gift/new_gift（两形态均有）", example = "cash")
	public String card_type;

	@Schema(description = "有效期类型（两形态均有）", example = "DATE_TYPE_FIX_TERM")
	public String date_type;

	@Schema(description = "折扣券打折额度（百分比，库原值；两形态均有，不做换算）", example = "0")
	public Integer discount;

	@Schema(description = "代金券起用金额（分，库原值；两形态均有）", example = "0")
	public Integer least_cost;

	@Schema(description = "代金券减免金额/兑换券起用金额（分，库原值；两形态均有）", example = "0")
	public Integer reduce_cost;

	@Schema(description = "卡券发行数量（两形态均有，整型化）", example = "100")
	public Integer quantity;

	@Schema(description = "已领取数量（两形态均有，整型化）", example = "10")
	public Integer get_num;

	@Schema(description = "【精简卡】卡券类型中文名：折扣券/代金券/兑换券；未知为空串", example = "代金券")
	public String card_type_val;

	@Schema(description = "【精简卡】剩余可发放数：quantity>get_num>0 时为差值，否则 0", example = "90")
	public Integer card_num;

	@Schema(description = "【精简卡】生效文案，如「领取后当天生效,30天有效」；FIX_TIME_RANGE 形态为空串", example = "领取后当天生效,30天有效")
	public String takeEffect;

	@Schema(description = "生效时间：精简卡 FIX_TIME_RANGE 为 yyyy-MM-dd HH:mm:ss，FIX_TERM 为空串；完整行为 begin_date 库原值", example = "")
	public Object begin_time;

	@Schema(description = "失效时间：同 begin_time 规则", example = "")
	public Object end_time;

	@Schema(description = "【完整行】租户/公司 ID", example = "100")
	public Long company_id;

	@Schema(description = "【完整行】商户名称", nullable = true)
	public String brand_name;

	@Schema(description = "【完整行】卡券商户 logo", nullable = true)
	public String logo_url;

	@Schema(description = "【完整行】卡券使用提醒", nullable = true)
	public String notice;

	@Schema(description = "【完整行】卡券使用说明（zh-CN 分表覆盖）", nullable = true)
	public String description;

	@Schema(description = "【完整行】有效期开始（Unix 秒）；FIX_TERM/LONG 时被改写为当前时间", nullable = true)
	public Integer begin_date;

	@Schema(description = "【完整行】有效期结束（Unix 秒）；FIX_TERM/LONG 且原值≤0 时改写为 now+fixed_term*86400", nullable = true)
	public Integer end_date;

	@Schema(description = "【完整行】有效期有效天数", nullable = true)
	public Integer fixed_term;

	@Schema(description = "【完整行】客服电话", nullable = true)
	public String service_phone;

	@Schema(description = "【完整行】顶部居中按钮文案", nullable = true)
	public String center_title;

	@Schema(description = "【完整行】入口下方提示语", nullable = true)
	public String center_sub_title;

	@Schema(description = "【完整行】顶部居中 URL", nullable = true)
	public String center_url;

	@Schema(description = "【完整行】自定义外链入口名称", nullable = true)
	public String custom_url_name;

	@Schema(description = "【完整行】自定义跳转 URL", nullable = true)
	public String custom_url;

	@Schema(description = "【完整行】入口右侧提示语", nullable = true)
	public String custom_url_sub_title;

	@Schema(description = "【完整行】营销场景入口名称", nullable = true)
	public String promotion_url_name;

	@Schema(description = "【完整行】营销场景入口 URL", nullable = true)
	public String promotion_url;

	@Schema(description = "【完整行】营销入口右侧提示语", nullable = true)
	public String promotion_url_sub_title;

	@Schema(description = "【完整行】每人可领券数量限制", nullable = true)
	public Integer get_limit;

	@Schema(description = "【完整行】每人可核销数量限制", nullable = true)
	public Integer use_limit;

	@Schema(description = "【完整行】是否可分享（库原样字符串，如 \"false\"/\"1\"）", nullable = true)
	public String can_share;

	@Schema(description = "【完整行】是否可转赠（库原样字符串）", nullable = true)
	public String can_give_friend;

	@JsonProperty("abstract")
	@Schema(description = "【完整行】封面摘要", nullable = true)
	public String coverAbstract;

	@Schema(description = "【完整行】封面图片（字符串原样）", nullable = true)
	public String icon_url_list;

	@Schema(description = "【完整行】图文列表（字符串原样，未解码）", nullable = true)
	public String text_image_list;

	@Schema(description = "【完整行】使用时段：解码后的对象数组；空/无值时为 null", nullable = true)
	public Object time_limit;

	@Schema(description = "【完整行】兑换券兑换内容名称", nullable = true)
	public String gift;

	@Schema(description = "【完整行】优惠券优惠详情", nullable = true)
	public String default_detail;

	@Schema(description = "【完整行】团购券详情", nullable = true)
	public String deal_detail;

	@Schema(description = "【完整行】指定可用商品类目", nullable = true)
	public String accept_category;

	@Schema(description = "【完整行】指定不可用商品类目", nullable = true)
	public String reject_category;

	@Schema(description = "【完整行】购买 xx 可用类型门槛", nullable = true)
	public String object_use_for;

	@Schema(description = "【完整行】是否可与其他优惠共享（库原样字符串）", nullable = true)
	public String can_use_with_other_discount;

	@Schema(description = "【完整行】适用平台：mall/store", example = "store")
	public String use_platform;

	@Schema(description = "【完整行】是否适用所有门店（库原样字符串）", nullable = true)
	public String use_all_shops;

	@Schema(description = "【完整行】适用门店（库逗号串原样，未拆分）", nullable = true)
	public String rel_shops_ids;

	@Schema(description = "【完整行】核销场景：ONLINE/QUICK/SWEEP/SELF", example = "QUICK")
	public String use_scenes;

	@Schema(description = "【完整行】自助核销验证码", nullable = true)
	public Integer self_consume_code;

	@Schema(description = "【完整行】是否前台直接领取（库原样字符串）", nullable = true)
	public String receive;

	@Schema(description = "【完整行】店铺 ID（库逗号串原样）", nullable = true)
	public String distributor_id;

	@Schema(description = "【完整行】创建时间（Unix 秒）")
	public Integer created;

	@Schema(description = "【完整行】更新时间（Unix 秒）")
	public Integer updated;

	@Schema(description = "【完整行】代金券最高消费限额（分）", nullable = true)
	public Integer most_cost;

	@Schema(description = "【完整行】适用范围：0 全场；1 商品；2 分类；3 标签；4 品牌", example = "0")
	public Integer use_bound;

	@Schema(description = "【完整行】商品标签 ID 列表；空列表归一为 null", nullable = true)
	public Object tag_ids;

	@Schema(description = "【完整行】品牌 ID 列表；空列表归一为 null", nullable = true)
	public Object brand_ids;

	@Schema(description = "【完整行】适用范围原始串", nullable = true)
	public String apply_scope;

	@Schema(description = "【完整行】第三方优惠券模板编码", nullable = true)
	public String card_code;

	@Schema(description = "【完整行】第三方优惠券规则编码", nullable = true)
	public String card_rule_code;

	@Schema(description = "【完整行】卡券状态：0 正常；1 暂停；2 关闭；10 初始化", example = "0")
	public Integer kq_status;

	@Schema(description = "【完整行】发放开始时间（Unix 秒）", nullable = true)
	public Integer send_begin_time;

	@Schema(description = "【完整行】发放结束时间（Unix 秒）", nullable = true)
	public Integer send_end_time;

	@Schema(description = "【完整行】兑换商品后锁定时间", example = "0")
	public Integer lock_time;

	@Schema(description = "【完整行】添加者类型", nullable = true)
	public String source_type;

	@Schema(description = "【完整行】添加者 ID", example = "0")
	public Long source_id;

	@Schema(description = "【完整行】达摩 CRM 卡券 ID", nullable = true)
	public String dm_card_id;

	@Schema(description = "【完整行】达摩 CRM 适用渠道", nullable = true)
	public String dm_use_channel;

	@Schema(description = "【完整行】指定会员等级 ID 列表；空列表归一为 \"\"", example = "")
	public Object grade_ids;

	@Schema(description = "【完整行】指定付费会员等级 ID 列表；空列表归一为 \"\"", example = "")
	public Object vip_grade_ids;

	@Schema(description = "【完整行】券类型；本接口筛选强制 coupon_type=guide", example = "guide")
	public String coupon_type;

	@Schema(description = "【完整行】导购发放数量", example = "0")
	public Integer guide_issue_quantity;

	@Schema(description = "【完整行】已核销数量", example = "0")
	public Integer use_num;

	@Schema(description = "【完整行】日期状态：0 未知；1 未开始；2 进行中；3 已结束", example = "2")
	public Integer date_status;

	@Schema(description = "【完整行】原 begin_date（仅 date_type=FIX_TERM/LONG 时存在，用于保留相对天数）", example = "0")
	public Integer begin_day_type;
}