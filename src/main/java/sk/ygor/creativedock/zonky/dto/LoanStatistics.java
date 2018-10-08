package sk.ygor.creativedock.zonky.dto;

import sk.ygor.creativedock.zonky.controllers.RestApiController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JSON Entity returned by {@link RestApiController}
 */
public class LoanStatistics {

    private final String rating;
    private final long count;
    private final BigDecimal average;
    private final LocalDateTime dateTimeRetrieved;

    public LoanStatistics(String rating, long count, BigDecimal average, LocalDateTime dateTimeRetrieved) {
        this.rating = rating;
        this.count = count;
        this.average = average;
        this.dateTimeRetrieved = dateTimeRetrieved;
    }

    public String getRating() {
        return rating;
    }

    public long getCount() {
        return count;
    }

    public BigDecimal getAverage() {
        return average;
    }

    public LocalDateTime getDateTimeRetrieved() {
        return dateTimeRetrieved;
    }
}
