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

package cn.shopex.ecshopx.kujiale.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** KujialeDesignerWorksLevel */
@Data
@MpTable(value = "kujiale_designer_works_level", indexes = {@MpIndex(name = "idx_design_id", columns = {"design_id"}), @MpIndex(name = "idx_plan_id", columns = {"plan_id"})})
public class KujialeDesignerWorksLevel {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 方案id */
    @MpField(value = "design_id", columnType = "string", length = 255, comment = "方案id")
    private String designId;

    /** 户型ID */
    @MpField(value = "plan_id", columnType = "string", length = 255, nullable = true, comment = "户型ID")
    private String planId;

    /** 户型的房型 */
    @MpField(value = "spec_name", columnType = "string", length = 255, nullable = true, comment = "户型的房型")
    private String specName;

    /** 户型的建筑面积 */
    @MpField(value = "src_area", columnType = "string", length = 255, nullable = true, comment = "户型的建筑面积")
    private String srcArea;

    /** 户型的套内建筑面积 */
    @MpField(value = "area", columnType = "string", length = 255, nullable = true, comment = "户型的套内建筑面积")
    private String area;

    /** 户型的套内面积 */
    @MpField(value = "real_area", columnType = "string", length = 255, nullable = true, comment = "户型的套内面积")
    private String realArea;

    /** 户型图的URL */
    @MpField(value = "plan_pic", columnType = "text", length = 65535, nullable = true, comment = "户型图的URL")
    private String planPic;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
