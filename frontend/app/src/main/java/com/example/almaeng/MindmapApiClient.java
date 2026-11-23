package com.example.almaeng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Backend의 개념지도 API와 통신한다.
 */
public final class MindmapApiClient {

    // Android Emulator에서 개발 PC(Windows/WSL)의 localhost에 접근하는 주소.
    private static final String API_URL =
            "http://10.0.2.2:8080/api/v1/mindmaps";

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private MindmapApiClient() {
    }

    public interface Callback {
        void onSuccess(MindmapData data);

        void onFailure(String message);
    }

    public static void createMindmap(
            String sourceText,
            int depth,
            Callback callback
    ) {
        EXECUTOR.execute(() -> {
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
                connection.setRequestProperty("Accept", "application/json");

                JSONObject request = new JSONObject();
                request.put("sourceText", sourceText);
                request.put("depth", depth);

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(request.toString().getBytes(StandardCharsets.UTF_8));
                }

                int statusCode = connection.getResponseCode();
                InputStream responseStream = statusCode >= 200 && statusCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String responseText = readResponse(responseStream);

                if (statusCode < 200 || statusCode >= 300) {
                    callback.onFailure(extractErrorMessage(responseText, statusCode));
                    return;
                }

                callback.onSuccess(parseResponse(responseText));

            } catch (Exception exception) {
                String detail = exception.getMessage();
                callback.onFailure(
                        "서버 연결 또는 응답 처리에 실패했습니다."
                                + (detail == null || detail.isBlank() ? "" : "\n" + detail)
                );
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static String readResponse(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private static MindmapData parseResponse(String responseText) throws JSONException {
        JSONObject response = new JSONObject(responseText);
        JSONArray nodeArray = response.optJSONArray("nodes");
        JSONArray edgeArray = response.optJSONArray("edges");

        if (nodeArray == null || edgeArray == null || nodeArray.length() == 0) {
            throw new JSONException("서버 응답에 nodes 또는 edges 데이터가 없습니다.");
        }

        List<MindmapData.Node> nodes = new ArrayList<>();
        for (int i = 0; i < nodeArray.length(); i++) {
            JSONObject node = nodeArray.getJSONObject(i);

            long nodeId = node.getLong("nodeId");
            String conceptName = node.optString("conceptName", "").trim();
            String description = node.optString("description", "");
            String learningStatus = node.optString("learningStatus", "UNCHECKED");

            if (conceptName.isEmpty()) {
                throw new JSONException("개념 이름이 비어 있는 노드가 있습니다.");
            }

            nodes.add(new MindmapData.Node(
                    nodeId,
                    conceptName,
                    description,
                    learningStatus
            ));
        }

        List<MindmapData.Edge> edges = new ArrayList<>();
        for (int i = 0; i < edgeArray.length(); i++) {
            JSONObject edge = edgeArray.getJSONObject(i);
            edges.add(new MindmapData.Edge(
                    edge.getLong("edgeId"),
                    edge.getLong("sourceNodeId"),
                    edge.getLong("targetNodeId"),
                    edge.optString("relationshipLabel", "")
            ));
        }

        return new MindmapData(
                response.optString("sourceText", ""),
                response.optInt("depth", nodes.size()),
                response.optString("generationStatus", "UNKNOWN"),
                nodes,
                edges
        );
    }

    private static String extractErrorMessage(String responseText, int statusCode) {
        try {
            JSONObject error = new JSONObject(responseText);
            String message = error.optString("message", "");
            if (!message.isBlank()) {
                return "서버 오류 (HTTP " + statusCode + ")\n" + message;
            }
        } catch (JSONException ignored) {
            // 오류 응답이 JSON이 아닌 경우 기본 메시지를 사용한다.
        }

        return "서버 오류가 발생했습니다. (HTTP " + statusCode + ")";
    }
}
