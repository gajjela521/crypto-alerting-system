package com.crypto.alerting.commons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlertTrigger {
    private String userId;
    private String message;
    private String ticker;
    private Double triggeredPrice;
    private Long timestamp;
}
