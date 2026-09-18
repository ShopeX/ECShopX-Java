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
 * adapay上传商户证照
 */
@Data
@MpTable(value = "adapay_upload_license", comment = "adapay上传商户证照", indexes = {@MpIndex(name = "idx_company_id", columns = {"company_id"})})
public class AdapayUploadLicense {

    /** ID */
    @MpId(value = "id", type = IdType.AUTO, columnType = "bigint", comment = "ID")
    private Long id;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 渠道商下商户的apiKey */
    @MpField(value = "sub_api_key", columnType = "string", comment = "渠道商下商户的apiKey")
    private String subApiKey;

    /** 文件路径 */
    @MpField(value = "file_url", columnType = "text", comment = "文件路径")
    private String fileUrl;

    /** 图片类型，01：三证合一码，02：法人/小微负责人身份证正面，03：法人/小微负责人身份证反面，04：门店，05：开户许可证/小微负责人银行卡正面照，06：股东身份证正面，07：股东身份证反面，08：结算账号开户证明，09：网站截图，10：行业资质文件，11：icp备案许可证明或者许可证编码，12：租赁合同，13：交易测试记录，14：业务场景证明材料 */
    @MpField(value = "file_type", columnType = "string", length = 10, comment = "图片类型，01：三证合一码，02：法人/小微负责人身份证正面，03：法人/小微负责人身份证反面，04：门店，05：开户许可证/小微负责人银行卡正面照，06：股东身份证正面，07：股东身份证反面，08：结算账号开户证明，09：网站截图，10：行业资质文件，11：icp备案许可证明或者许可证编码，12：租赁合同，13：交易测试记录，14：业务场景证明材料")
    private String fileType;

    /** Adapay系统生成的图片id，作为 提交商户证照 接口的请求参数上送至Adapay */
    @MpField(value = "pic_id", columnType = "string", comment = "Adapay系统生成的图片id，作为 提交商户证照 接口的请求参数上送至Adapay")
    private String picId;

    /** 错误描述 */
    @MpField(value = "error_msg", columnType = "string", length = 500, nullable = true, comment = "错误描述")
    private String errorMsg;

    /** 创建时间 */
    @MpField(value = "create_time", columnType = "integer", comment = "创建时间")
    private Integer createTime;

    /** 更新时间 */
    @MpField(value = "update_time", columnType = "integer", nullable = true, comment = "更新时间")
    private Integer updateTime;
}
