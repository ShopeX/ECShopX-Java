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

package cn.shopex.ecshopx.common.cron;

/**
 * 上传定时任务侧删除源文件（对象存储/本地盘）的端口；与直接注入 {@code FileStorageService} 解耦，便于 test-cron 下替换为 Noop。
 */
public interface EspierScheduledUploadSourceFileRemover {

	/**
	 * 删除与导入写入时一致的「file」盘相对路径（键）。
	 */
	void removeRelativePath(String relativePath);
}
