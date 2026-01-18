package com.confiance.notification.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitInfo {

    private int minuteUsed;
    private int minuteLimit;
    private int minuteResetIn;

    private int hourUsed;
    private int hourLimit;
    private int hourResetIn;

    private int dailyUsed;
    private int dailyLimit;
    private int dailyResetIn;
}
