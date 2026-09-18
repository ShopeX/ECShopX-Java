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

import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionLocalArtifactWriterPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRemoteUploadPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsPaymentSettingLoadPort;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Formatter;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 生产环境：签名 .chk 与数据文件经 SFTP 上送。未配置 {@code ecshopx.ums.sftp.*} 时调用失败，便于与
 * 运维联调；test-cron 下为 Noop。
 */
@Service
@Profile("!test-cron")
public class ChinaumsDivisionSftpRemoteUploadService implements ChinaumsDivisionRemoteUploadPort {

	private final Path storageRoot;
	private final ChinaumsPaymentSettingLoadPort paymentSettingLoadPort;
	private final ChinaumsDivisionLocalArtifactWriterPort localWriter;

	@Value("${ecshopx.ums.sftp.host:}")
	private String sftpHost;

	@Value("${ecshopx.ums.sftp.port:22}")
	private int sftpPort;

	@Value("${ecshopx.ums.sftp.user:}")
	private String sftpUser;

	@Value("${ecshopx.ums.sftp.password:}")
	private String sftpPassword;

	public ChinaumsDivisionSftpRemoteUploadService(
			@Value("${ecshopx.storage.local.root:storage/app/public}") String storageLocalRoot,
			ChinaumsPaymentSettingLoadPort paymentSettingLoadPort,
			ChinaumsDivisionLocalArtifactWriterPort localWriter) {
		this.storageRoot = Path.of(storageLocalRoot).toAbsolutePath().normalize();
		this.paymentSettingLoadPort = paymentSettingLoadPort;
		this.localWriter = localWriter;
	}

	@Override
	public void uploadData(long companyId, String localRelativeToStorageRoot, String remoteDir, String fileName) {
		ensureSftp();
		Path local = resolveLocalFile(localRelativeToStorageRoot, fileName);
		String remotePath = joinRemote(remoteDir, fileName);
		ensureParentRemote(remotePath);
		sftpUpload(local, remotePath);
	}

	@Override
	public void uploadSign(long companyId, String dataFileRelativeToStorageRoot, String remoteDir, String dataFileName) {
		ensureSftp();
		Map<String, Object> pay = paymentSettingLoadPort.load(companyId, "");
		String pfxPath = str(pay.get("rsa_private_path"));
		String pfxPassword = str(pay.get("password"));
		if (!StringUtils.hasText(pfxPath) || !Files.isRegularFile(Path.of(pfxPath))) {
			throw new IllegalStateException("未配置私钥文件");
		}
		Path dataFile = resolveLocalFile(dataFileRelativeToStorageRoot, dataFileName);
		String md5Hex = md5FileHex(dataFile);
		byte[] message = md5Hex.getBytes(StandardCharsets.UTF_8);
		byte[] binarySig = signSha256RsaPfx(pfxPath, pfxPassword, message);
		String hexSig = HexFormat.of().formatHex(binarySig);
		String dataRel = fullRel(dataFileRelativeToStorageRoot, dataFileName);
		String parent = parentPath(dataRel);
		String chkRel = parent + dataFileName + ".chk";
		localWriter.put(chkRel, hexSig);
		Path localChk = storageRoot.resolve(chkRel).normalize();
		if (!localChk.startsWith(storageRoot)) {
			throw new IllegalStateException("invalid chk path");
		}
		String remoteChk = joinRemote(remoteDir, dataFileName) + ".chk";
		ensureParentRemote(remoteChk);
		sftpUpload(localChk, remoteChk);
	}

	private Path resolveLocalFile(String fileRel, String name) {
		String rel = normRel(fileRel);
		if (StringUtils.hasText(name) && !rel.isEmpty() && !rel.endsWith(name)) {
			rel = rel + (rel.endsWith("/") ? "" : "/") + name;
		} else if (rel.isEmpty() && StringUtils.hasText(name)) {
			rel = name;
		}
		Path p = storageRoot.resolve(rel).normalize();
		if (!p.startsWith(storageRoot)) {
			throw new IllegalStateException("path escapes storage root: " + rel);
		}
		return p;
	}

