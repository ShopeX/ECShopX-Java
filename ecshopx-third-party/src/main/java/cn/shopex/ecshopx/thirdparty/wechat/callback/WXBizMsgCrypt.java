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

package cn.shopex.ecshopx.thirdparty.wechat.callback;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * 企业微信回调 URL 校验与消息体解密（算法与官方示例一致：SHA1 签名、AES-256-CBC、PKCS#7）。
 */
public class WXBizMsgCrypt {

	public static final int OK = 0;
	public static final int VALIDATE_SIGNATURE_ERROR = -40001;
	public static final int PARSE_XML_ERROR = -40002;
	public static final int COMPUTE_SIGNATURE_ERROR = -40003;
	public static final int ILLEGAL_AES_KEY = -40004;
	public static final int VALIDATE_CORPID_ERROR = -40005;
	public static final int ENCRYPT_AES_ERROR = -40006;
	public static final int DECRYPT_AES_ERROR = -40007;
	public static final int ILLEGAL_BUFFER = -40008;

	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final int BLOCK_SIZE = 32;

	private final String token;
	private final String encodingAesKey;
	private final String receiveId;

	public WXBizMsgCrypt(String token, String encodingAesKey, String receiveId) {
		this.token = token == null ? "" : token;
		this.encodingAesKey = encodingAesKey == null ? "" : encodingAesKey;
		this.receiveId = receiveId == null ? "" : receiveId;
	}

	public int verifyUrl(
			String msgSignature, String timestamp, String nonce, String echoStr, String[] outEcho) {
		if (encodingAesKey.length() != 43) {
			return ILLEGAL_AES_KEY;
		}
		Sha1Result sig = sha1(token, timestamp, nonce, echoStr);
		if (sig.errCode != OK) {
			return sig.errCode;
		}
		if (!safeEquals(sig.digest, msgSignature)) {
			return VALIDATE_SIGNATURE_ERROR;
		}
		DecryptResult dec = prpcryptDecrypt(echoStr == null ? "" : echoStr, receiveId);
		if (dec.errCode != OK) {
			return dec.errCode;
		}
		if (outEcho != null && outEcho.length > 0) {
			outEcho[0] = dec.plainText;
		}
		return OK;
	}

	public int decryptMsg(
			String msgSignature, String timestamp, String nonce, String postData, String[] outMsg) {
		if (encodingAesKey.length() != 43) {
			return ILLEGAL_AES_KEY;
		}
		String encrypt = extractEncrypt(postData);
		if (encrypt == null) {
			return PARSE_XML_ERROR;
		}
		String ts = timestamp;
		if (ts == null) {
			ts = String.valueOf(System.currentTimeMillis() / 1000L);
		}
		Sha1Result sig = sha1(token, ts, nonce, encrypt);
		if (sig.errCode != OK) {
			return sig.errCode;
		}
		if (!safeEquals(sig.digest, msgSignature)) {
			return VALIDATE_SIGNATURE_ERROR;
		}
		DecryptResult dec = prpcryptDecrypt(encrypt, receiveId);
		if (dec.errCode != OK) {
			return dec.errCode;
		}
		if (outMsg != null && outMsg.length > 0) {
			outMsg[0] = dec.plainText;
		}
		return OK;
	}

	private static String extractEncrypt(String xmltext) {
		if (xmltext == null || xmltext.isEmpty()) {
			return null;
		}
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			Document doc = dbf.newDocumentBuilder().parse(new InputSource(new java.io.StringReader(xmltext)));
			NodeList nodes = doc.getElementsByTagName("Encrypt");
			if (nodes.getLength() < 1) {
				return null;
			}
			Node n = nodes.item(0);
			return n.getTextContent();
		} catch (Exception e) {
			return null;
		}
	}

	private static Sha1Result sha1(String token, String timestamp, String nonce, String encryptMsg) {
		try {
			String[] arr = new String[] {
				nvl(encryptMsg), nvl(token), nvl(timestamp), nvl(nonce)
			};
			Arrays.sort(arr, Comparator.comparing(a -> a));
			StringBuilder sb = new StringBuilder();
			for (String s : arr) {
				sb.append(s);
			}
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(sb.toString().getBytes(UTF8));
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				hex.append(String.format("%02x", b));
			}
			return new Sha1Result(OK, hex.toString());
		} catch (NoSuchAlgorithmException e) {
			return new Sha1Result(COMPUTE_SIGNATURE_ERROR, null);
		}
	}

	private static String nvl(String s) {
		return s == null ? "" : s;
	}

	private static boolean safeEquals(String a, String b) {
		if (a == null) {
			return b == null || b.isEmpty();
		}
		return a.equals(b);
	}

	private DecryptResult prpcryptDecrypt(String encrypted, String recvId) {
		try {
			byte[] key = Base64.getDecoder().decode(encodingAesKey + "=");
			byte[] iv = Arrays.copyOfRange(key, 0, 16);
			byte[] cipherBytes = Base64.getDecoder().decode(encrypted);
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			byte[] decrypted = cipher.doFinal(cipherBytes);
			byte[] unpadded = pkcs7Decode(decrypted);
			if (unpadded.length < 16) {
				return new DecryptResult(ILLEGAL_BUFFER, null);
			}
			int xmlLen = ByteBuffer.wrap(unpadded, 16, 4).getInt();
			if (xmlLen < 0 || 16 + 4 + xmlLen > unpadded.length) {
				return new DecryptResult(ILLEGAL_BUFFER, null);
			}
			String xmlContent = new String(unpadded, 20, xmlLen, UTF8);
			int tailLen = unpadded.length - 20 - xmlLen;
			String fromRecv = new String(unpadded, 20 + xmlLen, tailLen, UTF8);
			if (!recvId.equals(fromRecv)) {
				return new DecryptResult(VALIDATE_CORPID_ERROR, null);
			}
			return new DecryptResult(OK, xmlContent);
		} catch (Exception e) {
			return new DecryptResult(DECRYPT_AES_ERROR, null);
		}
	}

	private static byte[] pkcs7Decode(byte[] decrypted) {
		if (decrypted == null || decrypted.length == 0) {
			return decrypted;
		}
		int pad = decrypted[decrypted.length - 1] & 0xff;
		if (pad < 1 || pad > BLOCK_SIZE) {
			pad = 0;
		}
		if (pad == 0) {
			return decrypted;
		}
		return Arrays.copyOf(decrypted, decrypted.length - pad);
	}

	private record Sha1Result(int errCode, String digest) {}

	private record DecryptResult(int errCode, String plainText) {}
}
