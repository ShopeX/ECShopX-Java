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

package cn.shopex.ecshopx.chinaumspay.transfer;

import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteDownloadPort;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 从 UMS SFTP 拉取 {@code final_*.ret} 到本地 storage 根下，与 PHP {@code downloadFile} 路径同口径。
 */
@Service
@Profile("!test-cron")
public class ChinaumsDivisionSftpRemoteDownloadService implements ChinaumsDivisionRemoteDownloadPort {

	private static final String PREFIX = "final_";
	private static final String SUFFIX = ".ret";

	@Value("${ecshopx.ums.sftp.host:}")
	private String sftpHost;

	@Value("${ecshopx.ums.sftp.port:22}")
	private int sftpPort;

	@Value("${ecshopx.ums.sftp.user:}")
	private String sftpUser;

	@Value("${ecshopx.ums.sftp.password:}")
	private String sftpPassword;

	private final Path storageRoot;

	public ChinaumsDivisionSftpRemoteDownloadService(
			@Value("${ecshopx.storage.local.root:storage/app/public}") String storageLocalRoot) {
		this.storageRoot = Path.of(storageLocalRoot).toAbsolutePath().normalize();
	}

	@Override
	public void downloadFinalRetToStorage(
			long companyId, String localDirRelative, String remoteDir, String dataFileName) throws IOException {
		ensureSftp();
		String file = PREFIX + dataFileName + SUFFIX;
		String localRel = joinLocal(localDirRelative, file);
		Path local = resolveUnderStorage(localRel);
		Files.createDirectories(local.getParent());
		String remotePath = joinRemote(remoteDir, file);
		sftpGet(local, remotePath);
	}

	private void ensureSftp() {
		if (!StringUtils.hasText(sftpHost) || !StringUtils.hasText(sftpUser)) {
			throw new IllegalStateException("ecshopx.ums.sftp 未配置");
		}
	}

	private Path resolveUnderStorage(String relativePathFromStorageRoot) {
		if (!StringUtils.hasText(relativePathFromStorageRoot)) {
			throw new IllegalStateException("invalid path");
		}
		String norm = relativePathFromStorageRoot.replace("\\", "/");
		if (norm.startsWith("/")) {
			norm = norm.substring(1);
		}
		Path t = storageRoot.resolve(norm).normalize();
		if (!t.startsWith(storageRoot)) {
			throw new IllegalStateException("invalid path under storage");
		}
		return t;
	}

	private void sftpGet(Path localFile, String remotePath) throws IOException {
		JSch jsch = new JSch();
		Session session = null;
		ChannelSftp ch = null;
		try {
			session = jsch.getSession(sftpUser, sftpHost, sftpPort);
			session.setPassword(sftpPassword);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30_000);
			ch = (ChannelSftp) session.openChannel("sftp");
			ch.connect(30_000);
			try (var out = Files.newOutputStream(localFile)) {
				ch.get(remotePath, out);
			}
		} catch (Exception e) {
			throw new IOException("sftp get: " + remotePath + " -> " + localFile, e);
		} finally {
			if (ch != null) {
				ch.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}
	}

	private static String joinLocal(String localDir, String fileName) {
		String a = localDir == null ? "" : localDir.replace("\\", "/");
		if (a.isEmpty()) {
			return fileName;
		}
		if (!a.endsWith("/")) {
			a = a + "/";
		}
		return a + fileName;
	}

	private static String joinRemote(String remoteDir, String fileName) {
		String a = remoteDir == null ? "" : remoteDir.replace("\\", "/");
		if (a.isEmpty()) {
			return fileName.startsWith("/") ? fileName : "/" + fileName;
		}
		if (!a.startsWith("/")) {
			a = "/" + a;
		}
		if (!a.endsWith("/")) {
			a = a + "/";
		}
		return a + fileName;
	}
}
