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

package cn.shopex.ecshopx.superadmin.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 菜单类型关联
 *
 * <p>索引：ix_shopmenu_id（shopmenu_id）
 */
@Data
@MpTable(value = "shop_menu_rel_type", comment = "菜单类型关联", indexes = {@MpIndex(name = "ix_shopmenu_id", columns = {"shopmenu_id"})})
public class ShopMenuRelType {

    /** 关联ID */
    @MpId(value = "rel_id", type = IdType.AUTO, columnType = "bigint", comment = "关联ID")
    private Long relId;

    /** 菜单id */
    @MpField(value = "shopmenu_id", columnType = "bigint", comment = "菜单id")
    private Long shopmenuId;

    /** 菜单类型，默认 1 */
    @MpField(value = "menu_type", columnType = "integer", comment = "菜单类型", defaultValue = "1")
    private Integer menuType = 1;

    /** 公司ID */
    @MpField(value = "company_id", columnType = "integer", comment = "公司ID")
    private Integer companyId;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间，可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
