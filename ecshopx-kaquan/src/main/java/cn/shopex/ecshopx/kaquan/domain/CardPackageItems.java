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

package cn.shopex.ecshopx.kaquan.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 卡券包关联表 */
@Data
@MpTable(value = "card_package_items", comment = "卡券包关联表", indexes = {@MpIndex(name = "idx_companyid", columns = {"company_id"}), @MpIndex(name = "idx_packageid", columns = {"package_id"})})
public class CardPackageItems {

    /** 主键ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "主键ID")
    private Long id;

    /** 卡券包ID */
    @MpField(value = "package_id", columnType = "bigint", comment = "卡券包ID")
    private Long packageId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 优惠券卡ID */
    @MpField(value = "card_id", columnType = "bigint", comment = "优惠券卡ID")
    private Long cardId;

    /** 发送数量 */
    @MpField(value = "give_num", columnType = "bigint", comment = "发送数量")
    private Long giveNum;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
