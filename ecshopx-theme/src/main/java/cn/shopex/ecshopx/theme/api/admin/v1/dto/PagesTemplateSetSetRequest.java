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

package cn.shopex.ecshopx.theme.api.admin.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagesTemplateSetSetRequest {

	@JsonProperty("regionauth_id")
	private Long regionauthId;

	@JsonProperty("pages_template_id")
	private Long pagesTemplateId;

	@JsonProperty("index_type")
	private Integer indexType;

	@JsonProperty("is_enforce_sync")
	private Integer isEnforceSync;

	@JsonProperty("is_open_recommend")
	private Integer isOpenRecommend;

	@JsonProperty("is_open_wechatapp_location")
	private Integer isOpenWechatappLocation;

	@JsonProperty("is_open_scan_qrcode")
	private Integer isOpenScanQrcode;

	@JsonProperty("is_open_official_account")
	private Integer isOpenOfficialAccount;

	@JsonProperty("tab_bar")
	private String tabBar;
}
