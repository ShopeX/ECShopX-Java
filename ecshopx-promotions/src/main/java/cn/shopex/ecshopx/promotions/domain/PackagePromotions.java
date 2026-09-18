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

/** 组合促销促销规则表 */
@Data
@MpTable(value = "promotions_package", comment = "组合促销促销规则表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_created", columns = {"created"}), @MpIndex(name = "idx_package_status", columns = {"package_status"})})
public class PackagePromotions {

    /** 组合促销规则id */
    @MpId(value = "package_id", type = IdType.AUTO, columnType = "bigint", comment = "组合促销规则id")
    private Long packageId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 商品id */
    @MpField(value = "goods_id", columnType = "bigint", comment = "商品id")
    private Long goodsId;

    /** 主商品id */
    @MpField(value = "main_item_id", columnType = "bigint", comment = "主商品id")
    private Long mainItemId;

    /** 主商品价格 */
    @MpField(value = "main_item_price", columnType = "bigint", comment = "主商品价格")
    private Long mainItemPrice;

    /** 组合促销名称 */
    @MpField(value = "package_name", columnType = "string", length = 50, comment = "组合促销名称")
    private String packageName;

    /** 会员级别集合 */
    @MpField(value = "valid_grade", columnType = "string", comment = "会员级别集合")
    private String validGrade;

    /** 0 商家全场可用|1 只能用于pc|2 只能用于wap|3 只能用于app, 使用平台 */
    @MpField(value = "used_platform", columnType = "integer", nullable = true, comment = "0 商家全场可用|1 只能用于pc|2 只能用于wap|3 只能用于app, 使用平台", defaultValue = "0")
    private Integer usedPlatform = 0;

    /** 0 包邮|1 商品 */
    @MpField(value = "free_postage", columnType = "boolean", nullable = true, comment = "0 包邮|1 商品", defaultValue = "0")
    private Boolean freePostage = false;

    /** 组合促销各商品总价 */
    @MpField(value = "package_total_price", columnType = "integer", nullable = true, comment = "组合促销各商品总价", defaultValue = "0")
    private Integer packageTotalPrice = 0;

    /** 起始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "起始时间")
    private Integer startTime;

    /** 截止时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "截止时间")
    private Integer endTime;

    /**
     * NO_REVIEWED 未审核|PENDING 待审核|AGREE 审核通过|REFUSE 审核拒绝|CANCEL 已取消
     */
    @MpField(value = "package_status", columnType = "string", nullable = true, comment = "NO_REVIEWED 未审核|PENDING 待审核|AGREE 审核通过|REFUSE 审核拒绝|CANCEL 已取消", defaultValue = "NO_REVIEWED")
    private String packageStatus = "NO_REVIEWED";

    /** 审核不通过原因 */
    @MpField(value = "reason", columnType = "string", nullable = true, comment = "审核不通过原因")
    private String reason = "";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 添加者类型：distributor */
    @MpField(value = "source_type", columnType = "string", length = 20, nullable = true, comment = "添加者类型：distributor")
    private String sourceType;

    /** 添加者ID: 如店铺ID */
    @MpField(value = "source_id", columnType = "bigint", nullable = true, comment = "添加者ID: 如店铺ID", defaultValue = "0")
    private Long sourceId = 0L;
}
