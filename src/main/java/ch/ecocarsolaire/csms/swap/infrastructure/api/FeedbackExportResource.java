package ch.ecocarsolaire.csms.swap.infrastructure.api;

import ch.ecocarsolaire.csms.swap.application.service.ExportSwapFeedbackUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * Internal endpoint to trigger feedback export (used by E2E and operators).
 */
@Path("/internal/feedback")
@Produces(MediaType.APPLICATION_JSON)
public class FeedbackExportResource {

    @Inject
    ExportSwapFeedbackUseCase exportSwapFeedbackUseCase;

    @POST
    @Path("/export")
    public Response export() {
        int exported = exportSwapFeedbackUseCase.execute();
        return Response.ok(Map.of("exported", exported)).build();
    }
}
