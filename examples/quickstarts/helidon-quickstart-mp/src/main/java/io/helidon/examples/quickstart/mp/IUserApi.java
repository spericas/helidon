
package io.helidon.examples.quickstart.mp;

import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/users")
public interface IUserApi {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response create(JsonObject userJson) throws DailyApiException;
}


