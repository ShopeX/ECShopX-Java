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

package cn.shopex.ecshopx.theme.support;

import cn.shopex.ecshopx.theme.domain.ThemeMemberCenterShare;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ThemeMemberCenterShareColumnNamesDataMapper {

	public Map<String, Object> toColumnNamesData(ThemeMemberCenterShare entity) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("theme_member_center_share_id", entity.getThemeMemberCenterShareId());
		map.put("company_id", entity.getCompanyId());
		map.put("share_title", entity.getShareTitle());
		map.put("share_description", entity.getShareDescription());
		map.put("share_pic_wechatapp", entity.getSharePicWechatapp() == null ? "" : entity.getSharePicWechatapp());
		map.put("share_pic_h5", entity.getSharePicH5() == null ? "" : entity.getSharePicH5());
		map.put("created", entity.getCreated());
		map.put("updated", entity.getUpdated());
		return map;
	}
}
