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

    private static PoolingHttpClientConnectionManager POOLING_HTTP_CLIENT_CONNECTION_MANAGER;

    static {
        init();
    }

    private static void init() {
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER = new PoolingHttpClientConnectionManager(60000, TimeUnit.MILLISECONDS);
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setMaxTotal(20);
        POOLING_HTTP_CLIENT_CONNECTION_MANAGER.setDefaultMaxPerRoute(50);
    }

    private static CloseableHttpClient getHttpClient() {
        if (POOLING_HTTP_CLIENT_CONNECTION_MANAGER == null) {
            throw new RuntimeException("get httpClient fail, PoolingHttpClientConnectionManager is null, please init first");
        }
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(3000)
                .setSocketTimeout(10 * 1000)
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(POOLING_HTTP_CLIENT_CONNECTION_MANAGER)
                .disableAutomaticRetries()
                .build();
    }

    public static <T> void doPostRequest(String url, T t) {
        CloseableHttpClient httpClient = getHttpClient();
        HttpPost httpPost = new HttpPost(url);

        CloseableHttpResponse httpResponse = null;
        try {
            StringEntity s = new StringEntity(new Gson().toJson(t));
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            httpPost.setEntity(s);

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
