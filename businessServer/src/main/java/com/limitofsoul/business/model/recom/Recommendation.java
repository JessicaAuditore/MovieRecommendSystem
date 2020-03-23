package com.limitofsoul.business.model.recom;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Recommendation {

    private int mid;

    private Double score;
}
