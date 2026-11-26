package org.examplexxx.trainlog.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.examplexxx.trainlog.http.HttpException;
import org.examplexxx.trainlog.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp 封装工具：只负责"发请求、收响应"，不做业务。
 *
 * <p>响应模型在 {@code org.examplexxx.trainlog.http} 包：
 * {@link HttpResponse}、{@link HttpException}。
 *
 * <p>用法：
 * <pre>{@code
 * HttpResponse resp = OkHttpUtil.get("https://api.example.com/weather",
 *         Map.of("city", "beijing"), Map.of("Authorization", "Bearer xxx"));
 * WeatherVO weather = resp.as(WeatherVO.class);
 *
 * OkHttpUtil.postJson("https://api.example.com/users", Map.of("name", "tom"));
 * }</pre>
 */
public final class OkHttpUtil {

    private static final Logger log = LoggerFactory.getLogger(OkHttpUtil.class);

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(15))
            .writeTimeout(Duration.ofSeconds(15))
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .addInterceptor(new LoggingInterceptor())
            .build();

    private OkHttpUtil() {
    }

    /** 需要自定义 Call、WebSocket 等场景时拿到底层 client */
    public static OkHttpClient client() {
        return CLIENT;
    }

    // ==================== GET ====================

    public static HttpResponse get(String url) {
        return get(url, null, null);
    }

    public static HttpResponse get(String url, Map<String, String> headers) {
        return get(url, null, headers);
    }

    /**
     * @param query 拼到 URL 上的查询参数，value 为 null 时跳过
     */
    public static HttpResponse get(String url, Map<String, String> query, Map<String, String> headers) {
        HttpUrl.Builder urlBuilder = HttpUrl.get(url).newBuilder();
        if (query != null) {
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (entry.getValue() != null) {
                    urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        Request.Builder builder = new Request.Builder().url(urlBuilder.build()).get();
        applyHeaders(builder, headers);
        return execute(builder.build());
    }

    // ==================== POST JSON ====================

    public static HttpResponse postJson(String url, Object body) {
        return postJson(url, body, null);
    }

    public static HttpResponse postJson(String url, Object body, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(toJson(body), JSON));
        applyHeaders(builder, headers);
        return execute(builder.build());
    }

    /** POST JSON 并反序列化成对象，非 2xx 抛 {@link HttpException} */
    public static <T> T postJson(String url, Object body, Map<String, String> headers, Class<T> responseType) {
        return postJson(url, body, headers).as(responseType);
    }

    // ==================== POST 表单 ====================

    public static HttpResponse postForm(String url, Map<String, String> form) {
        return postForm(url, form, null);
    }

    public static HttpResponse postForm(String url, Map<String, String> form, Map<String, String> headers) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (form != null) {
            for (Map.Entry<String, String> entry : form.entrySet()) {
                if (entry.getValue() != null) {
                    formBuilder.add(entry.getKey(), entry.getValue());
                }
            }
        }
        Request.Builder builder = new Request.Builder().url(url).post(formBuilder.build());
        applyHeaders(builder, headers);
        return execute(builder.build());
    }

    // ==================== PUT / DELETE ====================

    public static HttpResponse putJson(String url, Object body) {
        return putJson(url, body, null);
    }

    public static HttpResponse putJson(String url, Object body, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .put(RequestBody.create(toJson(body), JSON));
        applyHeaders(builder, headers);
        return execute(builder.build());
    }

    public static HttpResponse delete(String url) {
        return delete(url, null);
    }

    public static HttpResponse delete(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url).delete();
        applyHeaders(builder, headers);
        return execute(builder.build());
    }

    // ==================== 通用执行 ====================

    /**
     * 执行任意 Request。非 2xx 不抛异常，返回结果里带状态码和响应体。
     */
    public static HttpResponse execute(Request request) {
        try (Response response = CLIENT.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String body = responseBody == null ? "" : responseBody.string();
            return new HttpResponse(response.code(), response.message(), body, response.headers());
        } catch (IOException e) {
            throw new HttpException(0, "请求失败：" + request.method() + " " + request.url(), e);
        }
    }

    // ==================== 内部工具 ====================

    private static void applyHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name != null && value != null) {
                    builder.header(name, value);
                }
            });
        }
    }

    private static String toJson(Object body) {
        if (body == null) {
            return "";
        }
        if (body instanceof String text) {
            return text;
        }
        try {
            return MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new HttpException(0, "JSON 序列化失败", e);
        }
    }

    /**
     * 只记录方法、URL、状态码和耗时，不打请求体/响应体，避免日志里出现密码和 token。
     */
    private static final class LoggingInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long start = System.nanoTime();
            try {
                Response response = chain.proceed(request);
                long costMs = (System.nanoTime() - start) / 1_000_000;
                log.debug("HTTP {} {} -> {} ({} ms)",
                        request.method(), request.url(), response.code(), costMs);
                return response;
            } catch (IOException e) {
                long costMs = (System.nanoTime() - start) / 1_000_000;
                log.warn("HTTP {} {} 失败 ({} ms): {}",
                        request.method(), request.url(), costMs, e.getMessage());
                throw e;
            }
        }
    }
}
