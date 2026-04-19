package co.edu.escuelaing;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class PostsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String TABLE = System.getenv("POSTS_TABLE");
    private static final String AUTH0_DOMAIN = System.getenv("AUTH0_DOMAIN");
    private static final String AUTH0_AUDIENCE = System.getenv("AUTH0_AUDIENCE");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        String method = event.getHttpMethod();
        Map<String, String> headers = corsHeaders();

        try {
            if ("GET".equals(method)) {
                return handleGet(headers);
            } else if ("POST".equals(method)) {
                return handlePost(event, headers);
            } else if ("OPTIONS".equals(method)) {
                return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody("");
            }
            return error(405, "Method not allowed", headers);
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return error(500, e.getMessage(), headers);
        }
    }

    private APIGatewayProxyResponseEvent handleGet(Map<String, String> headers) throws Exception {
        ScanRequest scan = ScanRequest.builder().tableName(TABLE).build();
        ScanResponse resp = dynamoDb.scan(scan);

        List<Map<String, String>> posts = resp.items().stream()
                .map(item -> {
                    Map<String, String> p = new HashMap<>();
                    p.put("id", item.getOrDefault("id", AttributeValue.builder().s("").build()).s());
                    p.put("content", item.getOrDefault("content", AttributeValue.builder().s("").build()).s());
                    p.put("authorName", item.getOrDefault("authorName", AttributeValue.builder().s("Anonymous").build()).s());
                    p.put("authorEmail", item.getOrDefault("authorEmail", AttributeValue.builder().s("").build()).s());
                    p.put("createdAt", item.getOrDefault("createdAt", AttributeValue.builder().s("").build()).s());
                    return p;
                })
                .sorted(Comparator.comparing(p -> p.get("createdAt"), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(headers)
                .withBody(mapper.writeValueAsString(posts));
    }

    private APIGatewayProxyResponseEvent handlePost(APIGatewayProxyRequestEvent event, Map<String, String> headers) throws Exception {
        String authHeader = readAuthHeader(event);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return error(401, "Unauthorized", headers);
        }

        String token = authHeader.substring(7);
        JwtValidator validator = new JwtValidator(AUTH0_DOMAIN, AUTH0_AUDIENCE);
        Map<String, String> claims = validator.validate(token);
        if (claims == null) {
            return error(401, "Invalid token", headers);
        }

        // Parse body
        Map<String, String> body = mapper.readValue(event.getBody(), Map.class);
        String content = body.get("content");

        if (content == null || content.isBlank()) {
            return error(400, "Content is required", headers);
        }
        if (content.length() > 140) {
            return error(400, "Content exceeds 140 characters", headers);
        }

        // Save to DynamoDB
        String id = UUID.randomUUID().toString();
        String now = Instant.now().toString();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(id).build());
        item.put("content", AttributeValue.builder().s(content).build());
        item.put("authorName", AttributeValue.builder().s(claims.getOrDefault("name", "User")).build());
        item.put("authorEmail", AttributeValue.builder().s(claims.getOrDefault("email", "")).build());
        item.put("authorId", AttributeValue.builder().s(claims.get("sub")).build());
        item.put("createdAt", AttributeValue.builder().s(now).build());

        dynamoDb.putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());

        Map<String, String> result = new HashMap<>();
        result.put("id", id);
        result.put("content", content);
        result.put("authorName", claims.getOrDefault("name", "User"));
        result.put("createdAt", now);

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(201)
                .withHeaders(headers)
                .withBody(mapper.writeValueAsString(result));
    }

    private String readAuthHeader(APIGatewayProxyRequestEvent event) {
        if (event.getHeaders() == null) {
            return null;
        }
        String auth = event.getHeaders().get("Authorization");
        if (auth == null) {
            auth = event.getHeaders().get("authorization");
        }
        return auth;
    }

    private APIGatewayProxyResponseEvent error(int code, String msg, Map<String, String> headers) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(code)
                    .withHeaders(headers)
                    .withBody(mapper.writeValueAsString(Map.of("error", msg)));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("{\"error\":\"internal\"}");
        }
    }

    private Map<String, String> corsHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("Access-Control-Allow-Origin", "*");
        h.put("Access-Control-Allow-Headers", "Content-Type,Authorization");
        h.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        h.put("Content-Type", "application/json");
        return h;
    }
}