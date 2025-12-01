package com.crypto.alerting.commons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceEvent {
    private String ticker;
    private Double price;
    private Long timestamp;
}
