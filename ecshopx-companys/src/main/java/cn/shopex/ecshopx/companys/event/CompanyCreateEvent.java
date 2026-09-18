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

package cn.shopex.ecshopx.companys.event;

import org.springframework.context.ApplicationEvent;

/**
 * Published after a new company is committed from online open / OAuth registration flows.
 * Optional fields support post-commit notifications; when absent, listeners no-op the same way as empty legacy payloads.
 */
public class CompanyCreateEvent extends ApplicationEvent {

	private final long companyId;
	private final String issueId;
	private final String smsMobile;
	private final String notifyEmail;
	private final long activeAtEpochSeconds;
	private final long expiredAtEpochSeconds;

	public CompanyCreateEvent(Object source, long companyId) {
		this(source, companyId, null, null, null, 0L, 0L);
	}

	public CompanyCreateEvent(
			Object source,
			long companyId,
			String issueId,
			String smsMobile,
			String notifyEmail,
			long activeAtEpochSeconds,
			long expiredAtEpochSeconds) {
		super(source);
		this.companyId = companyId;
		this.issueId = issueId;
		this.smsMobile = smsMobile;
		this.notifyEmail = notifyEmail;
		this.activeAtEpochSeconds = activeAtEpochSeconds;
		this.expiredAtEpochSeconds = expiredAtEpochSeconds;
	}

	public long getCompanyId() {
		return companyId;
	}

	/** Opaque online-open issue id for Prism callback; empty means skip callback (same as missing id upstream). */
	public String getIssueId() {
		return issueId;
	}

	public String getSmsMobile() {
		return smsMobile;
	}

	public String getNotifyEmail() {
		return notifyEmail;
	}

	public long getActiveAtEpochSeconds() {
		return activeAtEpochSeconds;
	}

	public long getExpiredAtEpochSeconds() {
		return expiredAtEpochSeconds;
	}
}
