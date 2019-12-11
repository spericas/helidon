
package io.helidon.examples.quickstart.mp;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class DailyApiExceptionMapper implements ExceptionMapper<DailyApiException> {

    @Override
    public Response toResponse(DailyApiException e) {
        Map<String, Object> entities = new HashMap<>();
        entities.put("code", e.getCode());
        entities.put("message", e.getMessage());
        return Response
                .status(e.getCode())
                .entity(entities)
                .build();
    }
}
