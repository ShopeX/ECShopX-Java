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

package cn.shopex.ecshopx.shuyun.service.openplatform;

/** 单笔线下权益发券结果。对齐 PHP {@code ShuyunOfflineBenefitIssueResult}。 */
public final class OfflineBenefitIssueResult {

	private final boolean success;
	private final String benefitCode;
	private final String failReason;
	private final Long memberUserId;

	private OfflineBenefitIssueResult(
			boolean success, String benefitCode, String failReason, Long memberUserId) {
		this.success = success;
		this.benefitCode = benefitCode;
		this.failReason = failReason;
		this.memberUserId = memberUserId;
	}

	public static OfflineBenefitIssueResult ok(String benefitCode, Long memberUserId) {
		return new OfflineBenefitIssueResult(true, benefitCode, null, memberUserId);
	}

	public static OfflineBenefitIssueResult fail(String failReason) {
		return new OfflineBenefitIssueResult(false, null, failReason, null);
	}

	public boolean isSuccess() {
		return success;
	}

	public String getBenefitCode() {
		return benefitCode;
	}

	public String getFailReason() {
		return failReason;
	}

	public Long getMemberUserId() {
		return memberUserId;
	}
}
