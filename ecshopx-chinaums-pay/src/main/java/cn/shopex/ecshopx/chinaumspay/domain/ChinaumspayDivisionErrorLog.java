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

package cn.shopex.ecshopx.chinaumspay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 银联商务支付分账错误日志
 */
@Data
@MpTable(value = "chinaumspay_division_error_log", comment = "银联商务支付分账错误日志", indexes = {@MpIndex(name = "idx_company", columns = {"company_id"}), @MpIndex(name = "idx_division_id", columns = {"division_id"}), @MpIndex(name = "idx_type", columns = {"type"})})
public class ChinaumspayDivisionErrorLog {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 分账流水ID */
    @MpField(value = "division_id", columnType = "bigint", length = 64, comment = "分账流水ID")
    private Long divisionId;

    /** 上传明细ID */
    @MpField(value = "upload_detail_id", columnType = "bigint", length = 64, comment = "上传明细ID")
    private Long uploadDetailId;

    /** 类型 division:分账;transfer:划付; */
    @MpField(value = "type", columnType = "string", length = 50, comment = "类型 division:分账;transfer:划付;")
    private String type;

    /** 店铺ID */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺ID")
    private Long distributorId;

    /** 错误状态 0:未处理、1:处理中、2:成功、3:部分成功、4:失败 */
    @MpField(value = "status", columnType = "string", length = 20, nullable = true, comment = "错误状态 0:未处理、1:处理中、2:成功、3:部分成功、4:失败")
    private String status;

    /** 错误描述 */
    @MpField(value = "error_desc", columnType = "text", nullable = true, comment = "错误描述")
    private String errorDesc;

    /**
     * 是否重新提交：0 未提交、1 已提交、2 等待执行。
     */
    @MpField(value = "is_resubmit", columnType = "boolean", nullable = true, comment = "是否重新提交", defaultValue = "False")
    private Integer isResubmit;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;
}
