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

package cn.shopex.ecshopx.youshu.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 有数参数设置表。索引：company_id（idx_company_id）。
 */
@Data
@MpTable(value = "youshu_setting", comment = "有数参数设置表", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class YoushuSetting {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 公司 id */
    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** merchant_id */
    @MpField(value = "merchant_id", columnType = "string", nullable = true, comment = "merchant_id")
    private String merchantId;

    /** 有数app_id，正式 */
    @MpField(value = "app_id", columnType = "string", nullable = true, comment = "有数app_id，正式")
    private String appId;

    /** 有数app_secret，正式 */
    @MpField(value = "app_secret", columnType = "string", nullable = true, comment = "有数app_secret，正式")
    private String appSecret;

    /** 有数后端api url，正式 */
    @MpField(value = "api_url", columnType = "string", nullable = true, comment = "有数后端api url，正式")
    private String apiUrl;

    /** 有数app_id，沙箱 */
    @MpField(value = "sandbox_app_id", columnType = "string", nullable = true, comment = "有数app_id，沙箱")
    private String sandboxAppId;

    /** 有数app_secret，沙箱 */
    @MpField(value = "sandbox_app_secret", columnType = "string", nullable = true, comment = "有数app_secret，沙箱")
    private String sandboxAppSecret;

    /** 有数后端api url，沙箱 */
    @MpField(value = "sandbox_api_url", columnType = "string", nullable = true, comment = "有数后端api url，沙箱")
    private String sandboxApiUrl;

    /** 小程序名称 */
    @MpField(value = "weapp_name", columnType = "string", nullable = true, comment = "小程序名称")
    private String weappName;

    /** 小程序app_id */
    @MpField(value = "weapp_app_id", columnType = "string", nullable = true, comment = "小程序app_id")
    private String weappAppId;

    /** 创建时间，datetime 非空 */
    @MpField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间，datetime 非空 */
    @MpField("updated_at")
    private LocalDateTime updatedAt;
}
