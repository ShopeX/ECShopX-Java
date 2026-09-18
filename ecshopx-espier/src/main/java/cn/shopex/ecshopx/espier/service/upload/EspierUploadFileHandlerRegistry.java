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

package cn.shopex.ecshopx.espier.service.upload;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EspierUploadFileHandlerRegistry {

	private final List<EspierUploadFileHandler> handlers;
	private Map<String, EspierUploadFileHandler> map;

	public EspierUploadFileHandlerRegistry(List<EspierUploadFileHandler> handlers) {
		this.handlers = handlers;
	}

	@PostConstruct
	public void init() {
		map = new HashMap<>();
		for (EspierUploadFileHandler h : handlers) {
			String t = h.supportedFileType();
			if (map.put(t, h) != null) {
				throw new IllegalStateException("duplicate file_type: " + t);
			}
		}
	}

	public EspierUploadFileHandler requireHandler(String fileType) {
		return map.get(fileType);
	}
}
