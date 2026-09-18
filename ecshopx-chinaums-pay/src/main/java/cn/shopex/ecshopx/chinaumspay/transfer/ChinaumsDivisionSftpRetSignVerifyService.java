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

import cn.shopex.ecshopx.chinaumspay.port.ChinaumsDivisionRetSignVerifyPort;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsPaymentSettingLoadPort;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Formatter;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 与 PHP {@code DivisionSignService::verifySignFile} 一致：拉取 {@code .chk} 并对 {@code .ret} 的 MD5 做 RSA
 * SHA256 验签。
 */
@Service
@Profile("!test-cron")
public class ChinaumsDivisionSftpRetSignVerifyService implements ChinaumsDivisionRetSignVerifyPort {

	@Value("${ecshopx.ums.sftp.host:}")
	private String sftpHost;

	@Value("${ecshopx.ums.sftp.port:22}")
	private int sftpPort;

	@Value("${ecshopx.ums.sftp.user:}")
	private String sftpUser;

	@Value("${ecshopx.ums.sftp.password:}")
	private String sftpPassword;

	private final Path storageRoot;
	private final ChinaumsPaymentSettingLoadPort paymentSettingLoadPort;

	public ChinaumsDivisionSftpRetSignVerifyService(
			@Value("${ecshopx.storage.local.root:storage/app/public}") String storageLocalRoot,
			ChinaumsPaymentSettingLoadPort paymentSettingLoadPort) {
		this.storageRoot = Path.of(storageLocalRoot).toAbsolutePath().normalize();
		this.paymentSettingLoadPort = paymentSettingLoadPort;
	}

	@Override
	public void verifyDataFileSign(
			long companyId, String localRetFileRelative, String remoteDir, String dataFileNameBase) throws IOException {
		ensureSftp();
		Path ret = resolveLocal(localRetFileRelative);
		String md5Hex = md5FileHex(ret);
		Path chkLocal = Path.of(ret.toString() + ".chk");
		String fileBase = "final_" + dataFileNameBase + ".ret";
		String remoteChk = joinRemote(remoteDir, fileBase + ".chk");
		sftpGet(chkLocal, remoteChk);
		String chkText = Files.readString(chkLocal, StandardCharsets.UTF_8);
		chkText = chkText.replace("\r\n", "").replace("\n", "");
		Map<String, Object> pay = paymentSettingLoadPort.load(companyId, "");
		String publicPath = str(pay.get("rsa_public_path"));
		if (!StringUtils.hasText(publicPath) || !Files.isRegularFile(Path.of(publicPath))) {
			throw new IllegalStateException("未配置公钥文件");
		}
		byte[] der = Files.readAllBytes(Path.of(publicPath));
		PublicKey pub = loadPublicKey(der);
		byte[] sig = HexFormat.of().parseHex(chkText);
		try {
			Signature ver = Signature.getInstance("SHA256withRSA");
			ver.initVerify(pub);
			ver.update(md5Hex.getBytes(StandardCharsets.UTF_8));
			if (!ver.verify(sig)) {
				throw new IllegalStateException("签名验证失败");
			}
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("签名验证失败", e);
		}
	}

	private void ensureSftp() {
		if (!StringUtils.hasText(sftpHost) || !StringUtils.hasText(sftpUser)) {
			throw new IllegalStateException("ecshopx.ums.sftp 未配置");
		}
	}

	private Path resolveLocal(String localRetFileRelative) {
		String n = localRetFileRelative == null ? "" : localRetFileRelative.replace("\\", "/");
		if (n.startsWith("/")) {
			n = n.substring(1);
		}
		Path t = storageRoot.resolve(n).normalize();
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
			Files.createDirectories(localFile.getParent());
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

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static String md5FileHex(Path file) {
		try {
			var md = java.security.MessageDigest.getInstance("MD5");
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

	private static PublicKey loadPublicKey(byte[] fileBytes) {
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509Certificate c =
					(X509Certificate) cf.generateCertificate(new ByteArrayInputStream(fileBytes));
			return c.getPublicKey();
		} catch (Exception e) {
			throw new IllegalStateException("公钥文件读取错误", e);
		}
	}
}
