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
 * 数据敏感信息查看
 */
@Data
@MpTable(value = "operator_data_pass_log", comment = "数据敏感信息查看", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_operator_id", columns = {"operator_id"})})
public class OperatorDataPassLog {

    /** id */
    @MpId(value = "log_id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long logId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id", defaultValue = "0")
    private Long companyId = 0L;

    /** 操作者id */
    @MpField(value = "operator_id", columnType = "integer", comment = "操作者id", defaultValue = "0")
    private Integer operatorId = 0;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间", defaultValue = "0")
    private Integer createTime = 0;

    /** 路由地址 */
    @MpField(value = "path", columnType = "string", comment = "路由地址")
    private String path = "";

    /** 全地址 */
    @MpField(value = "url", columnType = "string", length = 1000, comment = "全地址")
    private String url = "";
}
