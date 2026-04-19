package co.edu.escuelaing;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class UserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.builder().region(Region.US_EAST_1).build();
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
        if (!"GET".equals(event.getHttpMethod())) {
            return error(405, "Method not allowed", headers);
        }

        String authHeader = readAuthHeader(event);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return error(401, "Unauthorized", headers);
        }

        JwtValidator validator = new JwtValidator(AUTH0_DOMAIN, AUTH0_AUDIENCE);
        Map<String, String> claims = validator.validate(authHeader.substring(7));
        if (claims == null) {
            return error(401, "Invalid token", headers);
        }

        try {
            String auth0Id = claims.get("sub");
            GetItemResponse existing = dynamoDb.getItem(GetItemRequest.builder()
                    .tableName(TABLE)
                    .key(Map.of("auth0Id", AttributeValue.builder().s(auth0Id).build()))
                    .build());

            Map<String, String> user;
            if (existing.hasItem()) {
                user = toMap(existing.item());
            } else {
                Map<String, AttributeValue> item = new HashMap<>();
                item.put("auth0Id", AttributeValue.builder().s(auth0Id).build());
                item.put("email", AttributeValue.builder().s(claims.getOrDefault("email", "")).build());
                item.put("name", AttributeValue.builder().s(claims.getOrDefault("name", "User")).build());
                item.put("picture", AttributeValue.builder().s(claims.getOrDefault("picture", "")).build());
                item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());
                dynamoDb.putItem(PutItemRequest.builder().tableName(TABLE).item(item).build());
                user = toMap(item);
            }

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(mapper.writeValueAsString(user));
        } catch (Exception e) {
            context.getLogger().log("UserHandler error: " + e.getMessage());
            return error(500, "Internal error", headers);
        }
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

    private Map<String, String> toMap(Map<String, AttributeValue> item) {
        Map<String, String> m = new HashMap<>();
        item.forEach((k, v) -> m.put(k, v.s()));
        return m;
    }

    private APIGatewayProxyResponseEvent error(int code, String msg, Map<String, String> headers) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(code)
                    .withHeaders(headers)
                    .withBody(mapper.writeValueAsString(Map.of("error", msg)));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500);
        }
    }

    private Map<String, String> corsHeaders() {
        return Map.of(
                "Access-Control-Allow-Origin", "*",
                "Access-Control-Allow-Headers", "Content-Type,Authorization",
                "Access-Control-Allow-Methods", "GET,OPTIONS",
                "Content-Type", "application/json"
        );
    }
}
