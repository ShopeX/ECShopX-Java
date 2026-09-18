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

package cn.shopex.ecshopx.wechat.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import lombok.Data;

/** 微信授权表(公众号,小程序) */
@Data
@MpTable(value = "wechat_authorization", comment = "微信授权表(公众号,小程序)")
public class WechatAuth {

    /** (公众号，小程序)微信appid */
    @MpId(value = "authorizer_appid", type = IdType.INPUT, columnType = "string", length = 64, comment = "(公众号，小程序)微信appid")
    private String authorizerAppid;

    /** (公众号，小程序)微信appsecret */
    @MpField(value = "authorizer_appsecret", columnType = "string", nullable = true, comment = "(公众号，小程序)微信appsecret")
    private String authorizerAppsecret;

    /** 授权操作者id */
    @MpField(value = "operator_id", columnType = "bigint", comment = "授权操作者id")
    private Long operatorId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** (公众号，小程序)微信refresh_token */
    @MpField(value = "authorizer_refresh_token", columnType = "string", nullable = true, comment = "(公众号，小程序)微信refresh_token")
    private String authorizerRefreshToken;

    /** (公众号，小程序)昵称 */
    @MpField(value = "nick_name", columnType = "string", length = 50, comment = "(公众号，小程序)昵称")
    private String nickName;

    /** (公众号，小程序)头像 */
    @MpField(value = "head_img", columnType = "string", nullable = true, comment = "(公众号，小程序)头像")
    private String headImg;

    /**
     * (公众号，小程序)类型。可选值有 0代表订阅号；1代表由历史老帐号升级后的订阅号；2代表服务号；3代表小程序(自定义)
     */
    @MpField(value = "service_type_info", columnType = "integer", nullable = true, comment = "(公众号，小程序)类型。可选值有 0代表订阅号；1代表由历史老帐号升级后的订阅号；2代表服务号；3代表小程序(自定义)")
    private Integer serviceTypeInfo;

    /**
     * (公众号，小程序)认证类型。-1代表未认证;0代表微信认证;1代表新浪微博认证;2代表腾讯微博认证;3代表已资质认证通过但还未通过名称认证;4代表已资质认证通过、还未通过名称认证，但通过了新浪微博认证;5代表已资质认证通过、还未通过名称认证，但通过了腾讯微博认证
     */
    @MpField(value = "verify_type_info", columnType = "integer", nullable = true, comment = "(公众号，小程序)认证类型。-1代表未认证;0代表微信认证;1代表新浪微博认证;2代表腾讯微博认证;3代表已资质认证通过但还未通过名称认证;4代表已资质认证通过、还未通过名称认证，但通过了新浪微博认证;5代表已资质认证通过、还未通过名称认证，但通过了腾讯微博认证")
    private Integer verifyTypeInfo;

    /** (公众号，小程序)原始 ID */
    @MpField(value = "user_name", columnType = "string", length = 32, nullable = true, comment = "(公众号，小程序)原始 ID")
    private String userName;

    /** (小程序)账号介绍 */
    @MpField(value = "signature", columnType = "text", nullable = true, comment = "(小程序)账号介绍")
    private String signature;

    /** (公众号，小程序)主体名称 */
    @MpField(value = "principal_name", columnType = "string", nullable = true, comment = "(公众号，小程序)主体名称")
    private String principalName;

    /** (公众号)授权方公众号所设置的微信号，可能为空 */
    @MpField(value = "alias", columnType = "string", length = 50, nullable = true, comment = "(公众号)授权方公众号所设置的微信号，可能为空")
    private String alias;

    /**
     * (公众号，小程序)用以了解以下功能的开通状况（0代表未开通，1代表已开通）。open_store:是否开通微信门店功能;open_scan:是否开通微信扫商品功能;open_pay:是否开通微信支付功能;open_card:是否开通微信卡券功能;open_shake:是否开通微信摇一摇功能
     */
    @MpField(value = "business_info", columnType = "json_array", nullable = true, comment = "(公众号，小程序)用以了解以下功能的开通状况（0代表未开通，1代表已开通）。open_store:是否开通微信门店功能;open_scan:是否开通微信扫商品功能;open_pay:是否开通微信支付功能;open_card:是否开通微信卡券功能;open_shake:是否开通微信摇一摇功能")
    private String businessInfo;

    /** (公众号，小程序)二维码图片的URL */
    @MpField(value = "qrcode_url", columnType = "string", nullable = true, comment = "(公众号，小程序)二维码图片的URL")
    private String qrcodeUrl;

    /** (小程序)小程序配置，根据这个字段判断是否为小程序类型授权 */
    @MpField(value = "miniprograminfo", columnType = "json_array", nullable = true, comment = "(小程序)小程序配置，根据这个字段判断是否为小程序类型授权")
    private String miniprograminfo;

    /** (公众号，小程序)授权给开发者的权限集列表,逗号隔开 */
    @MpField(value = "func_info", columnType = "string", nullable = true, comment = "(公众号，小程序)授权给开发者的权限集列表,逗号隔开")
    private String funcInfo;

    /** 绑定状态 bind绑定 unbind绑定解除 */
    @MpField(value = "bind_status", columnType = "string", comment = "绑定状态 bind绑定 unbind绑定解除")
    private String bindStatus;

    /**
     * (小程序)自动发布,第三方授权模式才有用，直连用不到此配置。1:自动发布,0:不自动发布
     */
    @MpField(value = "auto_publish", columnType = "smallint", length = 1, comment = "(小程序)自动发布,第三方授权模式才有用，直连用不到此配置。1:自动发布,0:不自动发布", defaultValue = "0")
    private Integer autoPublish = 0;

    /** 是否直连。1:直连模式,0:第三方授权模式 */
    @MpField(value = "is_direct", columnType = "smallint", length = 1, comment = "是否直连。1:直连模式,0:第三方授权模式", defaultValue = "0")
    private Integer isDirect = 0;

    @MpField("created_at")
    private LocalDateTime createdAt;

    @MpField(value = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @MpField(value = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}
