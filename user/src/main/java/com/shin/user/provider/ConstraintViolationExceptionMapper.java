package com.shin.user.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Set;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(final ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();

        ObjectNode response = mapper.createObjectNode();

        response.put("timestamp", System.currentTimeMillis());
        response.put("status", 400);
        response.put("title", "Validation Errors");
        response.put("resource", uriInfo.getAbsolutePath().getPath());

        ArrayNode errors = mapper.createArrayNode();

        for (final ConstraintViolation<?> violation : violations) {
            ObjectNode error = mapper.createObjectNode();
            error.put("field", violation.getPropertyPath().toString());
            error.put("message", violation.getMessage());
            error.put("rejectedValue", violation.getInvalidValue().toString());
            errors.add(error);
        }

        response.set("errors", errors);

        return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
    }
}