package com.limitofsoul.business.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRecommendationRequest {

    private int uid;
    private int sum;
}
