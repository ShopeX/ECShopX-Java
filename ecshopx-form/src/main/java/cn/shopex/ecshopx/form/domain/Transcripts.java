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
 * 成绩单配置表
 */
@Data
@MpTable(value = "transcripts", comment = "成绩单配置表")
public class Transcripts {

    /** 成绩单模板id */
    @MpId(value = "transcript_id", type = IdType.AUTO, columnType = "bigint", comment = "成绩单模板id")
    private Long transcriptId;

    /** 门店名称 */
    @MpField(value = "transcript_name", columnType = "string", length = 100, nullable = true, comment = "门店名称")
    private String transcriptName;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 小程序模板name */
    @MpField(value = "template_name", columnType = "string", comment = "小程序模板name")
    private String templateName;

    /** 状态；启用：on；禁用：off */
    @MpField(value = "transcript_status", columnType = "string", comment = "状态", defaultValue = "False")
    private String transcriptStatus;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
