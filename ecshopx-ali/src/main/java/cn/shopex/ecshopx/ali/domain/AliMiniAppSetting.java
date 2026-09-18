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

package cn.shopex.ecshopx.ali.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 支付宝小程序配置表。表级唯一约束：company_id；authorizer_appid。
 */
@Data
@MpTable(value = "ali_mini_app_setting", comment = "支付宝小程序配置表", uniqueIndexes = {@MpIndex(name = "ix_company_id", columns = {"company_id"}), @MpIndex(name = "ix_authorizer_appid", columns = {"authorizer_appid"})})
public class AliMiniAppSetting {

    /** 配置id */
    @MpId(value = "setting_id", type = IdType.AUTO, columnType = "bigint", comment = "配置id")
    private Long settingId;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 支付宝小程序 appid，varchar(64) */
    @MpField(value = "authorizer_appid", columnType = "string", length = 64, comment = "支付宝小程序appid")
    private String authorizerAppid;

    /** 应用私钥 */
    @MpField(value = "merchant_private_key", columnType = "text", comment = "应用私钥")
    private String merchantPrivateKey;

    /** api 加密类型，varchar(64) */
    @MpField(value = "api_sign_method", columnType = "string", length = 64, comment = "api加密类型")
    private String apiSignMethod;

    /** 支付宝公钥证书文件路径，可空 */
    @MpField(value = "alipay_cert_path", columnType = "text", nullable = true, comment = "支付宝公钥证书文件路径")
    private String alipayCertPath;

    /** 支付宝根证书文件路径，可空 */
    @MpField(value = "alipay_root_cert_path", columnType = "text", nullable = true, comment = "支付宝根证书文件路径")
    private String alipayRootCertPath;

    /** 应用公钥证书文件路径，可空 */
    @MpField(value = "merchant_cert_path", columnType = "text", nullable = true, comment = "应用公钥证书文件路径")
    private String merchantCertPath;

    /** 支付宝公钥字符串，可空 */
    @MpField(value = "alipay_public_key", columnType = "text", nullable = true, comment = "支付宝公钥字符串")
    private String alipayPublicKey;

    /** 支付类接口异步通知接收服务地址，可空 */
    @MpField(value = "notify_url", columnType = "string", nullable = true, comment = "支付类接口异步通知接收服务地址")
    private String notifyUrl;

    /** AES 密钥，可空 */
    @MpField(value = "encrypt_key", columnType = "string", nullable = true, comment = "AES密钥")
    private String encryptKey;

    /** 创建时间，bigint 非空 */
    @MpField(value = "created", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long created;

    /** 更新时间，bigint 非空 */
    @MpField(value = "updated", columnType = "integer", columnDefinition = "bigint NOT NULL")
    private Long updated;
}
