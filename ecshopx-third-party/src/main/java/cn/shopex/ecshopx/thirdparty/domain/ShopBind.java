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

package cn.shopex.ecshopx.thirdparty.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 矩阵节点绑定 */
@Data
@MpTable(value = "thirdparty_shop_bind", comment = "矩阵节点绑定", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_node_type", columns = {"node_type"}), @MpIndex(name = "idx_status", columns = {"status"}), @MpIndex(name = "idx_node", columns = {"company_id", "node_type", "status"})})
public class ShopBind {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 名称 */
    @MpField(value = "name", columnType = "string", comment = "名称")
    private String name;

    /** 节点 */
    @MpField(value = "node_id", columnType = "string", comment = "节点")
    private String nodeId;

    /** 节点类型 */
    @MpField(value = "node_type", columnType = "string", comment = "节点类型")
    private String nodeType;

    /** 绑定状态 1:已绑定 0：未绑定 */
    @MpField(value = "status", columnType = "smallint", comment = "绑定状态 1:已绑定 0：未绑定")
    private Integer status;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
