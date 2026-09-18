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

package cn.shopex.ecshopx.form.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 用户成绩单记录表
 */
@Data
@MpTable(value = "user_transcripts", comment = "用户成绩单记录表")
public class UserTranscripts {

    /** 记录id */
    @MpId(value = "record_id", type = IdType.AUTO, columnType = "bigint", comment = "记录id")
    private Long recordId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 公司id */
    @MpField(value = "shop_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long shopId;

    /** 成绩单id */
    @MpField(value = "transcript_id", columnType = "bigint", comment = "成绩单id")
    private Long transcriptId;

    /** 成绩单名称 */
    @MpField(value = "transcript_name", columnType = "string", comment = "成绩单名称")
    private String transcriptName;

    /** 指标详情 */
    @MpField(value = "indicator_details", columnType = "json_array", comment = "指标详情")
    private String indicatorDetails;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
