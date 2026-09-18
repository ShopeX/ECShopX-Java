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

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 微信经营类目
 */
@Data
@MpTable(value = "adapay_wx_business_category", comment = "微信经营类目", indexes = {@MpIndex(name = "idx_merchant_type_name", columns = {"merchant_type_name"}), @MpIndex(name = "idx_business_category_id", columns = {"business_category_id"})})
public class AdapayWxBusinessCategory {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 费率类型 */
    @MpField(value = "fee_type", columnType = "string", length = 10, comment = "费率类型")
    private String feeType;

    /** 费率类型名称 */
    @MpField(value = "fee_type_name", columnType = "string", length = 50, comment = "费率类型名称")
    private String feeTypeName;

    /** 商户种类名称 */
    @MpField(value = "merchant_type_name", columnType = "string", length = 50, comment = "商户种类名称")
    private String merchantTypeName;

    /** 微信经营类目id */
    @MpField(value = "business_category_id", columnType = "string", length = 50, comment = "微信经营类目id")
    private String businessCategoryId;
}
