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

package cn.shopex.ecshopx.promotions.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 秒杀关联商品表 */
@Data
@MpTable(value = "promotions_seckill_rel_goods", comment = "秒杀关联商品表", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_seckill_item_id", columns = {"seckill_id", "item_id"}), @MpIndex(name = "ix_item_type", columns = {"item_type"}), @MpIndex(name = "ix_release_time", columns = {"activity_release_time"}), @MpIndex(name = "ix_end_time", columns = {"activity_end_time"}), @MpIndex(name = "ix_itemid_activityreleasetime_activityendtime", columns = {"item_id", "activity_release_time", "activity_end_time"})})
public class SeckillRelGoods {

    /** 关联id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "关联id")
    private Long id;

    /** 秒杀活动id */
    @MpField(value = "seckill_id", columnType = "bigint", comment = "秒杀活动id")
    private Long seckillId;

    /** 秒杀类型 normal正常的秒杀活动， limited_time_sale限时特惠 */
    @MpField(value = "seckill_type", columnType = "string", comment = "秒杀类型 normal正常的秒杀活动， limited_time_sale限时特惠", defaultValue = "normal")
    private String seckillType = "normal";

    /** 秒杀活动商品id */
    @MpField(value = "item_id", columnType = "bigint", comment = "秒杀活动商品id")
    private Long itemId;

    /** 秒杀活动商品类型。可选值有 normal-实体类;services-服务类 */
    @MpField(value = "item_type", columnType = "string", comment = "秒杀活动商品类型。可选值有 normal-实体类;services-服务类", defaultValue = "normal")
    private String itemType = "normal";

    /** 查询列表是否显示 */
    @MpField(value = "is_show", columnType = "boolean", comment = "查询列表是否显示", defaultValue = "True")
    private Boolean isShow = true;

    /** 商品规格描述 */
    @MpField(value = "item_spec_desc", columnType = "string", nullable = true, comment = "商品规格描述")
    private String itemSpecDesc;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 秒杀开始时间 */
    @MpField(value = "activity_start_time", columnType = "integer", comment = "秒杀开始时间")
    private Integer activityStartTime;

    /** 秒杀结束时间 */
    @MpField(value = "activity_end_time", columnType = "integer", comment = "秒杀结束时间")
    private Integer activityEndTime;

    /** 秒杀活动发布时间 */
    @MpField(value = "activity_release_time", columnType = "integer", comment = "秒杀活动发布时间")
    private Integer activityReleaseTime;

    /** 秒杀活动商品名称 */
    @MpField(value = "item_title", columnType = "string", comment = "秒杀活动商品名称")
    private String itemTitle;

    /** 秒杀活动商品图片 */
    @MpField(value = "item_pic", columnType = "text", nullable = true, comment = "秒杀活动商品图片")
    private String itemPic;

    /** 秒杀活动价格 */
    @MpField(value = "activity_price", columnType = "integer", comment = "秒杀活动价格", defaultValue = "0")
    private Integer activityPrice = 0;

    /** 秒杀活动库存 */
    @MpField(value = "activity_store", columnType = "integer", comment = "秒杀活动库存", defaultValue = "0")
    private Integer activityStore = 0;

    /** 秒杀活动限购 */
    @MpField(value = "limit_num", columnType = "integer", comment = "秒杀活动限购", defaultValue = "0")
    private Integer limitNum = 0;

    /** 已购买库存 */
    @MpField(value = "sales_store", columnType = "integer", comment = "已购买库存", defaultValue = "0")
    private Integer salesStore = 0;

    /** 商品排序 */
    @MpField(value = "sort", columnType = "integer", comment = "商品排序", defaultValue = "0")
    private Integer sort = 0;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 是否失效 */
    @MpField(value = "disabled", columnType = "boolean", comment = "是否失效", defaultValue = "0")
    private Boolean disabled = false;
}
