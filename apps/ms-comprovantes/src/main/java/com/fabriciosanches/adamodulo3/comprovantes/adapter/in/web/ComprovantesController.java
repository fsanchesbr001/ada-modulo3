package com.fabriciosanches.adamodulo3.comprovantes.adapter.in.web;

import com.fabriciosanches.adamodulo3.comprovantes.adapter.in.web.dto.ComprovanteAcceptedResponse;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.in.web.dto.ComprovanteCreateRequest;
import com.fabriciosanches.adamodulo3.comprovantes.adapter.in.web.dto.ComprovanteResponse;
import com.fabriciosanches.adamodulo3.comprovantes.application.ComprovanteNotFoundException;
import com.fabriciosanches.adamodulo3.comprovantes.application.CreateComprovanteUseCase;
import com.fabriciosanches.adamodulo3.comprovantes.application.GetComprovanteUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comprovantes")
public class ComprovantesController {

    private final CreateComprovanteUseCase createUseCase;
    private final GetComprovanteUseCase getUseCase;

    public ComprovantesController(CreateComprovanteUseCase createUseCase, GetComprovanteUseCase getUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
    }

    @PostMapping
    public ResponseEntity<ComprovanteAcceptedResponse> create(@Valid @RequestBody ComprovanteCreateRequest request) {
        String id = createUseCase.execute(request.payloadPdfJson());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ComprovanteAcceptedResponse(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprovanteResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(ComprovanteResponse.from(getUseCase.execute(id)));
    }

    @ExceptionHandler(ComprovanteNotFoundException.class)
    public ResponseEntity<Void> handleNotFound(ComprovanteNotFoundException ignored) {
        return ResponseEntity.notFound().build();
    }
}
