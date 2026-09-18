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

package cn.shopex.ecshopx.workwechat.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 企业微信可信域名校验文件 */
@Data
@MpTable(value = "work_wechat_verify_domain_file", comment = "企业微信可信域名校验文件", uniqueIndexes = {@MpIndex(name = "ix_name", columns = {"name"})})
public class WorkWechatVerifyDomainFile {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 授权操作者id */
    @MpField(value = "operator_id", columnType = "bigint", comment = "授权操作者id")
    private Long operatorId;

    /** 验证文件名 */
    @MpField(value = "name", columnType = "string", comment = "验证文件名")
    private String name = "";

    /** 验证文件内容 */
    @MpField(value = "contents", columnType = "text", comment = "验证文件内容")
    private String contents = "";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
