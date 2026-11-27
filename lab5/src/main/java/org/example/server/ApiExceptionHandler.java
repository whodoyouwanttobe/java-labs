package org.example.server;

import org.example.api.ContactAlreadyExistsException;
import org.example.api.ContactNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ContactNotFoundException.class)
    public ProblemDetail notFound(ContactNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setType(URI.create("urn:problem:contact-not-found"));
        problem.setTitle("Contact not found");
        problem.setProperty("code", "CONTACT_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(ContactAlreadyExistsException.class)
    public ProblemDetail duplicate(ContactAlreadyExistsException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setType(URI.create("urn:problem:duplicate-name"));
        problem.setTitle("Duplicate contact name");
        problem.setProperty("code", "DUPLICATE_NAME");
        return problem;
    }
}
