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

/** 秒杀关联分类表 */
@Data
@MpTable(value = "promotions_seckill_rel_category", comment = "秒杀关联分类表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class SeckillRelCategory {

    /** 关联id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "关联id")
    private Long id;

    /** 秒杀活动id */
    @MpField(value = "seckill_id", columnType = "bigint", comment = "秒杀活动id")
    private Long seckillId;

    /** 分类id */
    @MpField(value = "category_id", columnType = "bigint", length = 64, comment = "分类id")
    private Long categoryId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /** 分类等级 */
    @MpField(value = "category_level", columnType = "integer", comment = "分类等级")
    private Integer categoryLevel = 0;
}
