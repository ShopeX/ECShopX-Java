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

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 笔记评论 */
@Data
@MpTable(value = "wsugc_comment", comment = "笔记评论")
public class Comment {

    @MpId(value = "comment_id", type = IdType.AUTO, columnType = "bigint")
    private Long commentId;

    /** 笔记id */
    @MpField(value = "post_id", columnType = "bigint", comment = "笔记id")
    private Long postId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 父级评论id */
    @MpField(value = "parent_comment_id", columnType = "bigint", comment = "父级评论id")
    private Long parentCommentId;

    /** 回复评论id */
    @MpField(value = "reply_comment_id", columnType = "bigint", comment = "回复评论id")
    private Long replyCommentId;

    /** 回复会员id */
    @MpField(value = "reply_user_id", columnType = "bigint", comment = "回复会员id")
    private Long replyUserId;

    /** 内容 */
    @MpField(value = "content", columnType = "text", nullable = true, comment = "内容")
    private String content = "";

    /** 点赞数 */
    @MpField(value = "likes", columnType = "string", comment = "点赞数")
    private String likes = "0";

    /** ip */
    @MpField(value = "ip", columnType = "string", nullable = true, comment = "ip")
    private String ip;

    /** 省份 */
    @MpField(value = "province", columnType = "string", nullable = true, comment = "省份")
    private String province;

    /** 城市 */
    @MpField(value = "city", columnType = "string", nullable = true, comment = "城市")
    private String city;

    /** 区县 */
    @MpField(value = "district", columnType = "string", nullable = true, comment = "区县")
    private String district;

    /** 排序 */
    @MpField(value = "p_order", columnType = "integer", nullable = true, comment = "排序", defaultValue = "0")
    private Integer pOrder = 0;

    /** 审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝) */
    @MpField(value = "status", columnType = "integer", comment = "审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝)", defaultValue = "0")
    private Integer status = 0;

    /** 上架状态(0下架,1上架) */
    @MpField(value = "enable", columnType = "integer", comment = "上架状态(0下架,1上架)", defaultValue = "1")
    private Integer enable = 1;

    /** 是否无效 */
    @MpField(value = "disabled", columnType = "boolean", comment = "是否无效", defaultValue = "False")
    private Boolean disabled = false;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 机器审核时间 */
    @MpField(value = "ai_verify_time", columnType = "bigint", nullable = true, comment = "机器审核时间", defaultValue = "0")
    private Long aiVerifyTime = 0L;

    /** 人工审核时间 */
    @MpField(value = "manual_verify_time", columnType = "bigint", nullable = true, comment = "人工审核时间", defaultValue = "0")
    private Long manualVerifyTime = 0L;

    /** 机器拒绝理由 */
    @MpField(value = "ai_refuse_reason", columnType = "string", nullable = true, comment = "机器拒绝理由")
    private String aiRefuseReason;

    /** 人工拒绝理由 */
    @MpField(value = "manual_refuse_reason", columnType = "string", nullable = true, comment = "人工拒绝理由")
    private String manualRefuseReason;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;
}
