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

package cn.shopex.ecshopx.salesperson.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 导购通知 */
@Data
@MpTable(value = "salesperson_notice_log", comment = "导购通知", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_distributor_id", columns = {"distributor_id"}), @MpIndex(name = "ix_notice_id", columns = {"notice_id"})})
public class SalespersonNoticeLog {

    /** 主键id */
    @MpId(value = "log_id", type = IdType.AUTO, columnType = "bigint", comment = "主键id")
    private Long logId;

    /** 通知id */
    @MpField(value = "notice_id", columnType = "bigint", comment = "通知id")
    private Long noticeId;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺id")
    private Integer distributorId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
