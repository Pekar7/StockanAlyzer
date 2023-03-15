package com.example.stockanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;


@Data
@AllArgsConstructor
@Value
public class Candle {
    String simpleDateFormat;
    Double close;
    Long value;
    Double open;
    Double high;
    Double low;
}
