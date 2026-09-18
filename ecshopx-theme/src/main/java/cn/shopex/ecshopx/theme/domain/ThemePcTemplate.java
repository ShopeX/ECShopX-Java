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

package cn.shopex.ecshopx.theme.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/** pc页面装修 */
@Data
@MpTable(
		value = "theme_pc_template",
		comment = "pc页面装修",
		indexes = {
			@MpIndex(name = "idx_company_id", columns = {"company_id"}),
			@MpIndex(name = "idx_company_distributor", columns = {"company_id", "distributor_id"})
		})
public class ThemePcTemplate {

    @MpId(value = "theme_pc_template_id", type = IdType.AUTO, columnType = "bigint")
    private Long themePcTemplateId;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 店铺ID，为0时表示商城总部 */
    @MpField(value = "distributor_id", columnType = "integer", comment = "店铺ID", defaultValue = "0")
    private Integer distributorId = 0;

    /** 页面名称 */
    @MpField(value = "template_title", columnType = "string", length = 50, comment = "页面名称")
    private String templateTitle;

    /** 页面描述 */
    @MpField(value = "template_description", columnType = "string", length = 150, comment = "页面描述")
    private String templateDescription;

    /** 页面类型 index 首页 custom 自定义 product_list 商品列表，默认 index */
    @MpField(value = "page_type", columnType = "string", length = 15, nullable = true, comment = "页面类型 index 首页 custom product_list", defaultValue = "index")
    private String pageType = "index";

    /** 启用状态 1启用 2未启用，默认 2 */
    @MpField(value = "status", columnType = "integer", nullable = true, comment = "启用状态 1启用 2未启用", defaultValue = "2")
    private Integer status = 2;

    /** 版本号 */
    @MpField(value = "version", columnType = "string", length = 10, nullable = true, comment = "版本号")
    private String version;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    @MpField(value = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}
