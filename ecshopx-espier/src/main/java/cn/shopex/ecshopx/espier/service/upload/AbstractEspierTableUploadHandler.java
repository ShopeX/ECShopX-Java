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
import java.util.function.Consumer;
import org.springframework.web.multipart.MultipartFile;

public abstract class AbstractEspierTableUploadHandler extends AbstractEspierUploadFileHandler implements EspierUploadTableHandler {

	private final UploadHeaderTitle headerTitle;
	private final Consumer<MultipartFile> extraCheck;

	protected AbstractEspierTableUploadHandler(UploadHeaderTitle headerTitle) {
		this(headerTitle, null);
	}

	protected AbstractEspierTableUploadHandler(UploadHeaderTitle headerTitle, Consumer<MultipartFile> extraCheck) {
		this.headerTitle = headerTitle;
		this.extraCheck = extraCheck;
	}

	@Override
	public void check(MultipartFile file) {
		assertBaseUploadConstraints(file);
		if (extraCheck != null) {
			extraCheck.accept(file);
		} else {
			checkDefaultExtension(file);
		}
	}

	@Override
	public UploadHeaderTitle getHeaderTitle(long companyId) {
		return headerTitle;
	}

	@Override
	public final void handleRow(EspierUploadRowContext ctx, Map<String, Object> row) {
		handleBusinessRow(ctx, row);
	}

	protected abstract void handleBusinessRow(EspierUploadRowContext ctx, Map<String, Object> row);
}
