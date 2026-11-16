package com.example.almaeng;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MindmapApiClient {

    // Android Emulator에서 PC/WSL의 localhost 서버에 접속하는 주소
    private static final String API_URL =
            "http://10.0.2.2:8080/api/v1/mindmaps";

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public interface Callback {
        void onSuccess(JSONObject response);
        void onFailure(String message);
    }

    public static void createMindmap(
            String sourceText,
            int depth,
            Callback callback
    ) {
        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                JSONObject request = new JSONObject();
                request.put("sourceText", sourceText);
                request.put("depth", depth);

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(
                            request.toString().getBytes(StandardCharsets.UTF_8)
                    );
                }

                int statusCode = connection.getResponseCode();

                InputStream input = statusCode >= 200 && statusCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder responseText = new StringBuilder();

                if (input != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(input, StandardCharsets.UTF_8)
                    )) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            responseText.append(line);
                        }
                    }
                }

                JSONObject response = new JSONObject(responseText.toString());

                if (statusCode >= 200 && statusCode < 300) {
                    callback.onSuccess(response);
                } else {
                    callback.onFailure(
                            response.optString(
                                    "message",
                                    "서버 오류가 발생했습니다. (HTTP " + statusCode + ")"
                            )
                    );
                }

            } catch (Exception exception) {
                callback.onFailure(
                        "서버 연결에 실패했습니다: " + exception.getMessage()
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
}
