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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 卡券包 */
@Data
@MpTable(value = "card_package", comment = "卡券包")
public class CardPackage {

    /** 卡券包ID */
    @MpId(value = "package_id", type = IdType.AUTO, columnType = "bigint", comment = "卡券包ID")
    private Long packageId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 标题 */
    @MpField(value = "title", columnType = "string", length = 10, comment = "标题")
    private String title = "";

    /** 描述 */
    @MpField(value = "package_describe", columnType = "string", length = 20, comment = "描述")
    private String packageDescribe = "";

    /** 卡券包限领次数 */
    @MpField(value = "limit_count", columnType = "integer", comment = "卡券包限领次数")
    private Integer limitCount;

    /** 被领取数量 */
    @MpField(value = "get_num", columnType = "integer", comment = "被领取数量", defaultValue = "0")
    private Integer getNum = 0;

    /** 行数据是否有效 */
    @MpField(value = "row_status", columnType = "integer", comment = "行数据是否有效", defaultValue = "1")
    private Integer rowStatus = 1;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
