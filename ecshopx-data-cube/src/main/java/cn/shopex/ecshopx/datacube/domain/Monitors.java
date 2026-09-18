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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 小程序页面监控
 */
@Data
@MpTable(value = "datacube_monitors", comment = "小程序页面监控")
public class Monitors {

    /** 监控id */
    @MpId(value = "monitor_id", type = IdType.AUTO, columnType = "bigint", comment = "监控id")
    private Long monitorId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 小程序 appid */
    @MpField(value = "wxappid", columnType = "string", comment = "小程序appid")
    private String wxappid;

    /** 小程序名称 */
    @MpField(value = "nick_name", columnType = "string", comment = "小程序名称")
    private String nickName;

    /** 页面描述 */
    @MpField(value = "page_name", columnType = "string", comment = "页面描述")
    private String pageName;

    /** 监控页面 */
    @MpField(value = "monitor_path", columnType = "string", comment = "监控页面")
    private String monitorPath;

    /** 监控页面的参数，可为空 */
    @MpField(value = "monitor_path_params", columnType = "string", nullable = true, comment = "监控页面的参数")
    private String monitorPathParams;

    /** 创建时间（整型时间戳） */
    @MpField(value = "created", columnType = "integer")
    private Integer created;

    /** 更新时间（整型时间戳），可为空 */
    @MpField(value = "updated", columnType = "integer")
    private Integer updated;

    /** 区域ID，可为空 */
    @MpField(value = "regionauth_id", columnType = "string", nullable = true, comment = "区域ID")
    private String regionauthId;
}
