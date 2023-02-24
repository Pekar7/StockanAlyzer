package com.example.stockanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;


@Data
@AllArgsConstructor
@Value
public class Candle {
    Double open;
    Double high;
    Double low;
    Double close;
    String simpleDateFormat;
}
