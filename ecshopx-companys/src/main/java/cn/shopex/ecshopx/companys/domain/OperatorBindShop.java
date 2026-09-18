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

import java.time.LocalDateTime;

/**
 * 员工关联门店表
 */
@Data
@MpTable(value = "operator_bind_shop", comment = "员工关联门店表", indexes = {@MpIndex(name = "idx_shop_id", columns = {"shop_id"})}, uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"operator_id", "shop_id"})})
public class OperatorBindShop {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    @MpField(value = "operator_id", columnType = "bigint")
    private Long operatorId;

    @MpField(value = "shop_id", columnType = "bigint")
    private Long shopId;

    @MpField(value = "bind_status", columnType = "boolean")
    private Boolean bindStatus;

    @MpField(value = "is_shopadmin", columnType = "boolean")
    private Boolean isShopadmin;

    @MpField("created_at")
    private LocalDateTime createdAt;

    @MpField("updated_at")
    private LocalDateTime updatedAt;

    @MpField("deleted_at")
    private LocalDateTime deletedAt;
}
