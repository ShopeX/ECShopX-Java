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

package cn.shopex.ecshopx.datacube.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 小程序页面与来源关联表
 */
@Data
@MpTable(value = "datacube_statistics", comment = "小程序页面与来源关联表", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"monitor_id", "source_id"})})
public class Statistics {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 监控id（联合主键之一） */
    @MpField(value = "monitor_id", columnType = "bigint", comment = "监控id")
    private Long monitorId;

    /** 来源id（联合主键之一） */
    @MpField(value = "source_id", columnType = "bigint", comment = "来源id")
    private Long sourceId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 浏览人数，默认 0 */
    @MpField(value = "view_num", columnType = "bigint", comment = "浏览人数", defaultValue = "0")
    private Long viewNum = 0L;

    /** 参与人数，默认 0 */
    @MpField(value = "entries_num", columnType = "bigint", comment = "参与人数", defaultValue = "0")
    private Long entriesNum = 0L;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
