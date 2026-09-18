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

package cn.shopex.ecshopx.orders.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 额外积分活动（订单模块只读，表与 promotions 域共用） */
@Data
@MpTable(value = "promotions_extrapoint_activity", comment = "积分营销活动表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class OrderPromotionsExtraPointActivity {

	@MpId(value = "activity_id", type = IdType.AUTO, columnType = "bigint", comment = "活动ID")
	private Long activityId;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "type", columnType = "string", comment = "营销类型: shop:店铺额外积分,birthday:会员生日,item:商品额外积分", defaultValue = "shop")
	private String type = "shop";

	@MpField(value = "title", columnType = "string", comment = "活动名称")
	private String title;

	@MpField(value = "trigger_condition", columnType = "text", comment = "触发条件")
	private String triggerCondition;

	@MpField(value = "condition_value", columnType = "integer", comment = "优惠配置")
	private Integer conditionValue;

	@MpField(value = "condition_type", columnType = "string", comment = "优惠方式: multiple:倍数, plus:增加")
	private String conditionType;

	@MpField(value = "valid_grade", columnType = "text", nullable = true, comment = "会员级别集合")
	private String validGrade;

	@MpField(value = "use_shop", columnType = "string", comment = "适用店铺: 0:全场可用,1:指定店铺可用")
	private String useShop;

	@MpField(
			value = "shop_ids",
			insertStrategy = FieldStrategy.ALWAYS,
			updateStrategy = FieldStrategy.ALWAYS)
	private String shopIds;

	@MpField(value = "activity_status", columnType = "string", comment = "活动状态")
	private String activityStatus;

	@MpField(value = "begin_time", columnType = "bigint", comment = "活动开始时间")
	private Long beginTime;

	@MpField(value = "end_time", columnType = "bigint", nullable = true, comment = "活动结束时间")
	private Long endTime;

	@MpField(value = "created", columnType = "integer")
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
