package ch.ecocarsolaire.csms.swap.infrastructure.ki;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.Map;

@RegisterRestClient(configKey = "ki-service")
@Path("/api/optimization/feedback")
public interface KiFeedbackRemoteApi {

    @POST
    @Path("/batch")
    Map<String, Object> submitFeedbackBatch(List<Map<String, Object>> records);
}
