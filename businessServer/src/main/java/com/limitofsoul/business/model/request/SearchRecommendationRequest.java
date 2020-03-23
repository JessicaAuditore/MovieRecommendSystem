package com.limitofsoul.business.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchRecommendationRequest {

    private String text;
    private int sum;
}