	private static String fullRel(String fileRel, String name) {
		String rel = normRel(fileRel);
		if (StringUtils.hasText(name) && !rel.endsWith(name)) {
			rel = rel + (rel.endsWith("/") ? "" : "/") + name;
		} else if (rel.isEmpty() && StringUtils.hasText(name)) {
			rel = name;
		}
		return rel;
	}

	private static String parentPath(String fileRel) {
		int i = fileRel.lastIndexOf('/');
		if (i < 0) {
			return "";
		}
		return fileRel.substring(0, i + 1);
	}

	private void ensureSftp() {
		if (!StringUtils.hasText(sftpHost) || !StringUtils.hasText(sftpUser)) {
			throw new IllegalStateException("ecshopx.ums.sftp 未配置");
		}
	}

	private void ensureParentRemote(String remoteFilePath) {
		int i = remoteFilePath.lastIndexOf('/');
		if (i <= 0) {
			return;
		}
		String dir = remoteFilePath.substring(0, i);
		mkdirSftp(dir);
	}

	private void mkdirSftp(String dir) {
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
			String[] parts = dir.split("/");
			var b = new StringBuilder();
			for (String p : parts) {
				if (p.isEmpty()) {
					continue;
				}
				b.append("/").append(p);
				try {
					ch.mkdir(b.toString());
				} catch (SftpException e) {
					// 已存在
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("sftp mkdir: " + dir, e);
		} finally {
			if (ch != null) {
				ch.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}
	}

	private void sftpUpload(Path local, String remotePath) {
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
			try (InputStream in = Files.newInputStream(local)) {
				ch.put(in, remotePath);
			}
		} catch (Exception e) {
			throw new IllegalStateException("sftp put: " + local + " -> " + remotePath, e);
		} finally {
			if (ch != null) {
				ch.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}
	}

	private static String joinRemote(String remoteDir, String fileName) {
		String a = ch(remoteDir);
		if (a.isEmpty()) {
			return fileName;
		}
		if (!a.endsWith("/")) {
			a = a + "/";
		}
		return a + fileName;
	}

	private static String ch(String remoteDir) {
		return remoteDir == null ? "" : remoteDir.replace("\\", "/");
	}

	private static String normRel(String s) {
		if (s == null) {
			return "";
		}
		String n = s.replace("\\", "/");
		if (n.startsWith("/")) {
			return n.substring(1);
		}
		return n;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static String md5FileHex(Path file) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] buf = new byte[8192];
			try (var in = Files.newInputStream(file)) {
				int n;
				while ((n = in.read(buf)) >= 0) {
					md.update(buf, 0, n);
				}
			}
			try (var fmt = new Formatter()) {
				for (byte b : md.digest()) {
					fmt.format("%02x", b);
				}
				return fmt.toString();
			}
		} catch (Exception e) {
			throw new IllegalStateException("md5: " + file, e);
		}
	}

	private static byte[] signSha256RsaPfx(String pfxPath, String pfxPassword, byte[] message) {
		try {
			KeyStore ks = KeyStore.getInstance("PKCS12");
			char[] pw = pfxPassword == null ? new char[0] : pfxPassword.toCharArray();
			try (var in = Files.newInputStream(Path.of(pfxPath))) {
				ks.load(in, pw);
			}
			String alias = ks.aliases().nextElement();
			PrivateKey pk = (PrivateKey) ks.getKey(alias, pw);
			Signature sig = Signature.getInstance("SHA256withRSA");
			sig.initSign(pk, new SecureRandom());
			sig.update(message);
			return sig.sign();
		} catch (Exception e) {
			throw new IllegalStateException("sign", e);
		}
	}
}
