package com.example.stockanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;


@Data
@AllArgsConstructor
@Value
public class Stock {
    String figi;
    String ticker;
    String riskName;
    String countryRisk;
}
