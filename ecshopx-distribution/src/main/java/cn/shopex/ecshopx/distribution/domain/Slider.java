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

package cn.shopex.ecshopx.distribution.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@MpTable(value = "distribution_shopscreen_slider")
public class Slider {

    /** 轮播id */
    @MpId(value = "slide_id", type = IdType.AUTO, columnType = "bigint", comment = "轮播id")
    private Long slideId;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 分销商id */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "分销商id", defaultValue = "0")
    private Long distributorId = 0L;

    /** 标题 */
    @MpField(value = "title", columnType = "string", comment = "标题")
    private String title;

    /** 副标题 */
    @MpField(value = "sub_title", columnType = "string", comment = "副标题")
    private String subTitle;

    /** 风格参数 */
    @MpField(value = "style_params", columnType = "array", comment = "风格参数")
    private String styleParams;

    /** 图片描述状态11 */
    @MpField(value = "desc_status", columnType = "boolean", comment = "图片描述状态11", defaultValue = "False")
    private Boolean descStatus = false;

    /** 轮播图列表 */
    @MpField(value = "image_list", columnType = "array", comment = "轮播图列表")
    private String imageList;
}
