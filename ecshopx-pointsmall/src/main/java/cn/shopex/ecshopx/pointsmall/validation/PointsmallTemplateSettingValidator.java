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

package cn.shopex.ecshopx.pointsmall.validation;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PointsmallTemplateSettingValidator {

	public void validate(Map<String, Object> data) {
		Object bannerObj = data.get("pc_banner");
		if (bannerObj instanceof Collection<?> c && !c.isEmpty() && c.size() > 4) {
			throw new ResourceException("PC端Banner轮播图最多可设置4张");
		}

		Object screenObj = data.get("screen");
		if (!(screenObj instanceof Map<?, ?> screen)) {
			return;
		}

		Object pointOpen = screen.get("point_openstatus");
		if (!Boolean.TRUE.equals(pointOpen)) {
			return;
		}

		Object ps = screen.get("point_section");
		if (!(ps instanceof Collection<?> pointSections)
				|| pointSections.size() < 1) {
			throw new ResourceException("积分区间最少设置1组");
		}
		if (pointSections.size() > 5) {
			throw new ResourceException("积分区间最多可设置5组");
		}

		for (Object section : pointSections) {
			Object low;
			Object high;
			if (section instanceof List<?> l) {
				if (!listSlotIsset(l, 0) || !listSlotIsset(l, 1)) {
					throw new ResourceException("请设置完整的积分区间");
				}
				low = l.get(0);
				high = l.get(1);
			} else if (section instanceof Map<?, ?> m) {
				if (!mapSlotIsset(m, 0) || !mapSlotIsset(m, 1)) {
					throw new ResourceException("请设置完整的积分区间");
				}
				low = mapSlotGet(m, 0);
				high = mapSlotGet(m, 1);
			} else {
				throw new ResourceException("请设置完整的积分区间");
			}
			if (Objects.toString(low, "").trim().isEmpty()
					|| Objects.toString(high, "").trim().isEmpty()) {
				throw new ResourceException("请设置完整的积分区间");
			}
		}
	}

	private static boolean listSlotIsset(List<?> l, int index) {
		if (l.size() <= index) {
			return false;
		}
		return l.get(index) != null;
	}

	private static boolean mapSlotIsset(Map<?, ?> m, int index) {
		if (!mapHasSlotKey(m, index)) {
			return false;
		}
		return mapSlotGet(m, index) != null;
	}

	private static boolean mapHasSlotKey(Map<?, ?> m, int index) {
		return m.containsKey(index) || m.containsKey(String.valueOf(index));
	}

	private static Object mapSlotGet(Map<?, ?> m, int index) {
		if (m.containsKey(index)) {
			return m.get(index);
		}
		if (m.containsKey(String.valueOf(index))) {
			return m.get(String.valueOf(index));
		}
		return null;
	}
}
