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

package cn.shopex.ecshopx.adapay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 支付宝行业分类
 */
@Data
@MpTable(value = "adapay_alipay_industry_category", comment = "支付宝行业分类")
public class AdapayAlipayIndustryCategory {

    /** 分类id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "分类id")
    private Long id;

    /** 分类名称 */
    @MpField(value = "category_name", columnType = "string", length = 100, comment = "分类名称")
    private String categoryName;

    /** 父级id, 0为顶级 */
    @MpField(value = "parent_id", columnType = "bigint", comment = "父级id, 0为顶级", defaultValue = "0")
    private Long parentId = 0L;

    /** 分类等级 */
    @MpField(value = "category_level", columnType = "integer", comment = "分类等级", defaultValue = "1")
    private Integer categoryLevel = 1;

    /** 行业分类ID */
    @MpField(value = "alipay_cls_id", columnType = "bigint", nullable = true, comment = "行业分类ID")
    private Long alipayClsId;

    /** 支付宝经营类目 */
    @MpField(value = "alipay_category_id", columnType = "string", length = 20, nullable = true, comment = "支付宝经营类目")
    private String alipayCategoryId;
}
