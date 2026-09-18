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
 * adapay操作日志表
 */
@Data
@MpTable(value = "adapay_operation_log", comment = "adapay操作日志表", indexes = {@MpIndex(name = "idx_id", columns = {"company_id", "rel_id"}), @MpIndex(name = "idx_log_type", columns = {"log_type"})})
public class AdapayOperationLog {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 日志实际操作者ID */
    @MpField(value = "operator_id", columnType = "bigint", comment = "日志实际操作者ID", defaultValue = "0")
    private Long operatorId = 0L;

    /** 关联ID,店铺类型为分店ID，经销商为主经销商账号operator_id */
    @MpField(value = "rel_id", columnType = "bigint", comment = "关联ID,店铺类型为分店ID，经销商为主经销商账号operator_id", defaultValue = "0")
    private Long relId = 0L;

    /** merchant-主商户;distributor-店铺;dealer-经销 */
    @MpField(value = "log_type", columnType = "string", length = 50, comment = "merchant-主商户;distributor-店铺;dealer-经销")
    private String logType;

    /** 日志内容 */
    @MpField(value = "content", columnType = "string", length = 255, comment = "日志内容")
    private String content = "";

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
