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

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.dispatch.ExportFileJobTypeHandler;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EspierExportFileJobDispatchHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(EspierExportFileJobDispatchHandler.class);

	private final Map<String, ExportFileJobTypeHandler> delegatesByType;

	public EspierExportFileJobDispatchHandler(List<ExportFileJobTypeHandler> delegates) {
		Map<String, ExportFileJobTypeHandler> map = new LinkedHashMap<>();
		for (ExportFileJobTypeHandler delegate : delegates) {
			String type = delegate.exportType();
			if (map.containsKey(type)) {
				throw new IllegalStateException("duplicate export type handler: " + type);
			}
			map.put(type, delegate);
		}
		this.delegatesByType = Map.copyOf(map);
	}

	@Override
	public void handle(Map<String, Object> payload) {
		Object rawType = payload.get("type");
		if (rawType == null) {
			log.warn("export file job payload missing type");
			return;
		}
		String type = String.valueOf(rawType);
		ExportFileJobTypeHandler delegate = delegatesByType.get(type);
		if (delegate == null) {
			log.warn("export file job unknown type: {}", type);
			return;
		}
		delegate.handle(payload);
	}
}
