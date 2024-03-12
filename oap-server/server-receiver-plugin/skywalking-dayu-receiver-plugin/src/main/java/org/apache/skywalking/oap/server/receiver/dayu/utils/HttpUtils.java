package org.apache.skywalking.oap.server.receiver.dayu.utils;

import com.google.gson.Gson;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpUtils {

    /**
     * http连接池对象
     */
    private static PoolingHttpClientConnectionManager cm;

    static {
        init();
    }

    /**
     * 初始化连接池
     */
    private static void init() {
        //创建http连接池，可以同时指定连接超时时间
        cm = new PoolingHttpClientConnectionManager(60000, TimeUnit.MILLISECONDS);
        //最多同时连接20个请求
        cm.setMaxTotal(20);
        //每个路由最大连接数，路由指IP+PORT或者域名
        cm.setDefaultMaxPerRoute(50);
    }

    /**
     * 从连接池中获取httpClient连接
     */
    private static CloseableHttpClient getHttpClient() {
        if(cm == null){
            throw new RuntimeException("get httpClient fail, PoolingHttpClientConnectionManager is null, please init first");
        }
        //设置请求参数配置，创建连接时间、从连接池获取连接时间、数据传输时间、是否测试连接可用、构建配置对象
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(3000)
                .setSocketTimeout(10 * 1000)
                .build();

        //创建httpClient时从连接池中获取，并设置连接失败时自动重试（也可以自定义重试策略：setRetryHandler()）
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(cm)
                .disableAutomaticRetries()
                .build();
    }

    /**
     * 执行请求
     */
    public static <T> void doPostRequest(String url, T t) {
        CloseableHttpClient httpClient = getHttpClient();
        //创建http请求类型
        HttpPost httpPost = new HttpPost(url);

        CloseableHttpResponse httpResponse = null;
        try {
            //设置发送的数据
            StringEntity s = new StringEntity(new Gson().toJson(t));
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            httpPost.setEntity(s);

            // 返回结果不需要进行处理
            httpResponse = httpClient.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpResponse != null) {
                try {
                    EntityUtils.consume(httpResponse.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
