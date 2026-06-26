package com.lcyl.code.constant;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcyl.common.exception.base.BaseException;
import com.lcyl.common.utils.http.HttpUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName LoginConstant
 * @Description TODO
 * @Author GuiGui
 * @Date 2026-03-24 14:49
 * @Version 1.0
 */
public class LoginConstant {

    // 使用静态共享 HttpClient（线程安全），避免每次 new LoginConstant() 创建新连接导致资源泄露
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final String LOGIN_APPID = System.getenv("WX_APPID");
    public static final String LOGIN_SECRET = System.getenv("WX_SECRET");
    public static final String LOGIN_GRANT_TYPE = "authorization_code";
    public static final String LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session?appid=" + LOGIN_APPID + "&secret=" + LOGIN_SECRET + "&js_code=";
    public static final String LOGIN_PHONE_URL ="https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=";
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&";

    public String getOpenIdURL(String code){
        return LOGIN_URL + code + "&grant_type=" + LOGIN_GRANT_TYPE;
    }

    public String getLoginPhoneUrl(String token){
        return LOGIN_PHONE_URL + token ;
    }

    public String getTokenUrl() {
        return TOKEN_URL + "appid=" + LOGIN_APPID + "&secret=" + LOGIN_SECRET;
    }
    public String getOpenId(String url){
        HttpGet httpGet = new HttpGet(url);

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(httpGet)) {
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "UTF-8");
            JsonNode jsonNode = objectMapper.readTree(result);

            // 检查微信是否返回错误码
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                String errMsg = jsonNode.get("errmsg").asText();
                throw new RuntimeException("获取 openid 失败，微信返回错误：" + errMsg);
            }

            JsonNode openidNode = jsonNode.get("openid");
            if (openidNode == null || openidNode.asText().isEmpty()) {
                throw new RuntimeException("获取 openid 失败，返回数据中没有 openid 字段");
            }

            String openid = openidNode.asText();
            return openid;
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAccessToken(String tokenURL){
        String result = HttpUtils.sendGet(tokenURL);
        JSONObject jsonObject = JSONObject.parseObject(result);
        if (jsonObject.containsKey("errcode") && jsonObject.getInteger("errcode") != 0) {
            throw new BaseException("获取 access_token 失败：" + jsonObject.getString("errmsg"));
        }
        Object token = jsonObject.get("access_token");
        if (token == null) {
            throw new BaseException("获取 access_token 失败：返回数据中无 access_token 字段");
        }
        return token.toString();
    }

    public String getPhone(String phoneUrl,String phoneCode){
        Map<String, Object> param = new HashMap<>();
        param.put("code", phoneCode);

        String result = HttpUtils.sendPost(phoneUrl, JSONObject.toJSONString(param));
        JSONObject jsonObject = JSONObject.parseObject(result);
        // 如果code不正确，则失败
        Integer errcode = jsonObject.getInteger("errcode");
        if (errcode != null && errcode != 0) {
            throw new BaseException("获取手机号失败：" + jsonObject.getString("errmsg"));
        }
        JSONObject phoneInfo = jsonObject.getJSONObject("phone_info");
        if (phoneInfo == null) {
            throw new BaseException("获取手机号失败：响应中无 phone_info 字段");
        }
        return phoneInfo.getString("purePhoneNumber");
    }

}
