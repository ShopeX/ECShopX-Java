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
 * 上传文件日志ID
 *
 * <p>处理文件状态，可选值有，wait:等待处理
 */
@Data
@MpTable(value = "espier_uploadefile", comment = "上传文件日志ID")
public class UploadeFile {

    /** 上传文件日志ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "上传文件日志ID")
    private Long id;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 操作者id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "操作者id", defaultValue = "0")
    private Long operatorId = 0L;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商id", defaultValue = "0")
    private Long supplierId = 0L;

    /** 上传文件名称 */
    @MpField(value = "file_name", columnType = "string", comment = "上传文件名称")
    private String fileName;

    /** 上传文件类型 */
    @MpField(value = "file_type", columnType = "string", comment = "上传文件类型")
    private String fileType;

    /** 上传文件大小 */
    @MpField(value = "file_size", columnType = "string", comment = "上传文件大小")
    private String fileSize;

    /** 处理文件状态，可选值有，wait:等待处理 */
    @MpField(value = "handle_status", columnType = "string", comment = "处理文件状态，可选值有，wait:等待处理")
    private String handleStatus;

    /** 处理文件行数 */
    @MpField(value = "handle_line_num", columnType = "string", comment = "处理文件行数")
    private String handleLineNum;

    /** 处理完成时间 */
    @MpField(value = "finish_time", columnType = "bigint", nullable = true, comment = "处理完成时间")
    private Long finishTime;

    /** 处理错误文件 */
    @MpField(value = "handle_message", columnType = "text", nullable = true, comment = "处理错误文件")
    private String handleMessage;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** distributor_id */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "distributor_id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 剩余子任务数 */
    @MpField(value = "left_job_num", columnType = "integer", comment = "剩余子任务数", defaultValue = "0")
    private Integer leftJobNum = 0;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;

    /**
     * 关联 id（如内购活动 id）。迁移 {@code V20260902120000__espier_uploadefile_relation_id.sql}
     * 执行前不落库，经上传结果 Map 与队列 payload 传递；迁移完成后可改为 {@code exist = true}。
     */
    @MpField(value = "relation_id", columnType = "bigint", comment = "关联id", defaultValue = "0", exist = false)
    private Long relationId = 0L;
}
