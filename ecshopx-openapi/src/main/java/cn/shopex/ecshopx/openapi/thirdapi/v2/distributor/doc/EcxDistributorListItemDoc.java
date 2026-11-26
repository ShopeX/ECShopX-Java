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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor.doc;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 仅 OpenAPI 文档；{@code data.list} 单条，字段来源 {@code OpenapiDistributorOpenApiRowFormatSupport#toOpenapiRow}。 */
@Schema(name = "EcxDistributorListItem")
public class EcxDistributorListItemDoc {

	@Schema(description = "店铺 ID", example = "1001")
	public Integer distributor_id;

	@Schema(description = "店铺号", example = "S001")
	public String shop_code;

	@Schema(description = "状态：0 已删除，1 启用，2 禁用；-1 未知", example = "1")
	public Integer status;

	@Schema(description = "店铺名称")
	public String distributor_name;

	@Schema(description = "联系人姓名")
	public String contact_username;

	@Schema(description = "联系人手机")
	public String contact_mobile;

	@Schema(description = "省")
	public String province;

	@Schema(description = "市")
	public String city;

	@Schema(description = "区")
	public String area;

	@ArraySchema(schema = @Schema(description = "区域编码"))
	public List<String> region_codes;

	@ArraySchema(schema = @Schema(description = "区域名称"))
	public List<String> region_names;

	@Schema(description = "详细地址")
	public String address;

	@Schema(description = "经度")
	public String lng;

	@Schema(description = "纬度")
	public String lat;

	@Schema(description = "营业时间描述")
	public String hour;

	@Schema(description = "店铺 logo URL")
	public String logo;

	@Schema(description = "是否支持自提：0/1", example = "0")
	public Integer is_ziti;

	@Schema(description = "是否支持配送：0/1", example = "1")
	public Integer is_delivery;

	@Schema(description = "是否自动同步商品：0/1", example = "0")
	public Integer is_auto_sync_goods;

	@Schema(description = "是否达达配送：0/1", example = "0")
	public Integer is_dada;

	@Schema(description = "是否默认店铺：0/1", example = "0")
	public Integer is_default;

	@Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss")
	public String created;

	@Schema(description = "更新时间，格式 yyyy-MM-dd HH:mm:ss")
	public String updated;
}