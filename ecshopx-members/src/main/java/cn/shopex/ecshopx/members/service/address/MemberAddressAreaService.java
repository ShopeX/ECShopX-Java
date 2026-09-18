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

package cn.shopex.ecshopx.members.service.address;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class MemberAddressAreaService {

	private static final Logger log = LoggerFactory.getLogger(MemberAddressAreaService.class);

	private final ObjectMapper objectMapper;

	public MemberAddressAreaService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> getAddressArea() {
		ClassPathResource resource = new ClassPathResource("static/district.json");
		if (!resource.exists()) {
			return Collections.emptyList();
		}
		try (InputStream in = resource.getInputStream()) {
			return objectMapper.readValue(in, new TypeReference<List<Map<String, Object>>>() {});
		} catch (IOException | RuntimeException e) {
			log.warn("Failed to read or parse district.json: {}", e.getMessage());
			return Collections.emptyList();
		}
	}
}
