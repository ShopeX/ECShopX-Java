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

package cn.shopex.ecshopx.kaquan.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 卡券库存统计表 */
@Data
@MpTable(value = "kaquan_card_related", comment = "卡券库存统计表", uniqueIndexes = {@MpIndex(name = "idx_key", columns = {"card_id", "company_id"})})
public class CardRelated {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    /** 卡券id */
    @MpField(value = "card_id", columnType = "string", length = 40, comment = "卡券id")
    private String cardId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 卡券名,最大9个汉字 */
    @MpField(value = "title", columnType = "string", length = 27, comment = "卡券名,最大9个汉字")
    private String title;

    /** 卡券总库存 */
    @MpField(value = "quantity", columnType = "integer", nullable = true, comment = "卡券总库存", defaultValue = "0")
    private Integer quantity = 0;

    /** 被领取数量 */
    @MpField(value = "get_num", columnType = "integer", nullable = true, comment = "被领取数量", defaultValue = "0")
    private Integer getNum = 0;

    /** 被核销数量 */
    @MpField(value = "consume_num", columnType = "integer", nullable = true, comment = "被核销数量", defaultValue = "0")
    private Integer consumeNum = 0;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
