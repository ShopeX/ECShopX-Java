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

/** 评论点赞 */
@Data
@MpTable(value = "wsugc_comment_like", comment = "评论点赞", indexes = {@MpIndex(name = "idx_post_id", columns = {"post_id"}), @MpIndex(name = "idx_comment_id", columns = {"comment_id"}), @MpIndex(name = "idx_disabled", columns = {"disabled"})})
public class CommentLike {

    @MpId(value = "comment_like_id", type = IdType.AUTO, columnType = "bigint")
    private Long commentLikeId;

    /** 笔记id */
    @MpField(value = "post_id", columnType = "bigint", comment = "笔记id")
    private Long postId;

    /** 评论id */
    @MpField(value = "comment_id", columnType = "bigint", comment = "评论id")
    private Long commentId;

    /** 会员id */
    @MpField(value = "user_id", columnType = "bigint", comment = "会员id")
    private Long userId;

    /** 是否无效 */
    @MpField(value = "disabled", columnType = "boolean", comment = "是否无效", defaultValue = "False")
    private Boolean disabled = false;

    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Integer updated;
}
