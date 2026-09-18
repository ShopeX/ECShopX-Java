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

package cn.shopex.ecshopx.wechat.api.admin.v1.dto;

public class WxappTemplateSetDomainRequest {

	private Domain domain;

	public Domain getDomain() {
		return domain;
	}

	public void setDomain(Domain domain) {
		this.domain = domain;
	}

	public static class Domain {

		private String requestdomain;
		private String wsrequestdomain;
		private String uploaddomain;
		private String downloaddomain;
		private String webviewdomain;

		public String getRequestdomain() {
			return requestdomain;
		}

		public void setRequestdomain(String requestdomain) {
			this.requestdomain = requestdomain;
		}

		public String getWsrequestdomain() {
			return wsrequestdomain;
		}

		public void setWsrequestdomain(String wsrequestdomain) {
			this.wsrequestdomain = wsrequestdomain;
		}

		public String getUploaddomain() {
			return uploaddomain;
		}

		public void setUploaddomain(String uploaddomain) {
			this.uploaddomain = uploaddomain;
		}

		public String getDownloaddomain() {
			return downloaddomain;
		}

		public void setDownloaddomain(String downloaddomain) {
			this.downloaddomain = downloaddomain;
		}

		public String getWebviewdomain() {
			return webviewdomain;
		}

		public void setWebviewdomain(String webviewdomain) {
			this.webviewdomain = webviewdomain;
		}
	}
}
