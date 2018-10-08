package sk.ygor.creativedock.zonky;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
public class RestApiController {

    /**
     * By default, Zonky API returns 20 results only. We need to set X-Size header to ask for more.
     * This property contains an educated guess on both the maximum of loans, which will be retrieved
     * and also maximum of loans, which we want to parse into memory before processing them.
     * <p/>
     * If Zonky API indicates (X-Total header), that more loans exists, than we refuse to process this request.
     */
    private final long maxLoansExpected;

    private final RestTemplate restTemplate;

    private final Clock clock;

    public RestApiController(RestTemplateBuilder restTemplateBuilder,
                             @Value("${zonky.api.root.uri}") String rootUri,
                             @Value("${zonky.api.connection.timeout.millis}") int connectionTimeout,
                             @Value("${zonky.api.read.timeout.millis}") int readTimeout,
                             @Value("${zonky.api.max.loans.expected}") long maxLoansExpected, Clock clock) {
        this.maxLoansExpected = maxLoansExpected;
        this.clock = clock;
        this.restTemplate = restTemplateBuilder
                .rootUri(rootUri)
                .setConnectTimeout(connectionTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    @RequestMapping("/loanStatistics")
    public LoanStatistics loanStatistics(@RequestParam(value = "rating", defaultValue = "AAAAA") String rating) {
        ResponseEntity<Loan[]> response = consumeZonkyRestApi(rating);

        Loan[] loans = response.getBody();
        if (loans == null) {
            throw new RestApiControllerException("Zonky API server returned an empty body");
        } else {
            validateResponse(rating, loans, response.getHeaders());
            if (loans.length == 0) {
                // average of zero items is undefined, representing as null
                return new LoanStatistics(rating, 0, null, LocalDateTime.now(clock));
            } else {
                BigDecimal totalAmount = Stream.of(loans).map(Loan::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal average = totalAmount.divide(new BigDecimal(loans.length), MathContext.DECIMAL64);
                return new LoanStatistics(rating, loans.length, average, LocalDateTime.now(clock));
            }
        }
    }

    private ResponseEntity<Loan[]> consumeZonkyRestApi(String rating) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Size", String.valueOf(maxLoansExpected));

        ResponseEntity<Loan[]> response;
        try {
            response = restTemplate.exchange(
                    "/loans/marketplace?rating__eq={rating}",
                    HttpMethod.GET,
                    new HttpEntity<String>(headers),
                    Loan[].class,
                    Collections.singletonMap("rating", rating)
            );
        } catch (RestClientException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                throw new RestApiControllerException("Zonky API takes too long to respond."); // handling this as a special case
            } else {
                throw ex;
            }
        }
        return response;
    }

    private void validateResponse(String expectedRating, Loan[] loans, HttpHeaders responseHeader) {
        // validate X-Total and real count of loans which were fetched
        List<String> xTotalHeader = responseHeader.get("X-Total");
        if (xTotalHeader != null && xTotalHeader.size() == 1) {
            long totalCount = Long.parseLong(xTotalHeader.get(0));
            if (totalCount != loans.length) {
                throw new RestApiControllerException(
                        String.format("Zonky API returned indicated total of %d loans. However, %d loans were retrieved.",
                                totalCount, loans.length)
                );
            }
        } else {
            throw new RestApiControllerException(
                    "Zonky API did not provide X-Total header. Unable to confirm, that all loans were retrieved."
            );
        }

        // do all loans belong to the rating, which was used in the filter ?
        Optional<Loan> invalidLoanOption = Stream.of(loans)
                .filter(loan -> !loan.getRating().equals(expectedRating))
                .findFirst();
        if (invalidLoanOption.isPresent()) {
            Loan invalidLoan = invalidLoanOption.get();
            throw new RestApiControllerException(
                    String.format("Zonky API returned loan with invalid rating. Loan ID: %d. Expected rating: %s. Actual rating: %s",
                            invalidLoan.getId(), expectedRating, invalidLoan.getRating()
                    )
            );
        }
    }

}
