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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.builder()
            .region(Region.US_EAST_1).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String TABLE = System.getenv("USERS_TABLE");
    private static final String AUTH0_DOMAIN = System.getenv("AUTH0_DOMAIN");
    private static final String AUTH0_AUDIENCE = System.getenv("AUTH0_AUDIENCE");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        Map<String, String> headers = corsHeaders();
        if ("OPTIONS".equals(event.getHttpMethod())) {
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody("");
        }

        String authHeader = event.getHeaders() != null ? event.getHeaders().get("Authorization") : null;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return error(401, "Unauthorized", headers);
        }

        JwtValidator validator = new JwtValidator(AUTH0_DOMAIN, AUTH0_AUDIENCE);
        Map<String, String> claims = validator.validate(authHeader.substring(7));
        if (claims == null) return error(401, "Invalid token", headers);

        try {
            String auth0Id = claims.get("sub");

            // Try to get existing user
            GetItemResponse existing = dynamoDb.getItem(GetItemRequest.builder()
                    .tableName(TABLE)
                    .key(Map.of("auth0Id", AttributeValue.builder().s(auth0Id).build()))
                    .build());

            Map<String, String> user;
            if (existing.hasItem()) {
                user = toMap(existing.item());
            } else {
                // Create new user
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("auth0Id", AttributeValue.builder().s(auth0Id).build());
                item.put("email", AttributeValue.builder().s(claims.getOrDefault("email", "")).build());
                item.put("name", AttributeValue.builder().s(claims.getOrDefault("name", "User")).build());
                item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());
                dynamoDb.putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
                user = new HashMap<>();
                user.put("auth0Id", auth0Id);
                user.put("email", claims.getOrDefault("email", ""));
                user.put("name", claims.getOrDefault("name", "User"));
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(mapper.writeValueAsString(user));
        } catch (Exception e) {
            return error(500, e.getMessage(), headers);
        }
    }

    private Map<String, String> toMap(Map<String, AttributeValue> item) {
        Map<String, String> m = new HashMap<>();
        item.forEach((k, v) -> m.put(k, v.s()));
        return m;
    }

    private APIGatewayProxyResponseEvent error(int code, String msg, Map<String, String> headers) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(code).withHeaders(headers)
                    .withBody(mapper.writeValueAsString(Map.of("error", msg)));
        } catch (Exception e) { return new APIGatewayProxyResponseEvent().withStatusCode(500); }
    }

    private Map<String, String> corsHeaders() {
        return Map.of(
                "Access-Control-Allow-Origin", "*",
                "Access-Control-Allow-Headers", "Content-Type,Authorization",
                "Access-Control-Allow-Methods", "GET,POST,OPTIONS",
                "Content-Type", "application/json"
        );
    }
}