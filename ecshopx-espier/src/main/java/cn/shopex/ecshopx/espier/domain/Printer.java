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
 * 打印机表
 *
 * <p>打印机类型：yilianyun 易连云
 */
@Data
@MpTable(value = "espier_printer", comment = "打印机表")
public class Printer {

    /** 公司id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "公司id")
    private Long id;

    /** 打印机名称 */
    @MpField(value = "name", columnType = "string", comment = "打印机名称")
    private String name;

    /** 打印机类型 yilianyun 易连云 */
    @MpField(value = "type", columnType = "string", comment = "打印机类型 yilianyun 易连云")
    private String type;

    /** 公司company id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司company id")
    private Long companyId;

    /** 关联店铺 */
    @MpField(value = "distributor_id", columnType = "string", comment = "关联店铺")
    private String distributorId;

    /** 打印机终端号 */
    @MpField(value = "app_terminal", columnType = "string", comment = "打印机终端号")
    private String appTerminal;

    /** 打印机秘钥 */
    @MpField(value = "app_key", columnType = "string", comment = "打印机秘钥")
    private String appKey;
}
