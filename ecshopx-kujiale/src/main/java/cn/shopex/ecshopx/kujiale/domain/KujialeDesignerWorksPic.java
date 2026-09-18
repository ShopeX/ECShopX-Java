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

/** KujialeDesignerWorksPic */
@Data
@MpTable(value = "kujiale_designer_works_pic", indexes = {@MpIndex(name = "idx_design_id", columns = {"design_id"}), @MpIndex(name = "idx_plan_id", columns = {"plan_id"})})
public class KujialeDesignerWorksPic {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 渲染图ID */
    @MpField(value = "pic_id", columnType = "string", length = 255, comment = "渲染图ID")
    private String picId;

    /** 渲染图类型。0表示普通渲染图，1表示全景图，3表示俯视图 */
    @MpField(value = "pic_type", columnType = "string", length = 255, nullable = true, comment = "渲染图类型。0表示普通渲染图，1表示全景图，3表示俯视图")
    private String picType;

    /** 渲染图类型细分 */
    @MpField(value = "pic_detail_type", columnType = "bigint", nullable = true, comment = "渲染图类型细分")
    private Long picDetailType;

    /** 渲染图所属房间的名字 */
    @MpField(value = "room_name", columnType = "string", length = 255, nullable = true, comment = "渲染图所属房间的名字")
    private String roomName;

    /** 渲染图URL */
    @MpField(value = "img", columnType = "string", length = 255, nullable = true, comment = "渲染图URL")
    private String img;

    /** 全景图的链接地址 */
    @MpField(value = "pano_link", columnType = "string", length = 255, nullable = true, comment = "全景图的链接地址")
    private String panoLink;

    /** 方案ID */
    @MpField(value = "design_id", columnType = "string", length = 255, nullable = true, comment = "方案ID")
    private String designId;

    /** 户型ID */
    @MpField(value = "plan_id", columnType = "string", length = 255, nullable = true, comment = "户型ID")
    private String planId;

    /** 渲染图所在房间的楼层信息，正为地上，负为地下室，不存在0层 */
    @MpField(value = "`level`", columnType = "string", length = 255, nullable = true, comment = "渲染图所在房间的楼层信息，正为地上，负为地下室，不存在0层")
    private String level;

    /** 创建时间 */
    @MpField(value = "created", columnType = "integer", comment = "创建时间")
    private Integer created;

    /** 更新时间 */
    @MpField(value = "updated", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updated;
}
