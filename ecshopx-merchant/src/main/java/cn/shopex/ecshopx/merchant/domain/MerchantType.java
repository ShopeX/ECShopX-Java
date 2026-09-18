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

package cn.shopex.ecshopx.merchant.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 商户类型表 */
@Data
@MpTable(value = "merchant_type", comment = "商户类型表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_name", columns = {"name"}), @MpIndex(name = "idx_parent_id", columns = {"parent_id"})})
public class MerchantType {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", nullable = true, comment = "公司id")
    private Long companyId;

    /** 商户类型名称 */
    @MpField(value = "name", columnType = "string", length = 50, comment = "商户类型名称")
    private String name;

    /** 父级id, 0为顶级 */
    @MpField(value = "parent_id", columnType = "bigint", comment = "父级id, 0为顶级", defaultValue = "0")
    private long parentId = 0L;

    /** 路径 */
    @MpField(value = "path", columnType = "string", length = 255, nullable = true, comment = "路径", defaultValue = "0")
    private String path = "0";

    /** 排序 */
    @MpField(value = "sort", columnType = "bigint", nullable = true, comment = "排序", defaultValue = "0")
    private Long sort = 0L;

    /** 是否显示 */
    @MpField(value = "is_show", columnType = "boolean", comment = "是否显示", defaultValue = "False")
    private boolean isShow = false;

    /**
     * JavaBean-style getter for MyBatis-Plus {@code LambdaQueryWrapper} / {@code LambdaUpdateWrapper}.
     * Lombok exposes {@code isShow()} only; MP resolves that to property {@code show} and breaks column mapping.
     */
    public boolean getIsShow() {
        return isShow;
    }

    /** 等级 */
    @MpField(value = "level", columnType = "integer", nullable = true, comment = "等级", defaultValue = "1")
    private Integer level = 1;

    @MpField(value = "created", columnType = "integer")
    private int created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;
}
