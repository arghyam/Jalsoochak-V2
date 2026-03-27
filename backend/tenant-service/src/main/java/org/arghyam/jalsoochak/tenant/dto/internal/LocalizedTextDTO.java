package org.arghyam.jalsoochak.tenant.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for multilingual text supporting all Indian official languages.
 * 
 * Supported languages (ISO 639-1 codes):
 * - en: English (mandatory)
 * - hi: Hindi
 * - ta: Tamil
 * - te: Telugu
 * - kn: Kannada
 * - ml: Malayalam
 * - mr: Marathi
 * - gu: Gujarati
 * - bn: Bengali
 * - pa: Punjabi
 * - ur: Urdu
 * - or: Odia
 * - as: Assamese
 * - sa: Sanskrit
 * 
 * Only English (en) is mandatory. Other languages are optional.
 * 
 * JSON Example:
 * {
 *   "en": "English text",
 *   "hi": "हिंदी पाठ",
 *   "ta": "தமிழ் உரை",
 *   "as": "অসমীয়া পাঠ"
 * }
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class LocalizedTextDTO {
    @NotBlank(message = "English text is required")
    @JsonProperty("en")
    private String english;
    
    @JsonProperty("hi")
    private String hindi;
    
    @JsonProperty("ta")
    private String tamil;
    
    @JsonProperty("te")
    private String telugu;
    
    @JsonProperty("kn")
    private String kannada;
    
    @JsonProperty("ml")
    private String malayalam;
    
    @JsonProperty("mr")
    private String marathi;
    
    @JsonProperty("gu")
    private String gujarati;
    
    @JsonProperty("bn")
    private String bengali;
    
    @JsonProperty("pa")
    private String punjabi;
    
    @JsonProperty("ur")
    private String urdu;
    
    @JsonProperty("or")
    private String odia;
    
    @JsonProperty("as")
    private String assamese;
    
    @JsonProperty("sa")
    private String sanskrit;
}
