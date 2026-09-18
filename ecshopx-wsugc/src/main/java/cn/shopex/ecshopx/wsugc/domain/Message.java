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

/** 消息 */
@Data
@MpTable(value = "wsugc_message", comment = "消息", indexes = {@MpIndex(name = "idx_to_user_id", columns = {"to_user_id"}), @MpIndex(name = "idx_type", columns = {"type"}), @MpIndex(name = "idx_post_id", columns = {"post_id"}), @MpIndex(name = "idx_comment_id", columns = {"comment_id"}), @MpIndex(name = "idx_hasread", columns = {"hasread"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class Message {

    @MpId(value = "message_id", type = IdType.AUTO, columnType = "bigint")
    private Long messageId;

    /**
     * 消息类型.system系统消息;replyPost回复消息likePost笔记点赞 favoritePost收藏笔记 followerUser关注了您
     */
    @MpField(value = "type", columnType = "string", comment = "消息类型.system系统消息;replyPost回复消息likePost笔记点赞 favoritePost收藏笔记 followerUser关注了您")
    private String type;

    /**
     * 消息类型及子类型.系统消息(system):approve通过,reject拒绝,unenable下架;
     * 回复消息(reply):replyPost评论了您的笔记,replyComment回复了您的评论;
     * 收藏笔记(favoritePost):favorite收藏,unfavorite取消收藏;
     * 关注(followerUser):follow关注,unfollow取关;
     * 点赞(like):likePost点赞笔记,likeComment点赞评论
     */
    @MpField(value = "sub_type", columnType = "string", comment = "消息类型及子类型.系统消息(system):approve通过,reject拒绝,unenable下架; 回复消息(reply):replyPost评论了您的笔记,replyComment回复了您的评论; 收藏笔记(favoritePost):favorite收藏,unfavorite取消收藏; 关注(followerUser):follow关注,unfollow取关;  点赞(like):likePost点赞笔记,likeComment点赞评论")
    private String subType;

    /** 来源 1用户,2官方(系统通知) */
    @MpField(value = "source", columnType = "integer", nullable = true, comment = "来源 1用户,2官方(系统通知)", defaultValue = "1")
    private Integer source = 1;

    /** 来自用户 */
    @MpField(value = "from_user_id", columnType = "bigint", comment = "来自用户", defaultValue = "0")
    private Long fromUserId = 0L;

    /** 来自昵称 */
    @MpField(value = "from_nickname", columnType = "string", nullable = true, comment = "来自昵称")
    private String fromNickname = "";

    /** 发给用户 */
    @MpField(value = "to_user_id", columnType = "bigint", comment = "发给用户", defaultValue = "0")
    private Long toUserId = 0L;

    /** 发给昵称 */
    @MpField(value = "to_nickname", columnType = "string", nullable = true, comment = "发给昵称")
    private String toNickname = "";

    /** 笔记id */
    @MpField(value = "post_id", columnType = "bigint", comment = "笔记id", defaultValue = "0")
    private Long postId = 0L;

    /** 评论id */
    @MpField(value = "comment_id", columnType = "bigint", nullable = true, comment = "评论id", defaultValue = "0")
    private Long commentId = 0L;

    /** 通知标题 */
    @MpField(value = "title", columnType = "string", length = 255, nullable = true, comment = "通知标题")
    private String title;

    /** 通知内容 */
    @MpField(value = "content", columnType = "string", length = 255, nullable = true, comment = "通知内容")
    private String content;

    /** 添加时间 */
    @MpField(value = "created", columnType = "integer", comment = "添加时间")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;

    /** 是否已读. */
    @MpField(value = "hasread", columnType = "boolean", comment = "是否已读.", defaultValue = "False")
    private Boolean hasRead = false;
}
