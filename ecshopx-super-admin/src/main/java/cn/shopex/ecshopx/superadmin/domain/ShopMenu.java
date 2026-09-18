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
 * 商家和平台端菜单管理
 *
 * <p>索引：ix_company_id（company_id）
 */
@Data
@MpTable(value = "shop_menu", comment = "商家和平台端菜单管理", indexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"})})
public class ShopMenu {
    /** 菜单id */
    @MpId(value = "shopmenu_id", type = IdType.INPUT, columnType = "bigint", comment = "菜单id")
    private Long shopmenuId;

    /** 公司id，company_id 为 0 的时候表示通用 */
    @MpField(value = "company_id", columnType = "integer", comment = "公司id，company_id为0的时候表示通用")
    private Integer companyId;

    /** 菜单别名,唯一值，可为空 */
    @MpField(value = "alias_name", columnType = "string", nullable = true, comment = "菜单别名,唯一值")
    private String aliasName;

    /** 菜单名称 */
    @MpField(value = "name", columnType = "string", comment = "菜单名称")
    private String name;

    /** 菜单对应路由 */
    @MpField(value = "url", columnType = "string", comment = "菜单对应路由")
    private String url;

    /** 排序，可为空 */
    @MpField(value = "sort", columnType = "integer", nullable = true, comment = "排序")
    private Integer sort;

    /** 是否为菜单，可为空 */
    @MpField(value = "is_menu", columnType = "boolean", nullable = true, comment = "是否为菜单")
    private Boolean isMenu;

    /** 上级菜单id，默认 0 */
    @MpField(value = "pid", columnType = "bigint", comment = "上级菜单id", defaultValue = "0")
    private Long pid = 0L;

    /** API权限集，可为空 */
    @MpField(value = "apis", columnType = "text", nullable = true, comment = "API权限集")
    private String apis;

    /** 菜单图标，可为空 */
    @MpField(value = "icon", columnType = "string", nullable = true, comment = "菜单图标")
    private String icon;

    /** 是否显示 */
    @MpField(value = "is_show", columnType = "boolean", comment = "是否显示")
    private Boolean isShow;

    /**
     * 菜单版本，默认 1。1:平台菜单;2:IT端菜单,3:店铺菜单,4:供应商菜单,5:经销商菜单,6:商户菜单
     */
    @MpField(value = "version", columnType = "smallint", comment = "菜单版本,1:平台菜单;2:IT端菜单,3:店铺菜单,4:供应商菜单,5:经销商菜单,6:商户菜单", defaultValue = "1")
    private Integer version = 1;

    /** 是否有效 */
    @MpField(value = "disabled", columnType = "boolean", comment = "是否有效")
    private Boolean disabled;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
