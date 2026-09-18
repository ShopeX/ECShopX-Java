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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 资源操作记录表
 */
@Data
@MpTable(value = "resources_op_log", comment = "资源操作记录表")
public class ResourcesOpLog {

    /** 记录id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "记录id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 资源包id */
    @MpField(value = "resource_id", columnType = "bigint", comment = "资源包id")
    private Long resourceId;

    /** 门店id */
    @MpField(value = "shop_id", columnType = "bigint", comment = "门店id", defaultValue = "0")
    private Long shopId = 0L;

    /** 门店名称 */
    @MpField(value = "store_name", columnType = "string", length = 100, comment = "门店名称")
    private String storeName;

    /** 操作时间 */
    @MpField(value = "op_time", columnType = "integer", length = 100, comment = "操作时间")
    private Integer opTime;

    /** 操作类型。occupy:占用资源, release:释放资源 */
    @MpField(value = "op_type", columnType = "string", length = 100, comment = "操作类型。occupy:占用资源, release:释放资源")
    private String opType;

    /** 操作数量 */
    @MpField(value = "op_num", columnType = "integer", length = 100, comment = "操作数量")
    private Integer opNum;

    /** 操作员id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "操作员id")
    private Long operatorId;
}
