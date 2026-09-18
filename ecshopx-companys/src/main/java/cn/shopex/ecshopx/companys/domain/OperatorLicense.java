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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 商户许可协议表
 */
@Data
@MpTable(value = "operator_license", comment = "商户许可协议表")
public class OperatorLicense {

    /** 协议id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "协议id")
    private Long id;

    /** 许可协议类型: app */
    @MpField(value = "type", columnType = "string", comment = "许可协议类型: app")
    private String type;

    /** 许可协议标题 */
    @MpField(value = "title", columnType = "string", comment = "许可协议标题")
    private String title;

    /** 许可协议内容 */
    @MpField(value = "content", columnType = "text", comment = "许可协议内容")
    private String content;
}
