package top.bootz.common.utils;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

import net.sf.json.JSONObject;
import top.bootz.common.constant.BaseSecurityConstants;
import top.bootz.common.exception.AppException;

public final class JwtUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtil.class);
	
	/**
	 * jwt签发者（本服务器）
	 */
	public static final String CLAIMS_ISS = "iss";

	/**
	 * token面向的用户（此处用来存储请求方的标识，PC/Android/IOS/H5）
	 */
	public static final String CLAIMS_SUB = "sub";

	/**
	 * 接受jwt的一方（这里存储的是对方的ip地址）
	 */
	public static final String CLAIMS_AUD = "aud";

	/**
	 * token过期时间，必须要大于签发时间
	 */
	public static final String CLAIMS_EXP = "exp";

	/**
	 * 刷新过期时间，在该时间段之内，可以使用旧token换取新token
	 */
	public static final String CLAIMS_REF = "ref";
	
	/**
	 * 定义一个时间，在该时间之前，此token都不可用
	 */
	public static final String CLAIMS_NBF = "nbf";

	/**
	 * token签发的时间
	 */
	public static final String CLAIMS_IAT = "iat";

	/**
	 * jwt的唯一身份标识，主要用来作为一次性token,从而回避重放攻击
	 */
	public static final String CLAIMS_JTI = "jti";

	/**
	 * userid
	 */
	public static final String CLAIMS_USERID = "uid";
	
	/**
	 * 访问用户的username
	 */
	public static final String CLAIMS_USERNAME = "username";

	/**
	 * 访问用户的角色字符串
	 */
	public static final String CLAIMS_ROLES = "roles";

	/**
	 * 客户端的设备类型
	 */
//	public static final String CLAIMS_DEVICE = "device";

	private static RsaSigner signer;

	private static RsaVerifier verifier;

	private JwtUtil() {

	}

	static {
		try {
			signer = new RsaSigner((RSAPrivateKey) RSAUtil.getPrivateKey());
			verifier = new RsaVerifier((RSAPublicKey) RSAUtil.getPublicKey());
		} catch (Exception e) {
			LOGGER.error("init tokenUtil failure!", e);
		}
	}

	/**
	 * 同步加密生成JWT
	 * 
	 * @param payload：JSON字符串格式
	 * @return
	 */
	public static String encode(String payload) {
		Jwt jwt = JwtHelper.encode(payload, signer);
		return jwt.getEncoded();
	}

	/**
	 * 生成JWT
	 * 
	 * @param customPayloads：Map<String,
	 *            String>
	 * @param payload：JSON字符串格式
	 * @return
	 */
	public static String encode(Map<String, Object> customPayloads) {
		Map<String, Object> payloads = buildNormalJwtPayloads();
		payloads.putAll(customPayloads);
		JSONObject jsonObject = JsonUtil.getJSONObject(payloads);
		Jwt jwt = JwtHelper.encode(jsonObject.toString(), signer);
		return jwt.getEncoded();
	}

	/**
	 * 填充通用的payload字段
	 * 
	 * @return
	 */
	public static Map<String, Object> buildNormalJwtPayloads() {
		Map<String, Object> payloads = new HashMap<>();
		long createTime = Instant.now().toEpochMilli();
		payloads.put(CLAIMS_IAT, createTime); // 签发时间
		long expirationTime = createTime + BaseSecurityConstants.JWT_EXPIRATION;
		payloads.put(CLAIMS_EXP, expirationTime); // 过期截止时间
		long refreshEndTime = createTime + BaseSecurityConstants.JWT_REFRESH_END_TIME;
		payloads.put(CLAIMS_REF, refreshEndTime); // 刷新截止时间
		return payloads;
	}

	/**
	 * 检测Token的第三部分签名是否合法（即没有被篡改过）
	 */
	public static boolean verifyJwtToken(String jwtToken) {
		boolean isLegalToken = true;
		try {
			JwtHelper.decodeAndVerify(jwtToken, verifier);
		} catch (Exception e) {
			LOGGER.error("JWT - 签名验证失败！ [" + jwtToken + "]", e);
			isLegalToken = false;
		}
		return isLegalToken;
	}

	/**
	 * 解密Jwt, 提取payload
	 * 
	 * @return
	 */
	public static JSONObject getPayload(String jwtToken) {
		try {
			Jwt jwt = JwtHelper.decodeAndVerify(jwtToken, verifier);
			return JsonUtil.getJSONObject(jwt.getClaims());
		} catch (Exception e) {
			throw new AppException("获取JWT的payload数据失败！ [" + e.getMessage() + "]", e);
		}
	}

	/**
	 * 解密Jwt, 提取header
	 * 
	 * @return
	 */
	public static JSONObject getHeader(String jwtToken) {
		try {
			Jwt jwt = JwtHelper.decodeAndVerify(jwtToken, verifier);
			String header = new String(CodecUtil.fromUrlSafeBase64(jwt.getEncoded().split("\\.")[0]));
			return JsonUtil.getJSONObject(header);
		} catch (Exception e) {
			throw new AppException("获取JWT的header数据失败！ [" + e.getMessage() + "]", e);
		}
	}

	public static String getSignature(String jwtToken) {
		try {
			Jwt jwt = JwtHelper.decodeAndVerify(jwtToken, verifier);
			return new String(CodecUtil.fromUrlSafeBase64(jwt.getEncoded().split("\\.")[2]));
		} catch (Exception e) {
			throw new AppException("获取JWT的signature数据失败！ [" + e.getMessage() + "]", e);
		}
	}

}
