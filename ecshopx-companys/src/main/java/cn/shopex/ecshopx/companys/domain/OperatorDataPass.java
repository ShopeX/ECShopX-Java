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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 数据敏感信息申请
 */
@Data
@MpTable(value = "operator_data_pass", comment = "数据敏感信息申请", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_operator_id", columns = {"operator_id"}), @MpIndex(name = "idx_merchant_id", columns = {"merchant_id"})})
public class OperatorDataPass {

    /** 审核id */
    @MpId(value = "pass_id", type = IdType.AUTO, columnType = "bigint", comment = "审核id")
    private Long passId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id", defaultValue = "0")
    private Long companyId = 0L;

    /** 操作者id */
    @MpField(value = "operator_id", columnType = "integer", comment = "操作者id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 审批状态 0:未审批 1:同意 2:驳回 */
    @MpField(value = "status", columnType = "integer", comment = "审批状态 0:未审批 1:同意 2:驳回", defaultValue = "0")
    private Integer status = 0;

    /** 关闭状态: 0:未关闭 1:关闭 */
    @MpField(value = "is_closed", columnType = "integer", comment = "关闭状态: 0:未关闭 1:关闭", defaultValue = "0")
    private Integer isClosed = 0;

    /** 生效开始时间 */
    @MpField(value = "start_time", columnType = "integer", comment = "生效开始时间", defaultValue = "0")
    private Integer startTime = 0;

    /** 生效结束时间 */
    @MpField(value = "end_time", columnType = "integer", comment = "生效结束时间", defaultValue = "0")
    private Integer endTime = 0;

    /** 生效规则 '8-18 *': 每天8到18点有效 '* 1-5':周一到周五全天有效 */
    @MpField(value = "rule", columnType = "string", comment = "生效规则 '8-18 *': 每天8到18点有效 '* 1-5':周一到周五全天有效")
    private String rule = "";

    /** 申请理由 */
    @MpField(value = "reason", columnType = "string", comment = "申请理由")
    private String reason = "";

    /** 审批备注 */
    @MpField(value = "remarks", columnType = "string", comment = "审批备注")
    private String remarks = "";

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间", defaultValue = "0")
    private Integer createTime = 0;

    /** 审批时间 */
    @MpField(value = "approve_time", columnType = "integer", comment = "审批时间", defaultValue = "0")
    private Integer approveTime = 0;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;
}
