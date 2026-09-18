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

/** 笔记 */
@Data
@MpTable(value = "wsugc_post", comment = "笔记", indexes = {@MpIndex(name = "idx_user_id", columns = {"user_id"}), @MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class Post {

    @MpId(value = "post_id", type = IdType.AUTO, columnType = "bigint")
    private Long postId;

    /** 用户id */
    @MpField(value = "user_id", columnType = "bigint", comment = "用户id")
    private Long userId;

    /** 管理员id */
    @MpField(value = "operator_id", columnType = "bigint", nullable = true, comment = "管理员id", defaultValue = "0")
    private Long operatorId = 0L;

    /** 来源 1用户,2官方 */
    @MpField(value = "source", columnType = "integer", nullable = true, comment = "来源 1用户,2官方", defaultValue = "1")
    private Integer source = 1;

    /** 标题 */
    @MpField(value = "title", columnType = "string", comment = "标题")
    private String title;

    /** 点赞数 */
    @MpField(value = "likes", columnType = "integer", comment = "点赞数")
    private Integer likes = 0;

    /** ip地址 */
    @MpField(value = "ip", columnType = "string", nullable = true, comment = "ip地址")
    private String ip;

    /** 手机号 */
    @MpField(value = "mobile", columnType = "string", nullable = true, comment = "手机号")
    private String mobile;

    /** 封面图 */
    @MpField(value = "cover", columnType = "string", nullable = true, comment = "封面图")
    private String cover;

    /** 视频 */
    @MpField(value = "video", columnType = "string", nullable = true, comment = "视频")
    private String video;

    /** 视频比例 */
    @MpField(value = "video_ratio", columnType = "string", nullable = true, comment = "视频比例")
    private String videoRatio;

    /** 视频位置 */
    @MpField(value = "video_place", columnType = "string", nullable = true, comment = "视频位置")
    private String videoPlace;

    /** 视频缩略图 */
    @MpField(value = "video_thumb", columnType = "string", nullable = true, comment = "视频缩略图")
    private String videoThumb;

    /** 坐标 */
    @MpField(value = "position", columnType = "string", nullable = true, comment = "坐标")
    private String position;

    /** 是否置顶. */
    @MpField(value = "is_top", columnType = "integer", comment = "是否置顶.", defaultValue = "0")
    private Integer isTop = 0;

    /** 地址 */
    @MpField(value = "address", columnType = "string", nullable = true, comment = "地址")
    private String address;

    /** 话题 */
    @MpField(value = "topics", columnType = "text", nullable = true, comment = "话题")
    private String topics;

    /** 角标 */
    @MpField(value = "badges", columnType = "text", nullable = true, comment = "角标")
    private String badges;

    /** 商品 */
    @MpField(value = "goods", columnType = "text", nullable = true, comment = "商品")
    private String goods;

    /** 多图 */
    @MpField(value = "images", columnType = "text", nullable = true, comment = "多图")
    private String images = "";

    /** 多图相对路径 */
    @MpField(value = "image_path", columnType = "text", nullable = true, comment = "多图相对路径")
    private String imagePath = "";

    /** 图片的tag信息 */
    @MpField(value = "image_tag", columnType = "text", nullable = true, comment = "图片的tag信息")
    private String imageTag = "";

    /** 内容 */
    @MpField(value = "content", columnType = "text", nullable = true, comment = "内容")
    private String content = "";

    /** 排序 */
    @MpField(value = "p_order", columnType = "integer", nullable = true, comment = "排序", defaultValue = "0")
    private Integer pOrder = 0;

    /** 分享次数 */
    @MpField(value = "share_nums", columnType = "integer", nullable = true, comment = "分享次数", defaultValue = "0")
    private Integer shareNums;

    /** 可见状态0所有，1仅自己 */
    @MpField(value = "view_auth", columnType = "integer", comment = "可见状态0所有，1仅自己", defaultValue = "0")
    private Integer viewAuth = 0;

    /** 是否草稿0否，1是 */
    @MpField(value = "is_draft", columnType = "integer", comment = "是否草稿0否，1是", defaultValue = "0")
    private Integer isDraft = 0;

    /** 发布状态 */
    @MpField(value = "enabled", columnType = "integer", comment = "发布状态", defaultValue = "0")
    private Integer enabled = 0;

    /** 是否无效(删除) */
    @MpField(value = "disabled", columnType = "integer", comment = "是否无效(删除)", defaultValue = "0")
    private Integer disabled = 0;

    /** 审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝) */
    @MpField(value = "status", columnType = "integer", comment = "审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝)", defaultValue = "0")
    private Integer status = 0;

    /** 标题审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝) */
    @MpField(value = "title_status", columnType = "integer", comment = "标题审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝)", defaultValue = "0")
    private Integer titleStatus = 0;

    /** 内容审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝) */
    @MpField(value = "content_status", columnType = "integer", comment = "内容审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝)", defaultValue = "0")
    private Integer contentStatus = 0;

    /** 图片审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝) */
    @MpField(value = "image_status", columnType = "integer", comment = "图片审核状态(0待审核,1审核通过,2机器拒绝,3待人工审核,4人工拒绝)", defaultValue = "0")
    private Integer imageStatus = 0;

    /** 微信内容审查-图片内容追踪id集合,仅ID ,111:false,2222:false, */
    @MpField(value = "trace_ids", columnType = "text", comment = "微信内容审查-图片内容追踪id集合,仅ID ,111:false,2222:false,")
    private String traceIds = "";

    /** 微信内容审查-图片内容追踪id集合 */
    @MpField(value = "mediacheck_traceid", columnType = "text", comment = "微信内容审查-图片内容追踪id集合")
    private String mediacheckTraceid = "";

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer", nullable = true)
    private Integer updated;

    /** 机器审核时间 */
    @MpField(value = "ai_verify_time", columnType = "bigint", nullable = true, comment = "机器审核时间", defaultValue = "0")
    private Long aiVerifyTime;

    /** 人工审核时间 */
    @MpField(value = "manual_verify_time", columnType = "bigint", nullable = true, comment = "人工审核时间", defaultValue = "0")
    private Long manualVerifyTime;

    /** 机器拒绝理由 */
    @MpField(value = "ai_refuse_reason", columnType = "string", nullable = true, comment = "机器拒绝理由")
    private String aiRefuseReason;

    /** 人工拒绝理由 */
    @MpField(value = "manual_refuse_reason", columnType = "string", nullable = true, comment = "人工拒绝理由")
    private String manualRefuseReason;

    @MpField(value = "company_id", columnType = "bigint")
    private Long companyId;
}
