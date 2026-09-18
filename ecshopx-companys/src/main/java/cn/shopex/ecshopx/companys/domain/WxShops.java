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

package cn.shopex.ecshopx.companys.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/**
 * 微信门店表
 */
@Data
@MpTable(value = "wxshops", comment = "微信门店表")
public class WxShops {

    /** 自增id */
    @MpId(value = "wx_shop_id", type = IdType.AUTO, columnType = "bigint", comment = "自增id")
    private Long wxShopId;

    /** 从腾讯地图换取的位置点id，即search_map_poi接口返回的sosomap_poi_uid字段 */
    @MpField(value = "map_poi_id", columnType = "string", nullable = true, comment = "从腾讯地图换取的位置点id，即search_map_poi接口返回的sosomap_poi_uid字段")
    private String mapPoiId;

    /** 腾讯地图的门店名称 */
    @MpField(value = "store_name", columnType = "string", length = 100, nullable = true, comment = "腾讯地图的门店名称")
    private String storeName;

    /** 门店id */
    @MpField(value = "poi_id", columnType = "string", nullable = true, comment = "门店id")
    private String poiId;

    /** 腾讯地图纬度 */
    @MpField(value = "lng", columnType = "string", nullable = true, comment = "腾讯地图纬度")
    private String lng;

    /** 腾讯地图经度 */
    @MpField(value = "lat", columnType = "string", nullable = true, comment = "腾讯地图经度")
    private String lat;

    /** 腾讯地图门店地址 */
    @MpField(value = "address", columnType = "string", length = 500, comment = "腾讯地图门店地址")
    private String address;

    /** 腾讯地图门店类目 */
    @MpField(value = "category", columnType = "string", nullable = true, comment = "腾讯地图门店类目")
    private String category;

    /** 门店所属店铺ID */
    @MpField(value = "distributor_id", columnType = "bigint", comment = "门店所属店铺ID", defaultValue = "0")
    private Long distributorId = 0L;

    /** 门店图片，可传多张图片pic_list字段是一个json */
    @MpField(value = "pic_list", columnType = "text", nullable = true, comment = "门店图片，可传多张图片pic_list字段是一个json")
    private String picList;

    /** 联系电话 */
    @MpField(value = "contract_phone", columnType = "string", length = 20, comment = "联系电话")
    private String contractPhone;

    /** 1,公众号主体；2,相关主体; 3,无主体 */
    @MpField(value = "add_type", columnType = "smallint", comment = "1,公众号主体；2,相关主体; 3,无主体")
    private Integer addType = 3;

    /** 营业时间，格式11:11-12:12 */
    @MpField(value = "hour", columnType = "string", length = 20, comment = "营业时间，格式11:11-12:12")
    private String hour;

    /** 经营资质证件号 */
    @MpField(value = "credential", columnType = "string", length = 30, nullable = true, comment = "经营资质证件号")
    private String credential;

    /** 主体名字 临时素材mediaid，如果复用公众号主体，则company_name为空，如果不复用公众号主体，则company_name为具体的主体名字 */
    @MpField(value = "company_name", columnType = "string", length = 30, nullable = true, comment = "主体名字 临时素材mediaid，如果复用公众号主体，则company_name为空，如果不复用公众号主体，则company_name为具体的主体名字")
    private String companyName;

    /** 相关证明材料，临时素材mediaid，不复用公众号主体时，才需要填 */
    @MpField(value = "qualification_list", columnType = "string", length = 255, nullable = true, comment = "相关证明材料，临时素材mediaid，不复用公众号主体时，才需要填")
    private String qualificationList;

    /** 卡券id，如果不需要添加卡券，该参数可为空，目前仅开放支持会员卡、买单和刷卡支付券，不支持自定义code，需要先去公众平台卡券后台创建cardid */
    @MpField(value = "card_id", columnType = "string", length = 20, nullable = true, comment = "卡券id，如果不需要添加卡券，该参数可为空，目前仅开放支持会员卡、买单和刷卡支付券，不支持自定义code，需要先去公众平台卡券后台创建cardid")
    private String cardId;

    /** 审核状态，1：审核成功，2：审核中，3：审核失败，4：管理员拒绝, 5: 无需审核 */
    @MpField(value = "status", columnType = "smallint", comment = "审核状态，1：审核成功，2：审核中，3：审核失败，4：管理员拒绝, 5: 无需审核")
    private Integer status = 5;

    /** 审核失败原因 */
    @MpField(value = "errmsg", columnType = "string", length = 255, nullable = true, comment = "审核失败原因")
    private String errmsg;

    /** 公司id */
    @MpField(value = "company_id", columnType = "bigint", comment = "公司id")
    private Long companyId;

    /** 微信返回的审核id */
    @MpField(value = "audit_id", columnType = "string", length = 20, nullable = true, comment = "微信返回的审核id")
    private String auditId;

    /** 资源包id */
    @MpField(value = "resource_id", columnType = "bigint", nullable = true, comment = "资源包id")
    private Long resourceId;

    /** 过期时间 */
    @MpField(value = "expired_at", columnType = "bigint", nullable = true, comment = "过期时间")
    private Long expiredAt;

    /** 是否是默认门店 */
    @MpField(value = "is_default", columnType = "boolean", comment = "是否是默认门店", defaultValue = "False")
    private Boolean isDefault = false;

    /** 非中国国家名称 */
    @MpField(value = "country", columnType = "string", length = 100, nullable = true, comment = "非中国国家名称")
    private String country;

    /** 非中国门店所在城市 */
    @MpField(value = "city", columnType = "string", length = 100, nullable = true, comment = "非中国门店所在城市")
    private String city;

    /** 是否是中国国内门店 1:国内(包含港澳台),2:非国内 */
    @MpField(value = "is_domestic", columnType = "smallint", length = 1, nullable = true, comment = "是否是中国国内门店 1:国内(包含港澳台),2:非国内", defaultValue = "1")
    private Integer isDomestic = 1;

    /** 是否为直营店 1:直营店,2:非直营店 */
    @MpField(value = "is_direct_store", columnType = "smallint", length = 1, nullable = true, comment = "是否为直营店 1:直营店,2:非直营店", defaultValue = "1")
    private Integer isDirectStore = 1;

    /** 是否开启 1:开启,0:关闭 */
    @MpField(value = "is_open", columnType = "boolean", length = 1, nullable = true, comment = "是否开启 1:开启,0:关闭", defaultValue = "True")
    private Boolean isOpen = true;

    @MpField(value = "created", columnType = "integer")
    private Integer created;

    @MpField(value = "updated", columnType = "integer")
    private Integer updated;
}
