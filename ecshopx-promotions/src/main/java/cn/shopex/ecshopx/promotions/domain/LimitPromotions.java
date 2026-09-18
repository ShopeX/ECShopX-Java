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

/** 限购活动规则表 */
@Data
@MpTable(value = "promotions_limit", comment = "限购活动规则表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class LimitPromotions {

    /** 限购活动id */
    @MpId(value = "limit_id", type = IdType.AUTO, columnType = "bigint", comment = "限购活动id")
    private Long limitId;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
    private Long companyId;

    /** 限购活动名称 */
    @MpField(value = "limit_name", columnType = "string", length = 50, comment = "限购活动名称")
    private String limitName;

    /** 限购类型, 全局限购：global, 店铺限购：shop */
    @MpField(value = "limit_type", columnType = "string", length = 50, nullable = true, comment = "限购类型, 全局限购：global, 店铺限购：shop")
    private String limitType;

    /** 会员级别集合 */
    @MpField(value = "valid_grade", columnType = "string", comment = "会员级别集合")
    private String validGrade;

    /** 限购规则 */
    @MpField(value = "rule", columnType = "string", length = 255, comment = "限购规则")
    private String rule;

    /** 起始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "起始时间")
    private Integer startTime;

    /** 截止时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "截止时间")
    private Integer endTime;

    /**
     * 适用范围: 1:指定商品可用,2:指定分类可用,3:指定商品标签可用,4:指定商品品牌可用
     */
    @MpField(value = "use_bound", columnType = "integer", comment = "适用范围: 1:指定商品可用,2:指定分类可用,3:指定商品标签可用,4:指定商品品牌可用", defaultValue = "1")
    private Integer useBound = 1;

    /** 标签id集合 */
    @MpField(value = "tag_ids", columnType = "text", nullable = true, comment = "标签id集合")
    private String tagIds;

    /** 品牌id集合 */
    @MpField(value = "brand_ids", columnType = "text", nullable = true, comment = "品牌id集合")
    private String brandIds;

    /** 限购商品总数 */
    @MpField(value = "total_item_num", columnType = "integer", comment = "限购商品总数", defaultValue = "0")
    private Integer totalItemNum = 0;

    /** 已导入的限购商品数 */
    @MpField(value = "valid_item_num", columnType = "integer", comment = "已导入的限购商品数", defaultValue = "0")
    private Integer validItemNum = 0;

    /** 导入错误描述 */
    @MpField(value = "error_desc", columnType = "text", nullable = true, comment = "导入错误描述")
    private String errorDesc;

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
