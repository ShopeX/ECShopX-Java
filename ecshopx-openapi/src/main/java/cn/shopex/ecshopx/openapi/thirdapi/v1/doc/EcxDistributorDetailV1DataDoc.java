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

package cn.shopex.ecshopx.openapi.thirdapi.v1.doc;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 仅 OpenAPI 文档；{@code OpenapiEnvelope.data} 成功载荷（单对象，非列表），来源
 * {@code OpenapiThirdApiV1DistributorDetailService#execute} / {@code toOpenapiDetailRow}。
 * contact/mobile 解密后返回；name/contact/logo/省市区地址按默认语言分表覆盖。
 */
@Schema(name = "EcxDistributorDetailV1Data", description = "店铺详情单对象（23 字段）")
public class EcxDistributorDetailV1DataDoc {

	@Schema(description = "店铺 ID（整型化，null→0）", example = "1001")
	public Integer distributor_id;

	@Schema(description = "店铺号；null→空串", example = "S001")
	public String shop_code;

	@Schema(description = "状态：0 已删除（is_valid=delete）；1 启用（true）；2 禁用（false）；-1 未知", example = "1")
	public Integer status;

	@Schema(description = "店铺名称（默认语言分表覆盖）；null→空串", example = "示例门店")
	public String distributor_name;

	@Schema(description = "联系人姓名（解密 + 默认语言分表覆盖）；null→空串", example = "张三")
	public String contact_username;

	@Schema(description = "联系人手机（解密）；null→空串", example = "13800000000")
	public String contact_mobile;

	@Schema(description = "省（默认语言分表覆盖）；null→空串", example = "上海市")
	public String province;

	@Schema(description = "市（默认语言分表覆盖）；null→空串", example = "上海市")
	public String city;

	@Schema(description = "区（默认语言分表覆盖）；null→空串", example = "黄浦区")
	public String area;

	@ArraySchema(arraySchema = @Schema(description = "区域编码列表（regions_id JSON 数组/逗号串解码）"))
	public List<String> region_codes;

	@ArraySchema(arraySchema = @Schema(description = "区域名称列表（regions JSON 数组/逗号串解码）"))
	public List<String> region_names;

	@Schema(description = "详细地址（默认语言分表覆盖）；null→空串", example = "南京东路 1 号")
	public String address;

	@Schema(description = "经度；null→空串", example = "121.47")
	public String lng;

	@Schema(description = "纬度；null→空串", example = "31.23")
	public String lat;

	@Schema(description = "营业时间描述；null→空串", example = "10:00-22:00")
	public String hour;

	@Schema(description = "店铺 logo URL（默认语言分表覆盖）；null→空串", example = "/fixtures/logo.png")
	public String logo;

	@Schema(description = "是否支持自提：0/1（缺省 0）", example = "0")
	public Integer is_ziti;

	@Schema(description = "是否支持配送：0/1（缺省 1）", example = "1")
	public Integer is_delivery;

	@Schema(description = "是否自动同步商品：0/1（缺省 0）", example = "0")
	public Integer is_auto_sync_goods;

	@Schema(description = "是否达达配送：0/1（缺省 0）", example = "0")
	public Integer is_dada;

	@Schema(description = "是否默认店铺：0/1（缺省 0）", example = "0")
	public Integer is_default;

	@Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss；null/非法→空串", example = "2026-11-26 10:00:00")
	public String created;

	@Schema(description = "更新时间，格式 yyyy-MM-dd HH:mm:ss；null/非法→空串", example = "2026-11-26 10:00:00")
	public String updated;
}