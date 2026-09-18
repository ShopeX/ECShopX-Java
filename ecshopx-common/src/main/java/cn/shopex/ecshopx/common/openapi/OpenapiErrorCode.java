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

package cn.shopex.ecshopx.common.openapi;

/** OpenAPI 错误码（对齐 PHP {@code OpenapiBundle\Constants\ErrorCode} 常用项）。 */
public final class OpenapiErrorCode {

	public static final String SUCCESS = "E0000";
	public static final String BUSINESS_ERROR = "E0001";
	public static final String VALIDATION_APPKEY_ERROR = "E4003";
	public static final String SIGN_ERROR = "E4004";
	public static final String METHOD_NOT_FOUND = "E4005";
	public static final String VALIDATION_MISSING_PARAMS = "E4001";
	public static final String VALIDATION_TIMESTAMP_ERROR = "E4002";
	public static final String NOT_IMPLEMENTED = "E0001";
	public static final String SYSTEM_ERROR = "E9999";
	public static final String SERVICE_MISSING_PARAMS = "E5001";
	public static final String SERVICE_PARAMS_FORMAT_ERROR = "E5002";
	/** 对齐 PHP ErrorCode::ORDER_ERROR — OpenAPI is_dada=1 未开启同城配 */
	public static final String ORDER_ERROR = "E5300";
	public static final String ORDER_NOT_FOUND = "E5301";
	public static final String ORDER_HANDLE_ERROR = "E5310";
	public static final String ORDER_HANDLE_EXIST = "E5312";
	/** 对齐 PHP ErrorCode::ORDER_AFTERSALES_HANDLE_ERROR */
	public static final String ORDER_AFTERSALES_HANDLE_ERROR = "E5320";
	/** 对齐 PHP ErrorCode::ORDER_MEMBER_NOT_FOUND */
	public static final String ORDER_MEMBER_NOT_FOUND = "E5331";
	/** 对齐 PHP ErrorCode::ORDER_REFUND_HANDLE_ERROR */
	public static final String ORDER_REFUND_HANDLE_ERROR = "E5340";
	/** 对齐 PHP ErrorCode::MEMBER_TAGCATEGORY_EXIST */
	public static final String MEMBER_TAGCATEGORY_EXIST = "E5572";
	/** 对齐 PHP ErrorCode::MEMBER_TAGCATEGORY_NOT_FOUND */
	public static final String MEMBER_TAGCATEGORY_NOT_FOUND = "E5571";
	/** 对齐 PHP ErrorCode::MEMBER_TAG_EXIST */
	public static final String MEMBER_TAG_EXIST = "E5562";
	/** 对齐 PHP ErrorCode::MEMBER_TAG_NOT_FOUND */
	public static final String MEMBER_TAG_NOT_FOUND = "E5561";
	/** 对齐 PHP ErrorCode::MEMBER_NOT_FOUND */
	public static final String MEMBER_NOT_FOUND = "E5501";
	/** 对齐 PHP ErrorCode::MEMBER_TAG_ERROR */
	public static final String MEMBER_TAG_ERROR = "E5560";
	/** 对齐 PHP ErrorCode::MEMBER_RECHARGE_ERROR */
	public static final String MEMBER_RECHARGE_ERROR = "E5580";
	/** 对齐 PHP ErrorCode::MEMBER_RECHARGE_NOT_FOUND */
	public static final String MEMBER_RECHARGE_NOT_FOUND = "E5581";
	/** 对齐 PHP ErrorCode::GOODS_BRAND_ERROR */
	public static final String GOODS_BRAND_ERROR = "E5110";
	/** 对齐 PHP ErrorCode::GOODS_BRAND_DELETE_ERROR */
	public static final String GOODS_BRAND_DELETE_ERROR = "E5113";
	/** 对齐 PHP ErrorCode::GOODS_CATEGORY_ERROR */
	public static final String GOODS_CATEGORY_ERROR = "E5120";
	/** 对齐 PHP ErrorCode::GOODS_CATEGORY_DELETE_ERROR */
	public static final String GOODS_CATEGORY_DELETE_ERROR = "E5123";
	/** 对齐 PHP ErrorCode::GOODS_MAINCATEGORY_ERROR */
	public static final String GOODS_MAINCATEGORY_ERROR = "E5130";
	/** 对齐 PHP ErrorCode::GOODS_MAINCATEGORY_NOT_FOUND */
	public static final String GOODS_MAINCATEGORY_NOT_FOUND = "E5131";
	/** 对齐 PHP ErrorCode::GOODS_ERROR */
	public static final String GOODS_ERROR = "E5100";
	/** 对齐 PHP ErrorCode::GOODS_DELETE_ERROR */
	public static final String GOODS_DELETE_ERROR = "E5103";
	/** 对齐 PHP ErrorCode::GOODS_NOT_FOUND */
	public static final String GOODS_NOT_FOUND = "E5101";
	/** 对齐 PHP ErrorCode::DISTRIBUTOR_NOT_FOUND */
	public static final String DISTRIBUTOR_NOT_FOUND = "E5401";
	/** 对齐 PHP ErrorCode::WECHAT_ERROR — @message("微信小程序信息有误")；OpenAPI download 自定义文案「没有开通此小程序」 */
	public static final String WECHAT_ERROR = "E5700";
	/** 对齐 PHP ErrorCode::DISTRIBUTOR_EXIST — 店铺号/名称/手机号重复 */
	public static final String DISTRIBUTOR_EXIST = "E5402";
	/** 对齐 PHP ErrorCode::DISTRIBUTOR_ITEM_ERROR */
	public static final String DISTRIBUTOR_ITEM_ERROR = "E5410";
	/** 对齐 PHP ErrorCode::SERVICE_ERROR */
	public static final String SERVICE_ERROR = "E5000";
	/** 对齐 PHP ErrorCode::MEMBER_ERROR — @message("会员错误") */
	public static final String MEMBER_ERROR = "E5500";
	/** 对齐 PHP ErrorCode::MEMBER_EXIST — 本接口文案常为「会员手机号已存在」 */
	public static final String MEMBER_EXIST = "E5502";
	/** 对齐 PHP ErrorCode::MEMBER_INVITER_NOT_FOUND — @message("会员的推荐人找不到") */
	public static final String MEMBER_INVITER_NOT_FOUND = "E5511";
	/** 对齐 PHP ErrorCode::MEMBER_CARD_EXIST — @message("会员卡号已存在") */
	public static final String MEMBER_CARD_EXIST = "E5522";
	/** 对齐 PHP ErrorCode::MEMBER_GRADE_ERROR — @message("会员等级错误")；OpenAPI create 上限文案为「最多只能创建5个会员等级」 */
	public static final String MEMBER_GRADE_ERROR = "E5530";
	/** 对齐 PHP ErrorCode::MEMBER_GRADE_NOT_FOUND — @message("会员等级找不到") */
	public static final String MEMBER_GRADE_NOT_FOUND = "E5531";
	/** 对齐 PHP ErrorCode::MEMBER_GRADE_DELETE_ERROR — @message("会员等级删除失败")；OpenAPI delete 自定义文案含错字「扔」 */
	public static final String MEMBER_GRADE_DELETE_ERROR = "E5533";
	/** 对齐 PHP ErrorCode::MEMBER_VIP_GRADE_ERROR — @message("会员付费等级错误")；OpenAPI create 上限文案为「最多只能创建2个会员付费等级」 */
	public static final String MEMBER_VIP_GRADE_ERROR = "E5540";
	/** 对齐 PHP ErrorCode::MEMBER_VIP_GRADE_NOT_FOUND — @message("会员付费等级找不到") */
	public static final String MEMBER_VIP_GRADE_NOT_FOUND = "E5541";
	/** 对齐 PHP ErrorCode::MEMBER_VIP_GRADE_EXIST — @message("会员付费等级已存在")；OpenAPI create 文案为「该类型下会员付费等级已存在」 */
	public static final String MEMBER_VIP_GRADE_EXIST = "E5542";
	/** 对齐 PHP ErrorCode::MEMBER_VIP_GRADE_DELETE_ERROR — @message("会员付费等级删除错误")；OpenAPI delete 关联会员自定义文案含错字「扔」 */
	public static final String MEMBER_VIP_GRADE_DELETE_ERROR = "E5543";
	/** 对齐 PHP ErrorCode::SALESPERSON_NOT_FOUND — @message("导购找不到") */
	public static final String SALESPERSON_NOT_FOUND = "E5601";
	/** 对齐 PHP ErrorCode::SALESPERSON_RELATION_MEMBER_EXIST — @message("该会员已与导购绑定") */
	public static final String SALESPERSON_RELATION_MEMBER_EXIST = "E5612";
	/** 对齐 PHP ErrorCode::MEMBER_POINT_ERROR — @message("积分异常") */
	public static final String MEMBER_POINT_ERROR = "E5550";

	private OpenapiErrorCode() {}
}
