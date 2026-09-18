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

package cn.shopex.ecshopx.wechat.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/** 小程序模板表 */
@Data
@MpTable(value = "wechat_weapp_template", comment = "小程序模板表")
public class WeappTemplate {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 小程序模板名称 */
    @MpField(value = "template_name", columnType = "string", length = 50, comment = "小程序模板名称")
    private String templateName;

    /**
     * succ 成功
     *
     * 小程序模板开通状态
     */
    @MpField(value = "template_open_status", columnType = "string", length = 10, comment = "小程序模板开通状态")
    private String templateOpenStatus;

    /**
     * succ 成功
     *
     * 小程序模板开通金额，默认免费
     */
    @MpField(value = "template_money", columnType = "string", comment = "小程序模板开通金额，默认免费")
    private String templateMoney;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField("created_at")
    private LocalDateTime createdAt;

    @MpField(value = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @MpField(value = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}
