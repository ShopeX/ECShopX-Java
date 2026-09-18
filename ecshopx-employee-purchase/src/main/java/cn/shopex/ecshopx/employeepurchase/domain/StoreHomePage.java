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

package cn.shopex.ecshopx.employeepurchase.domain;

import cn.shopex.ecshopx.common.mybatis.metadata.MpField;
import cn.shopex.ecshopx.common.mybatis.metadata.MpId;
import cn.shopex.ecshopx.common.mybatis.metadata.MpIndex;
import cn.shopex.ecshopx.common.mybatis.metadata.MpTable;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

/** 内购模版（门店首页配置） */
@Data
@MpTable(
		value = "employee_purchase_store_home_page",
		comment = "内购模版（门店首页配置）",
		indexes = {
			@MpIndex(name = "idx_company_id", columns = {"company_id"}),
			@MpIndex(name = "idx_distributor_id", columns = {"distributor_id"})
		})
public class StoreHomePage {

	@MpId(value = "id", type = IdType.AUTO, columnType = "bigint unsigned")
	private Long id;

	@MpField(value = "company_id", columnType = "bigint", comment = "公司ID")
	private Long companyId;

	@MpField(value = "distributor_id", columnType = "integer", comment = "门店ID", defaultValue = "0")
	private Integer distributorId = 0;

	@MpField(value = "template_name", columnType = "string", length = 64, nullable = true, comment = "小程序模板名称")
	private String templateName;

	@MpField(value = "page_name", columnType = "string", length = 255, comment = "页面名称")
	private String pageName;

	@MpField(value = "page_description", columnType = "string", length = 500, comment = "页面描述")
	private String pageDescription;

	@MpField(value = "page_share_title", columnType = "string", length = 255, nullable = true, comment = "分享标题")
	private String pageShareTitle;

	@MpField(value = "page_share_desc", columnType = "string", length = 500, nullable = true, comment = "分享描述")
	private String pageShareDesc;

	@MpField(value = "page_share_imageUrl", columnType = "string", length = 500, nullable = true, comment = "分享图片")
	private String pageShareImageUrl;

	@MpField(value = "is_open", columnType = "integer", comment = "是否开启", defaultValue = "1")
	private Integer isOpen = 1;

	@MpField(value = "weapp_customize_page_id", columnType = "bigint unsigned", nullable = true)
	private Long weappCustomizePageId;

	@MpField(value = "created", columnType = "integer", nullable = true)
	private Integer created;

	@MpField(value = "updated", columnType = "integer", nullable = true)
	private Integer updated;
}
