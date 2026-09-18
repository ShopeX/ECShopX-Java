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

package cn.shopex.ecshopx.wsugc.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 笔记与话题关联 */
@Data
@MpTable(value = "wsugc_post_topic", indexes = {@MpIndex(name = "idx_post_id", columns = {"post_id"}), @MpIndex(name = "idx_topic_id", columns = {"topic_id"})})
public class PostTopic {

    @MpId(value = "post_topic_id", type = IdType.AUTO, columnType = "bigint")
    private Long postTopicId;

    /** 笔记id */
    @MpField(value = "post_id", columnType = "bigint", length = 50, comment = "笔记id")
    private Long postId;

    /** 话题id */
    @MpField(value = "topic_id", columnType = "string", length = 50, comment = "话题id")
    private String topicId;

    /** 添加时间 */
    @MpField(value = "created", columnType = "integer", comment = "添加时间")
    private Integer created;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;
}
