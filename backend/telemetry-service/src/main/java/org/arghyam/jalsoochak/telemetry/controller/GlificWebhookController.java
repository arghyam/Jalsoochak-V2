package org.arghyam.jalsoochak.telemetry.controller;

import org.arghyam.jalsoochak.telemetry.dto.response.ClosingResponse;
import org.arghyam.jalsoochak.telemetry.dto.response.CreateReadingResponse;
import org.arghyam.jalsoochak.telemetry.dto.response.IntroResponse;
import org.arghyam.jalsoochak.telemetry.dto.response.SelectionResponse;
import org.arghyam.jalsoochak.telemetry.dto.requests.ClosingRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.GlificWebhookRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.IntroRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.ManualReadingRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.MeterChangeRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.SelectedChannelRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.SelectedItemRequest;
import org.arghyam.jalsoochak.telemetry.dto.requests.SelectedLanguageRequest;
import org.arghyam.jalsoochak.telemetry.service.GlificWebhookService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/webhook")
public class GlificWebhookController {
    private static final Logger log = LoggerFactory.getLogger(GlificWebhookController.class);
    private final GlificWebhookService glificWebhookService;

    public GlificWebhookController(GlificWebhookService glificWebhookService) {
        this.glificWebhookService = glificWebhookService;
    }

    @PostMapping(
            value = "/glific",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<CreateReadingResponse> receive(@RequestBody GlificWebhookRequest glificWebhookRequest) {
        try {
            CreateReadingResponse response = glificWebhookService.processImage(glificWebhookRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing webhook for contactId {}: {}",
                    glificWebhookRequest.getContactId(), e.getMessage(), e);

            CreateReadingResponse errorResponse = CreateReadingResponse.builder()
                    .correlationId(glificWebhookRequest.getContactId())
                    .meterReading(null)
                    .qualityStatus("REJECTED")
                    .qualityConfidence(null)
                    .lastConfirmedReading(null)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }

    }

    @PostMapping("/intro")
    public ResponseEntity<IntroResponse> sendIntro(@RequestBody @Valid IntroRequest introRequest) {
        try {
            IntroResponse response = glificWebhookService.introMessage(introRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending intro message for contactId {}: {}", introRequest.getContactId(), e.getMessage(), e);

            IntroResponse fallbackResponse = IntroResponse.builder()
                    .success(false)
                    .message("Something went wrong. Please try again.")
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fallbackResponse);
        }
    }

    @PostMapping("/closing")
    public ResponseEntity<ClosingResponse> closingMessage(@RequestBody @Valid ClosingRequest closingRequest) {
        try {
            ClosingResponse response = glificWebhookService.closingMessage(closingRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending closing message for contactId {}: {}", closingRequest.getContactId(), e.getMessage(), e);

            ClosingResponse fallbackResponse = ClosingResponse.builder()
                    .success(false)
                    .message("Something went wrong. Please try again.")
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fallbackResponse);
        }
    }

    @PostMapping("/language/selection")
    public ResponseEntity<IntroResponse> languageSelection(@RequestBody @Valid IntroRequest request) {
        try {
            IntroResponse response = glificWebhookService.languageSelectionMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error preparing language selection for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    IntroResponse.builder()
                            .success(false)
                            .message("Language selection could not be prepared.")
                            .build()
            );
        }
    }

    @PostMapping("/selected/language")
    public ResponseEntity<IntroResponse> selectedLanguage(@RequestBody @Valid SelectedLanguageRequest request) {
        try {
            IntroResponse response = glificWebhookService.selectedLanguageMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing selected language for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    IntroResponse.builder()
                            .success(false)
                            .message("Language selection could not be saved.")
                            .build()
            );
        }
    }

    @PostMapping("/channel/selection")
    public ResponseEntity<IntroResponse> channelSelection(@RequestBody @Valid IntroRequest request) {
        try {
            IntroResponse response = glificWebhookService.channelSelectionMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error preparing channel selection for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    IntroResponse.builder()
                            .success(false)
                            .message("Channel selection could not be prepared.")
                            .build()
            );
        }
    }

    @PostMapping("/selected/channel")
    public ResponseEntity<IntroResponse> selectedChannel(@RequestBody @Valid SelectedChannelRequest request) {
        try {
            IntroResponse response = glificWebhookService.selectedChannelMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing selected channel for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    IntroResponse.builder()
                            .success(false)
                            .message("Channel selection could not be saved.")
                            .build()
            );
        }
    }

    @PostMapping("/item/selection")
    public ResponseEntity<IntroResponse> itemSelection(@RequestBody @Valid IntroRequest request) {
        try {
            IntroResponse response = glificWebhookService.itemSelectionMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error preparing item selection for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    IntroResponse.builder()
                            .success(false)
                            .message("Item selection could not be prepared.")
                            .build()
            );
        }
    }

    @PostMapping("/selected/item")
    public ResponseEntity<SelectionResponse> selectedItem(@RequestBody @Valid SelectedItemRequest request) {
        try {
            SelectionResponse response = glificWebhookService.selectedItemMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing selected item for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    SelectionResponse.builder()
                            .success(false)
                            .selected(null)
                            .message("Item selection could not be saved.")
                            .build()
            );
        }
    }

    @PostMapping("/meterChange")
    public ResponseEntity<IntroResponse> meterChange(@RequestBody @Valid MeterChangeRequest request) {
        try {
            IntroResponse response = glificWebhookService.meterChangeMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error preparing meter change reasons for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    IntroResponse.builder()
                            .success(false)
                            .message("Meter change reasons could not be prepared.")
                            .build()
            );
        }
    }

    @PostMapping("/takemeterreading")
    public ResponseEntity<IntroResponse> takeMeterReading(@RequestBody @Valid MeterChangeRequest request) {
        try {
            IntroResponse response = glificWebhookService.takeMeterReadingMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error preparing take meter reading prompt for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    IntroResponse.builder()
                            .success(false)
                            .message("Take meter reading prompt could not be prepared.")
                            .build()
            );
        }
    }

    @PostMapping("/manualReading")
    public ResponseEntity<CreateReadingResponse> manualReading(@RequestBody @Valid ManualReadingRequest request) {
        try {
            CreateReadingResponse response = glificWebhookService.manualReadingMessage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error saving manual reading for contactId {}: {}", request.getContactId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CreateReadingResponse.builder()
                            .success(false)
                            .message("Manual reading could not be saved.")
                            .qualityStatus("REJECTED")
                            .correlationId(request.getContactId())
                            .build()
            );
        }
    }

}
