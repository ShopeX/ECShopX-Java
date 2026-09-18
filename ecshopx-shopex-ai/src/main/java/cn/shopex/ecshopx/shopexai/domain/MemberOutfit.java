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

package cn.shopex.ecshopx.shopexai.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/** 会员穿搭模型 */
@Data
@MpTable(value = "member_outfit")
public class MemberOutfit {

    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint")
    private Long id;

    @MpField(value = "member_id", columnType = "integer")
    private Integer memberId;

    @MpField(value = "model_image", columnType = "string", length = 255)
    private String modelImage;

    /** 状态，默认 1 */
    @MpField(value = "status", columnType = "smallint")
    private Integer status = 1;

    @MpField(value = "created_at", columnType = "datetime")
    private LocalDateTime createdAt;

    @MpField(value = "updated_at", columnType = "datetime")
    private LocalDateTime updatedAt;
}
