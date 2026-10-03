package de.phlup.circuitchaos.config;

import de.phlup.circuitchaos.exception.CircuitChaosException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@SuppressWarnings("unused")
public class CircuitChaosControllerAdvice {

    @ExceptionHandler(CircuitChaosException.class)
    public ResponseEntity<String> handleCircuitChaosException(CircuitChaosException ex) {
        return ResponseEntity.unprocessableEntity().body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleOtherExceptions(Exception ex) {
        return ResponseEntity.unprocessableEntity().body("ERROR: " + ex.getMessage());
    }

}
