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
 * 银联商务支付sftp上传明细
 */
@Data
@MpTable(value = "chinaumspay_division_upload_detail", comment = "银联商务支付sftp上传明细", indexes = {@MpIndex(name = "idx_company", columns = {"company_id"}), @MpIndex(name = "idx_division_id", columns = {"division_id"}), @MpIndex(name = "idx_distributor_id", columns = {"distributor_id"})})
public class ChinaumspayDivisionUploadDetail {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 分账流水ID */
    @MpField(value = "division_id", columnType = "bigint", comment = "分账流水ID")
    private Long divisionId;

    /** 店铺ID */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "店铺ID")
    private Long distributorId;

    /** 文件类型 division:分账;transfer:划付; */
    @MpField(value = "file_type", columnType = "string", length = 50, comment = "文件类型 division:分账;transfer:划付;")
    private String fileType;

    /** 分账明细 */
    @MpField(value = "detail", columnType = "string", comment = "分账明细")
    private String detail;

    /** 上传次数 */
    @MpField(value = "times", columnType = "integer", comment = "上传次数", defaultValue = "0")
    private Integer times = 0;

    /** 回盘成功金额，以分为单位 */
    @MpField(value = "backsucc_fee", columnType = "integer", comment = "回盘成功金额，以分为单位", defaultValue = "0")
    private Integer backsuccFee = 0;

    /** 银联商务该笔指令收取的业务处理费，以分为单位 */
    @MpField(value = "rate_fee", columnType = "integer", comment = "银联商务该笔指令收取的业务处理费，以分为单位", defaultValue = "0")
    private Integer rateFee = 0;

    /** 回盘状态 0:未处理、1:处理中、2:成功、3:部分成功、4:失败 */
    @MpField(value = "back_status", columnType = "string", nullable = true, comment = "回盘状态 0:未处理、1:处理中、2:成功、3:部分成功、4:失败")
    private String backStatus;

    /** 回盘状态描述 */
    @MpField(value = "back_status_msg", columnType = "string", nullable = true, comment = "回盘状态描述")
    private String backStatusMsg;

    /** 银商内部ID */
    @MpField(value = "chinaumspay_id", columnType = "string", nullable = true, comment = "银商内部ID")
    private String chinaumspayId;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;
}
