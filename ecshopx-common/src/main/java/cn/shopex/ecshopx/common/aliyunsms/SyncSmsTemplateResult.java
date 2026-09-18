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

package cn.shopex.ecshopx.common.aliyunsms;

import java.util.ArrayList;
import java.util.List;

public class SyncSmsTemplateResult {

	private int created;
	private int updated;
	private int deleted;
	private int skipped;
	private int failed;
	private final List<SyncSmsTemplateError> errors = new ArrayList<>();

	public int created() { return created; }
	public int updated() { return updated; }
	public int deleted() { return deleted; }
	public int skipped() { return skipped; }
	public int failed() { return failed; }
	public List<SyncSmsTemplateError> errors() { return List.copyOf(errors); }
	public void incCreated() { created++; }
	public void incUpdated() { updated++; }
	public void incDeleted() { deleted++; }
	public void incSkipped() { skipped++; }
	public void incFailed() { failed++; }
	public void addError(String templateCode, String message) { errors.add(new SyncSmsTemplateError(templateCode, message)); }

	public record SyncSmsTemplateError(String templateCode, String message) {}
}
