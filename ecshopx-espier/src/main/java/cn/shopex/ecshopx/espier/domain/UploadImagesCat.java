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

package cn.shopex.ecshopx.espier.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 图片分类表
 *
 * <p>图片类型可选值：item:商品
 */
@Data
@MpTable(value = "espier_uploadimages_cat", comment = "图片分类表")
public class UploadImagesCat {

    /** 图片分类id */
    @MpId(value = "image_cat_id", type = IdType.AUTO, columnType = "bigint", comment = "图片分类id")
    private Long imageCatId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 图片分类名称 */
    @MpField(value = "image_cat_name", columnType = "string", comment = "图片分类名称")
    private String imageCatName;

    /** 父分类id,顶级为0 */
    @MpField(value = "parent_id", columnType = "bigint", comment = "父分类id,顶级为0", defaultValue = "0")
    private Long parentId = 0L;

    /** 图片类型,可选值有 item:商品; */
    @MpField(value = "image_type", columnType = "string", nullable = true, comment = "图片类型,可选值有 item:商品;")
    private String imageType;

    /** 路径 */
    @MpField(value = "path", columnType = "string", length = 255, comment = "路径")
    private String path;

    /** 排序 */
    @MpField(value = "sort", columnType = "bigint", comment = "排序", defaultValue = "0")
    private Long sort = 0L;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商id", defaultValue = "0")
    private Long supplierId = 0L;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;
}
