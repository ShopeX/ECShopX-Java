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

package cn.shopex.ecshopx.adapay.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * adapay商户入驻
 */
@Data
@MpTable(value = "adapay_merchant_resident", comment = "adapay商户入驻", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"}), @MpIndex(name = "idx_request_id", columns = {"request_id"})})
public class AdapayMerchantResident {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 请求ID */
    @MpField(value = "request_id", columnType = "string", length = 100, comment = "请求ID")
    private String requestId;

    /** 商户开户进件返回的API Key */
    @MpField(value = "sub_api_key", columnType = "string", comment = "商户开户进件返回的API Key")
    private String subApiKey;

    /** 费率类型：01-标准费率线上，02-标准费率线下 */
    @MpField(value = "fee_type", columnType = "string", length = 10, comment = "费率类型：01-标准费率线上，02-标准费率线下")
    private String feeType;

    /** 商户开户进件返回的应用ID */
    @MpField(value = "app_id", columnType = "string", comment = "商户开户进件返回的应用ID")
    private String appId;

    /** 微信经营类目（与支付宝二选一） */
    @MpField(value = "wx_category", columnType = "string", length = 50, nullable = true, comment = "微信经营类目（与支付宝二选一）")
    private String wxCategory;

    /** 支付宝经营类目（与微信二选一） */
    @MpField(value = "alipay_category", columnType = "string", length = 50, nullable = true, comment = "支付宝经营类目（与微信二选一）")
    private String alipayCategory;

    /** 行业分类 */
    @MpField(value = "cls_id", columnType = "string", length = 50, nullable = true, comment = "行业分类")
    private String clsId;

    /** 入驻模式：1-服务商模式 */
    @MpField(value = "model_type", columnType = "string", length = 10, comment = "入驻模式：1-服务商模式")
    private String modelType;

    /** 商户种类，1-政府机构,2-国营企业,3-私营企业,4-外资企业,5-个体工商户,7-事业单位,8-小微 */
    @MpField(value = "mer_type", columnType = "string", length = 10, comment = "商户种类，1-政府机构,2-国营企业,3-私营企业,4-外资企业,5-个体工商户,7-事业单位,8-小微")
    private String merType;

    /** 省份编码 */
    @MpField(value = "province_code", columnType = "string", length = 20, comment = "省份编码")
    private String provinceCode;

    /** 城市编码 */
    @MpField(value = "city_code", columnType = "string", length = 20, comment = "城市编码")
    private String cityCode;

    /** 区县编码 */
    @MpField(value = "district_code", columnType = "string", length = 20, comment = "区县编码")
    private String districtCode;

    /** 支付渠道配置信息 */
    @MpField(value = "add_value_list", columnType = "text", comment = "支付渠道配置信息")
    private String addValueList;

    /** 总商户 手续费扣除方式 I:内扣 O:外扣 */
    @MpField(value = "adapay_fee_mode", columnType = "string", length = 20, comment = "总商户 手续费扣除方式 I:内扣 O:外扣")
    private String adapayFeeMode;

    /** 接口调用状态，succeeded - 成功 failed - 失败 pending - 处理中 */
    @MpField(value = "status", columnType = "string", length = 20, comment = "接口调用状态，succeeded - 成功 failed - 失败 pending - 处理中")
    private String status;

    /** 支付宝入驻结果：S-成功，F-失败 */
    @MpField(value = "alipay_stat", columnType = "string", length = 20, nullable = true, comment = "支付宝入驻结果：S-成功，F-失败")
    private String alipayStat;

    /** 支付宝入驻错误描述 */
    @MpField(value = "alipay_stat_msg", columnType = "string", length = 500, nullable = true, comment = "支付宝入驻错误描述")
    private String alipayStatMsg;

    /** 微信入驻结果：S-成功，F-失败 */
    @MpField(value = "wx_stat", columnType = "string", length = 20, nullable = true, comment = "微信入驻结果：S-成功，F-失败")
    private String wxStat;

    /** 微信入驻错误描述 */
    @MpField(value = "wx_stat_msg", columnType = "string", length = 500, nullable = true, comment = "微信入驻错误描述")
    private String wxStatMsg;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
