package com.shin.comment.infrastructure.openapi.docs;

import com.shin.comment.application.dto.*;
import com.shin.comment.infrastructure.openapi.schema.ProblemDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Comments", description = "Operations for creating, listing, updating and deleting commentEntities")
public interface CommentControllerApi {

    @Operation(
            summary = "Create commentEntity",
            description = "Creates a top-level commentEntity when parentId is omitted, or a reply when parentId is provided."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment created successfully", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateCommentResponse.class),
                    examples = @ExampleObject(name = "create-commentEntity-success", value = """
                            {
                              "id": "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea",
                              "threadId": "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea",
                              "parentId": null,
                              "videoId": "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b",
                              "authorId": "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf",
                              "authorDisplayName": "Jane Doe",
                              "authorAvatarUrl": "https://cdn.shin.com/avatar/jane.png",
                              "authorUrl": "https://shin.com/@janedoe",
                              "textOriginal": "First!",
                              "textDisplay": "First!",
                              "likeCount": 0
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or invalid parent reference", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class),
                    examples = @ExampleObject(name = "invalid-commentEntity", value = """
                            {
                              "type": "https://api.shin.com/errors/validation-failed",
                              "title": "Validation Failed",
                              "status": 400,
                              "detail": "Invalid commentEntity data",
                              "instance": "/v1/commentEntities",
                              "timestamp": "2026-04-11T15:15:29.403Z",
                              "correlationId": "a091fd6a-d947-4d9a-93bf-23ebf8be72c7"
                            }
                            """))),
            @ApiResponse(responseCode = "401", description = "Comment content became empty or violated content sanitization rules", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class),
                    examples = @ExampleObject(name = "invalid-commentEntity-content", value = """
                            {
                              "type": "https://api.shin.com/errors/invalid-commentEntity-content",
                              "title": "Invalid Comment Content",
                              "status": 401,
                              "detail": "Invalid commentEntity content.",
                              "instance": "/v1/commentEntities",
                              "timestamp": "2026-04-11T15:15:29.403Z",
                              "correlationId": "1f788f95-c006-4f78-b2ce-6dd4d0f20439"
                            }
                            """))),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class),
                    examples = @ExampleObject(name = "internal-error", value = """
                            {
                              "type": "https://api.shin.com/errors/internal-server-error",
                              "title": "Internal Server Error",
                              "status": 500,
                              "detail": "An unexpected error occurred. Please try again later or contact support if the problem persists.",
                              "instance": "/v1/commentEntities",
                              "timestamp": "2026-04-11T15:15:29.403Z",
                              "correlationId": "1723df22-0ddb-4c6a-9a9f-4514336384d6"
                            }
                            """)))
    })
    ResponseEntity<CreateCommentResponse> create(
            @RequestBody(required = true, description = "Payload for creating a top-level commentEntity or a reply", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateCommentRequest.class),
                    examples = {
                            @ExampleObject(name = "top-level-commentEntity", value = """
                                    {
                                      "videoId": "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b",
                                      "channelId": "5ee87cec-f4ff-4e3e-b0bd-7c7569c95cca",
                                      "content": "Great video!"
                                    }
                                    """),
                            @ExampleObject(name = "reply-commentEntity", value = """
                                    {
                                      "parentId": "34dddacf-f148-4eea-bf56-a180fe8f77fe",
                                      "videoId": "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b",
                                      "channelId": "5ee87cec-f4ff-4e3e-b0bd-7c7569c95cca",
                                      "content": "I agree with this point"
                                    }
                                    """)
                    }
            )) CreateCommentRequest createCommentRequest,
            @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", required = true,
                    description = "Authenticated user identifier in UUID format",
                    schema = @Schema(type = "string", format = "uuid", example = "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf"))
            UUID userId
    );

    @Operation(
            summary = "List commentEntities",
            description = "Returns commentEntities by ids or by parentId. Exactly one filter must be provided: id or parentId."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comments listed successfully", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CommentListResponse.class),
                    examples = @ExampleObject(name = "list-commentEntities-success", value = """
                            {
                              "nextPageToken": "eyJpZCI6IjM0ZGRkYWNmLWYxNDgtNGVlYS1iZjU2LWExODBmZThmNzdmZSIsImNyZWF0ZWRBdCI6IjIwMjYtMDQtMTFUMTE6MzM6NDUiLCJwYXJlbnRJZCI6ImM1YmNmNGJmLTBlMDEtNGZjOC04NzA2LTZiMDdjMmQ1ZTZlYSJ9",
                              "pageInfo": {
                                "totalResults": 2,
                                "resultsPerPage": 20
                              },
                              "items": [
                                {
                                  "id": "34dddacf-f148-4eea-bf56-a180fe8f77fe",
                                  "parentId": "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea",
                                  "videoId": "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b",
                                  "authorId": "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf",
                                  "authorDisplayName": "Jane Doe",
                                  "authorAvatarUrl": "https://cdn.shin.com/avatar/jane.png",
                                  "authorUrl": "https://shin.com/@janedoe",
                                  "textDisplay": "Great point",
                                  "textOriginal": "Great point",
                                  "likeCount": 12,
                                  "createdAt": "2026-04-11T11:33:45",
                                  "updatedAt": "2026-04-11T11:33:45"
                                }
                              ]
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "Invalid query combination or invalid pagination data", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class),
                    examples = {
                            @ExampleObject(name = "invalid-query", value = """
                                    {
                                      "type": "https://api.shin.com/errors/validation-failed",
                                      "title": "Validation Failed",
                                      "status": 400,
                                      "detail": "Invalid commentEntity data",
                                      "instance": "/v1/commentEntities",
                                      "timestamp": "2026-04-11T15:15:29.403Z",
                                      "correlationId": "af4ec196-b36f-4e9c-bd2d-3699d59e80a3"
                                    }
                                    """),
                            @ExampleObject(name = "type-mismatch", value = """
                                    {
                                      "type": "https://api.shin.com/errors/validation-failed",
                                      "title": "Invalid Parameter Type",
                                      "status": 400,
                                      "detail": "Parameter 'maxResults' has invalid value 'abc'. Expected type: int",
                                      "instance": "/v1/commentEntities",
                                      "timestamp": "2026-04-11T15:15:29.403Z",
                                      "correlationId": "3362f57f-bb09-4c58-a3d4-d6b9985330b3"
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class)
            ))
    })
    ResponseEntity<CommentListResponse> list(
            @Parameter(in = ParameterIn.QUERY, name = "id", description = "Comma-separated list of commentEntity ids. Use this or parentId.",
                    schema = @Schema(type = "string", example = "34dddacf-f148-4eea-bf56-a180fe8f77fe,2829f860-1e06-4db9-9e3f-837d2ff9311f"))
            String id,
            @Parameter(in = ParameterIn.QUERY, name = "parentId", description = "Parent commentEntity id to list replies. Use this or id.",
                    schema = @Schema(type = "string", format = "uuid", example = "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea"))
            String parentId,
            @Parameter(in = ParameterIn.QUERY, name = "maxResults", description = "Maximum number of records returned. Accepted range: 1 to 100.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20", example = "20"))
            int maxResults,
            @Parameter(in = ParameterIn.QUERY, name = "pageToken", description = "Opaque pagination token returned by a previous call",
                    schema = @Schema(type = "string"))
            String pageToken,
            @Parameter(in = ParameterIn.QUERY, name = "textFormat", description = "Comment text rendering format",
                    schema = @Schema(type = "string", allowableValues = {"html", "plainText"}, defaultValue = "html", example = "html"))
            String textFormat
    );

    @Operation(
            summary = "Update commentEntity",
            description = "Updates commentEntity content and/or like count delta for the commentEntity owner."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment updated successfully", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateCommentResponse.class),
                    examples = @ExampleObject(name = "update-commentEntity-success", value = """
                            {
                              "id": "34dddacf-f148-4eea-bf56-a180fe8f77fe",
                              "threadId": "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea",
                              "parentId": "c5bcf4bf-0e01-4fc8-8706-6b07c2d5e6ea",
                              "videoId": "e6f4722b-bbd2-48d5-8f89-e9a0cbfbc90b",
                              "authorId": "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf",
                              "textOriginal": "Updated content",
                              "textDisplay": "Updated content",
                              "likeCount": 10
                            }
                            """))),
            @ApiResponse(responseCode = "400", description = "Invalid request body, malformed UUID, or unsupported likeDelta value", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class),
                    examples = @ExampleObject(name = "invalid-update", value = """
                            {
                              "type": "https://api.shin.com/errors/validation-failed",
                              "title": "Validation Failed",
                              "status": 400,
                              "detail": "Invalid commentEntity data",
                              "instance": "/v1/commentEntities/34dddacf-f148-4eea-bf56-a180fe8f77fe",
                              "timestamp": "2026-04-11T15:15:29.403Z",
                              "correlationId": "fae8faef-a1a8-48f9-861f-6f34d2f0b88d"
                            }
                            """))),
            @ApiResponse(responseCode = "403", description = "User is not allowed to update this commentEntity", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class),
                    examples = @ExampleObject(name = "forbidden-update", value = """
                            {
                              "type": "https://api.shin.com/errors/unauthorized-operation",
                              "title": "Unauthorized Operation",
                              "status": 403,
                              "detail": "You dont have access to this resource",
                              "instance": "/v1/commentEntities/34dddacf-f148-4eea-bf56-a180fe8f77fe",
                              "timestamp": "2026-04-11T15:15:29.403Z",
                              "correlationId": "0c357327-91f4-4921-907f-16f7e0f2ca8d"
                            }
                            """))),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class),
                    examples = @ExampleObject(name = "commentEntity-not-found", value = """
                            {
                              "type": "https://api.shin.com/errors/commentEntity-not-found",
                              "title": "Comment Not Found",
                              "status": 404,
                              "detail": "Comment not found",
                              "instance": "/v1/commentEntities/34dddacf-f148-4eea-bf56-a180fe8f77fe",
                              "timestamp": "2026-04-11T15:15:29.403Z",
                              "correlationId": "61d8df4d-c358-4f57-8078-c1260de73896"
                            }
                            """))),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class)
            ))
    })
    ResponseEntity<UpdateCommentResponse> update(
            @RequestBody(required = true, description = "Payload for updating commentEntity content and/or like delta", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdateCommentRequest.class),
                    examples = @ExampleObject(name = "update-commentEntity-request", value = """
                            {
                              "content": "Updated content",
                              "likeDelta": 1
                            }
                            """)
            )) UpdateCommentRequest request,
            @Parameter(in = ParameterIn.PATH, name = "commentId", required = true,
                    description = "Comment identifier",
                    schema = @Schema(type = "string", format = "uuid", example = "34dddacf-f148-4eea-bf56-a180fe8f77fe"))
            UUID commentId,
            @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", required = true,
                    description = "Authenticated user identifier in UUID format",
                    schema = @Schema(type = "string", format = "uuid", example = "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf"))
            UUID userId
    );

    @Operation(
            summary = "Delete commentEntity",
            description = "Marks a commentEntity as deleted. Current implementation returns 204 even when the commentEntity does not exist or is not owned by the requester."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Comment deletion processed"),
            @ApiResponse(responseCode = "400", description = "Malformed commentId UUID", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class),
                    examples = @ExampleObject(name = "invalid-commentEntity-id", value = """
                            {
                              "type": "https://api.shin.com/errors/validation-failed",
                              "title": "Invalid Parameter Type",
                              "status": 400,
                              "detail": "Parameter 'commentId' has invalid value 'abc'. Expected type: UUID",
                              "instance": "/v1/commentEntities/abc",
                              "timestamp": "2026-04-11T15:15:29.403Z",
                              "correlationId": "5ae6a1b7-cf0d-47f3-ab2c-cd7a86af68eb"
                            }
                            """))),
            @ApiResponse(responseCode = "500", description = "Unexpected internal error", content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetailResponse.class)
            ))
    })
    ResponseEntity<Void> delete(
            @Parameter(in = ParameterIn.PATH, name = "commentId", required = true,
                    description = "Comment identifier",
                    schema = @Schema(type = "string", format = "uuid", example = "34dddacf-f148-4eea-bf56-a180fe8f77fe"))
            UUID commentId,
            @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", required = true,
                    description = "Authenticated user identifier in UUID format",
                    schema = @Schema(type = "string", format = "uuid", example = "68b94f26-65b5-4d34-b1a2-ed95ca5f88cf"))
            UUID userId
    );
}
