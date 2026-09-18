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

package cn.shopex.ecshopx.point.service.shuyun;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ShuyunMemberPointSyncNoOp implements ShuyunMemberPointSyncPort {

	private final boolean oemShuyun;

	public ShuyunMemberPointSyncNoOp(
			@Value("${ecshopx.request-field.oem-shuyun:false}") boolean oemShuyun) {
		this.oemShuyun = oemShuyun;
	}

	@Override
	public long readMemberPointBalance(long companyId, long userId) {
		return 0L;
	}

	@Override
	public void shuyunAddPoint(
			int point, boolean plus, String record, String orderId, Map<String, Object> otherParams) {
		if (!oemShuyun) {
			return;
		}
		throw new ResourceException("数云积分同步未实现，请关闭 ecshopx.request-field.oem-shuyun 或完成对接");
	}
}
