package com.shin.comment.infrastructure.openapi.docs;

import com.shin.comment.application.dto.ThreadListResponse;
import com.shin.comment.infrastructure.openapi.schema.ProblemDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Threads", description = "Operations for listing commentEntity threadEntities")
public interface ThreadControllerApi {

    @Operation(
            summary = "List threadEntities",
            description = "Returns threadEntities by ids, by videoId or by allThreadsRelatedToChannelId. Exactly one of these filters must be provided."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Threads listed successfully", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ThreadListResponse.class),
                    examples = @ExampleObject(name = "list-threadEntities-success", value = """
                            {
                              "nextPageToken": "eyJ2aWRlb0lkIjoiZTZmNDcyMmItYmJkMi00OGQ1LThmODktZTlhMGNiZmJjOTBiIiwidG9wTGV2ZWxDb21tZW50SWQiOiJjNWJjZjRiZi0wZTAxLTRmYzgtODcwNi02YjA3YzJkNWU2ZWEifQ==",
                              "pageInfo": {
                                "totalResults": 1,
                                "resultsPerPage": 20
                              },
                              "items": [
                                {
                                  "id": "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea",
                                  "videoId": "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b",
                                  "channelId": "5ee87cec-f4ff-4e3e-b0bd-7c7569c95cca",
                                  "authorId": "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf",
                                  "authorDisplayName": "Jane Doe",
                                  "authorAvatarUrl": "https://cdn.shin.com/avatar/jane.png",
                                  "authorUrl": "https://shin.com/@janedoe",
                                  "totalReplyCount": 4,
                                  "createdAt": "2026-04-11T11:33:45",
                                  "updatedAt": "2026-04-11T11:50:00"
                                }
                              ]
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "Invalid query combination, unsupported values, or invalid parameter type", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class),
                    examples = {
                            @ExampleObject(name = "invalid-filter-combination", value = """
                                    {
                                      "type": "https://api.shin.com/errors/validation-failed",
                                      "title": "Validation Failed",
                                      "status": 400,
                                      "detail": "Invalid commentEntity data",
                                      "instance": "/v1/threadEntities",
                                      "timestamp": "2026-04-11T15:15:29.403Z",
                                      "correlationId": "767fec8e-d9cb-4f03-a7d7-a58d8bbcc3a8"
                                    }
                                    """),
                            @ExampleObject(name = "invalid-max-results", value = """
                                    {
                                      "type": "https://api.shin.com/errors/validation-failed",
                                      "title": "Validation Failed",
                                      "status": 400,
                                      "detail": "Invalid commentEntity data",
                                      "instance": "/v1/threadEntities",
                                      "timestamp": "2026-04-11T15:15:29.403Z",
                                      "correlationId": "06baf3d9-3522-46fd-b154-5854db87e799"
                                    }
                                    """),
                            @ExampleObject(name = "header-type-mismatch", value = """
                                    {
                                      "type": "https://api.shin.com/errors/validation-failed",
                                      "title": "Invalid Parameter Type",
                                      "status": 400,
                                      "detail": "Parameter 'X-User-Id' has invalid value 'abc'. Expected type: UUID",
                                      "instance": "/v1/threadEntities",
                                      "timestamp": "2026-04-11T15:15:29.403Z",
                                      "correlationId": "90291e92-bf5e-4fb2-b4b8-553dbce30c2b"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class)
            ))
    })
    ResponseEntity<ThreadListResponse> list(
            @Parameter(in = ParameterIn.QUERY, name = "id", description = "Comma-separated list of top-level commentEntity ids. Use only one filter among id, videoId, or allThreadsRelatedToChannelId.",
                    schema = @Schema(type = "string", example = "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea,2829f860-1e06-4db9-9e3f-837d2ff9311f"))
            String id,
            @Parameter(in = ParameterIn.QUERY, name = "videoId", description = "Video identifier used to list related threadEntities",
                    schema = @Schema(type = "string", format = "uuid", example = "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b"))
            String videoId,
            @Parameter(in = ParameterIn.QUERY, name = "allThreadsRelatedToChannelId", description = "Channel identifier used to list related threadEntities",
                    schema = @Schema(type = "string", format = "uuid", example = "5ee87cec-f4ff-4e3e-b0bd-7c7569c95cca"))
            String allThreadsRelatedToChannelId,
            @Parameter(in = ParameterIn.QUERY, name = "maxResults", description = "Maximum number of records returned. Accepted range: 1 to 100.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20", example = "20"))
            int maxResults,
            @Parameter(in = ParameterIn.QUERY, name = "order", description = "Ordering strategy. Current implementation applies ordering by createdAt when order=time.",
                    schema = @Schema(type = "string", allowableValues = {"time"}, defaultValue = "time", example = "time"))
            String order,
            @Parameter(in = ParameterIn.QUERY, name = "pageToken", description = "Opaque pagination token returned by a previous call",
                    schema = @Schema(type = "string"))
            String pageToken,
            @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", required = true,
                    description = "Authenticated user identifier in UUID format",
                    schema = @Schema(type = "string", format = "uuid", example = "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf"))
            UUID userId
    );
}
