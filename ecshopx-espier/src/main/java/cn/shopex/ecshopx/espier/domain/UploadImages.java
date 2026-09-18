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
 * 图片上传表
 *
 * <p>存储引擎可选值：image、videos
 *
 * <p>图片类型可选值：item:商品；aftersales:售后
 */
@Data
@MpTable(value = "espier_uploadimages", comment = "图片上传表")
public class UploadImages {

    /** 图片id */
    @MpId(value = "image_id", type = IdType.AUTO, columnType = "bigint", comment = "图片id")
    private Long imageId;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 存储引擎，可选值有，image/videos */
    @MpField(value = "storage", columnType = "string", length = 50, comment = "存储引擎，可选值有，image/videos", defaultValue = "image")
    private String storage = "image";

    /** 图片名称 */
    @MpField(value = "image_name", columnType = "string", comment = "图片名称")
    private String imageName;

    /** 图片简介 */
    @MpField(value = "brief", columnType = "string", nullable = true, comment = "图片简介")
    private String brief = "";

    /** 图片分类id */
    @MpField(value = "image_cat_id", columnType = "bigint", comment = "图片分类id", defaultValue = "0")
    private Long imageCatId = 0L;

    /** 图片类型,可选值有 item:商品;aftersales:售后 */
    @MpField(value = "image_type", columnType = "string", length = 50, nullable = true, comment = "图片类型,可选值有 item:商品;aftersales:售后", defaultValue = "item")
    private String imageType = "item";

    /** 图片完成地址 */
    @MpField(value = "image_full_url", columnType = "string", nullable = true, comment = "图片完成地址")
    private String imageFullUrl;

    /** 图片标识, 不包含域名 */
    @MpField(value = "image_url", columnType = "string", comment = "图片标识, 不包含域名")
    private String imageUrl;

    /** 图片失效 */
    @MpField(value = "disabled", columnType = "boolean", comment = "图片失效", defaultValue = "0")
    private Boolean disabled = false;

    /** 店铺id */
    @MpField(value = "distributor_id", columnType = "bigint", nullable = true, comment = "店铺id", defaultValue = "0")
    private Long distributorId = 0L;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 供应商id */
    @MpField(value = "supplier_id", columnType = "bigint", comment = "供应商id", defaultValue = "0")
    private Long supplierId = 0L;

    /** 商户id */
    @MpField(value = "merchant_id", columnType = "bigint", comment = "商户id", defaultValue = "0")
    private Long merchantId = 0L;
}
