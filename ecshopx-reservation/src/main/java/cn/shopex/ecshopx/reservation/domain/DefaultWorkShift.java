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

package cn.shopex.ecshopx.reservation.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 商户配置默认排班 */
@Data
@MpTable(value = "reservation_default_work_shift", comment = "商户配置默认排班", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"company_id", "shop_id"})})
public class DefaultWorkShift {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 公司门店 id */
    @MpField(value = "shop_id", columnType = "bigint", comment = "公司门店 id")
    private Long shopId;

    /**
     * 周一至周日每天的排班；持久化格式与 {@link cn.shopex.ecshopx.reservation.util.WorkShiftDataCodec} 一致。
     */
    @MpField(value = "work_shift_data", columnType = "array", comment = "周一至周日每天的排班")
    private String workShiftData;
}
