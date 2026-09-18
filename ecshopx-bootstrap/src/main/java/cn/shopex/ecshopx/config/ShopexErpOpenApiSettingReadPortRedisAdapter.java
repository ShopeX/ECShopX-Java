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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.goods.service.ome.ShopexErpSettingRedisAccessor;
import cn.shopex.ecshopx.thirdparty.service.ome.ShopexErpOpenApiSettingReadPort;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ShopexErpOpenApiSettingReadPortRedisAdapter implements ShopexErpOpenApiSettingReadPort {

	private final ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor;

	public ShopexErpOpenApiSettingReadPortRedisAdapter(ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor) {
		this.shopexErpSettingRedisAccessor = shopexErpSettingRedisAccessor;
	}

	@Override
	public Optional<Map<String, Object>> loadParsed(long companyId) {
		return Optional.ofNullable(shopexErpSettingRedisAccessor.getParsedSetting(companyId));
	}
}
