package com.limitofsoul.business.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MovieHybridRecommendationRequest {

    private int mid;
    private int sum;
}
