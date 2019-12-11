
package io.helidon.examples.quickstart.mp;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserApiTest {

    static final String HOST = "http://localhost:8080/users";

    private final IUserApi userApi;

    public UserApiTest() throws URISyntaxException {
        userApi = RestClientBuilder.newBuilder()
                .baseUri(new URI(HOST))
                .register(DailyApiExceptionMapper.class)
                .build(IUserApi.class);
    }

    @Test
    public void testCreate() throws DailyApiException {
        final JsonObject userJson = Json.createObjectBuilder()
                .add("email", "test@gmail.com")
                .add("password", "test123")
                .build();
        final Response response = userApi.create(userJson);
        System.out.println(response.getStatus());
        assertEquals(201, response.getStatus(), "Response code should be 201");
    }
}
