package com.limitofsoul.business.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MovieRatingRequest {

    private int uid;
    private int mid;
    private Double score;

    public Double getScore() {
        return Double.parseDouble(String.format("%.2f",score/2D));
    }
}
