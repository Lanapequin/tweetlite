package co.edu.escuelaing;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String POSTS_TABLE = System.getenv("POSTS_TABLE");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        Map<String, String> headers = corsHeaders();

        if ("OPTIONS".equals(event.getHttpMethod())) {
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody("");
        }
        if (!"GET".equals(event.getHttpMethod())) {
            return error(405, "Method not allowed", headers);
        }

        try {
            ScanResponse resp = dynamoDb.scan(ScanRequest.builder().tableName(POSTS_TABLE).build());

            List<Map<String, String>> posts = new ArrayList<>();
            for (Map<String, AttributeValue> item : resp.items()) {
                Map<String, String> p = new HashMap<>();
                p.put("id", item.getOrDefault("id", AttributeValue.builder().s("").build()).s());
                p.put("content", item.getOrDefault("content", AttributeValue.builder().s("").build()).s());
                p.put("authorName", item.getOrDefault("authorName", AttributeValue.builder().s("Anonymous").build()).s());
                p.put("authorEmail", item.getOrDefault("authorEmail", AttributeValue.builder().s("").build()).s());
                p.put("createdAt", item.getOrDefault("createdAt", AttributeValue.builder().s("").build()).s());
                posts.add(p);
            }

            posts.sort(Comparator.comparing(p -> p.get("createdAt"), Comparator.reverseOrder()));

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(mapper.writeValueAsString(posts));
        } catch (Exception e) {
            context.getLogger().log("StreamHandler error: " + e.getMessage());
            return error(500, "Internal error", headers);
        }
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
