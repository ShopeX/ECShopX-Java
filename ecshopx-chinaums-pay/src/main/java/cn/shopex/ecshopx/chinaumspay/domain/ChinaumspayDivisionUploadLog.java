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
 * 银联商务支付上传文件日志
 */
@Data
@MpTable(value = "chinaumspay_division_upload_log", comment = "银联商务支付上传文件日志", indexes = {@MpIndex(name = "idx_company", columns = {"company_id"})})
public class ChinaumspayDivisionUploadLog {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 文件类型 division:分账;transfer:划付; */
    @MpField(value = "file_type", columnType = "string", length = 50, comment = "文件类型 division:分账;transfer:划付;")
    private String fileType;

    /** 本地文件路径 */
    @MpField(value = "local_file_path", columnType = "string", length = 50, comment = "本地文件路径")
    private String localFilePath;

    /** 远程文件路径 */
    @MpField(value = "remote_file_path", columnType = "string", length = 50, comment = "远程文件路径")
    private String remoteFilePath;

    /** 文件名 */
    @MpField(value = "file_name", columnType = "string", length = 50, comment = "文件名")
    private String fileName;

    /** 文件内容 */
    @MpField(value = "file_content", columnType = "text", comment = "文件内容")
    private String fileContent;

    /** 回盘状态 0:未回盘;1:已回盘; */
    @MpField(value = "back_status", columnType = "string", nullable = true, comment = "回盘状态 0:未回盘;1:已回盘;")
    private String backStatus;

    /** 订单创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "订单创建时间")
    private Integer createTime;

    /** 订单更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "订单更新时间")
    private Integer updateTime;
}
