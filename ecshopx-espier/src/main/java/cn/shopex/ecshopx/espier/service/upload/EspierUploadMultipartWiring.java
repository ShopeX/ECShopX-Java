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

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EspierUploadMultipartWiring {

	@Autowired
	public void wireMultipart(ApplicationContext ctx, MultipartProperties multipartProperties) {
		Map<String, AbstractEspierUploadFileHandler> beans = ctx.getBeansOfType(AbstractEspierUploadFileHandler.class);
		for (AbstractEspierUploadFileHandler h : beans.values()) {
			h.setMultipartProperties(multipartProperties);
		}
	}
}
