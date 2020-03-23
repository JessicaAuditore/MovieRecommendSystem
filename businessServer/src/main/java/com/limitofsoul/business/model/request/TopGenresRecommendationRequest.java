package com.limitofsoul.business.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopGenresRecommendationRequest {

    private String genres;
    private int sum;
}
