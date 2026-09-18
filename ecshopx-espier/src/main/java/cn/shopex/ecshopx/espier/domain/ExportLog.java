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

package cn.shopex.ecshopx.espier.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 导出日志表
 *
 * <p>导出类型：member:会员导出,order:订单导出,right:权益导出
 *
 * <p>处理文件状态：wait:等待处理,finish:处理完成,processing:处理中,fail:失败
 */
@Data
@MpTable(value = "espier_export_log", comment = "导出日志表")
public class ExportLog {

    /** 导出日志id */
    @MpId(value = "log_id", type = IdType.AUTO, columnType = "bigint", comment = "导出日志id")
    private Long logId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 导出文件名称 */
    @MpField(value = "file_name", columnType = "string", nullable = true, comment = "导出文件名称")
    private String fileName;

    /** 导出文件下载路径 */
    @MpField(value = "file_url", columnType = "text", nullable = true, comment = "导出文件下载路径")
    private String fileUrl;

    /** 导出类型  member:会员导出,order:订单导出,right:权益导出 */
    @MpField(value = "export_type", columnType = "string", comment = "导出类型  member:会员导出,order:订单导出,right:权益导出")
    private String exportType;

    /** 处理文件状态，可选值有，wait:等待处理,finish:处理完成,processing:处理中,fail:失败 */
    @MpField(value = "handle_status", columnType = "string", comment = "处理文件状态，可选值有，wait:等待处理,finish:处理完成,processing:处理中,fail:失败")
    private String handleStatus = "wait";

    /** 失败原因 */
    @MpField(value = "error_msg", columnType = "text", nullable = true, comment = "失败原因")
    private String errorMsg;

    /** 处理完成时间 */
    @MpField(value = "finish_time", columnType = "bigint", nullable = true, comment = "处理完成时间")
    private Long finishTime;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 账号id */
    @MpField(value = "operator_id", columnType = "bigint", comment = "账号id", defaultValue = "0")
    private Long operatorId = 0L;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商id", defaultValue = "0")
    private Long supplierId = 0L;
}
