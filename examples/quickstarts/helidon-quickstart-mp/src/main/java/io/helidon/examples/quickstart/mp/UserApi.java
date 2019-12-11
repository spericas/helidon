
package io.helidon.examples.quickstart.mp;

import javax.enterprise.context.RequestScoped;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

@RequestScoped
public class UserApi implements IUserApi {

    @Override
    public Response create(JsonObject userJson) throws DailyApiException {
        return Response
                .status(Response.Status.CREATED)
                .build();
    }
}
