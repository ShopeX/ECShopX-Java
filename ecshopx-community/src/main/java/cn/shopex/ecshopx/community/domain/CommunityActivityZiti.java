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

package cn.shopex.ecshopx.community.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 社区拼团活动自提表
 */
@Data
@MpTable(value = "community_activity_ziti", comment = "社区拼团活动自提表", indexes = {@MpIndex(name = "ix_activity_id", columns = {"activity_id"}), @MpIndex(name = "ix_ziti_id", columns = {"ziti_id"})})
public class CommunityActivityZiti {

    /** id */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "id")
    private Long id;

    /** 活动ID */
    @MpField(value = "activity_id", columnType = "bigint", comment = "活动ID")
    private Long activityId;

    /** 自提ID */
    @MpField(value = "ziti_id", columnType = "bigint", comment = "自提ID")
    private Long zitiId;

    /** 成团数量 */
    @MpField(value = "condition_num", columnType = "integer", comment = "成团数量")
    private Integer conditionNum;

    /** 备注，可为空 */
    @MpField(value = "remark", columnType = "string", nullable = true, comment = "备注")
    private String remark;
}
