package sk.ygor.creativedock.zonky;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class RestApiController {

    /**
     * By default, Zonky API returns 20 results only. We need to set X-Size header to ask for more.
     * This property contains an educated guess on both the maximum of loans, which will be retrieved
     * and also maximum of loans, which we want to parse into memory before processing them.
     * <p/>
     * If Zonky API indicates (X-Total header), that more loans exists, than we refuse to process this request.
     */
    private final long maxLoansPage;

    private final RestTemplate restTemplate;

    private final Logger logger = LoggerFactory.getLogger(RestApiController.class);

    public RestApiController(RestTemplateBuilder restTemplateBuilder,
                             @Value("${zonky.api.root.uri}") String rootUri,
                             @Value("${zonky.api.connection.timeout.millis}") int connectionTimeout,
                             @Value("${zonky.api.read.timeout.millis}") int readTimeout,
                             @Value("${zonky.api.max.loans.page}") long maxLoansPage) {
        this.maxLoansPage = maxLoansPage;
        this.restTemplate = restTemplateBuilder
                .rootUri(rootUri)
                .setConnectTimeout(connectionTimeout)
                .setReadTimeout(readTimeout)
                .build();
    }

    @RequestMapping("/loanStatistics")
    public LoanStatistics loanStatistics(@RequestParam(value = "rating", defaultValue = "AAAAA") String rating) {
        List<Loan> loans = fetchAllLoans(rating);

        validateResponse(rating, loans);
        if (loans.isEmpty()) {
            // average of zero items is undefined, representing as null
            return new LoanStatistics(rating, 0, null, LocalDateTime.now());
        } else {
            BigDecimal totalAmount = loans.stream().map(Loan::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = totalAmount.divide(new BigDecimal(loans.size()), MathContext.DECIMAL64);
            return new LoanStatistics(rating, loans.size(), average, LocalDateTime.now());
        }
    }

    private List<Loan> fetchAllLoans(@RequestParam(value = "rating", defaultValue = "AAAAA") String rating) {
        int page = 0;
        List<Loan> loans = new ArrayList<>();
        Loan[] lastPage;
        do {
            ResponseEntity<Loan[]> response = consumeZonkyRestApi(rating, page);
            lastPage = response.getBody();
            if (lastPage == null) {
                throw new RestApiControllerException("Zonky API server returned an empty body");
            }
            loans.addAll(Arrays.asList(lastPage));
            logger.info(String.format("Fetched %d loans on page %d", response.getBody().length, page));
            page++;
        } while (lastPage.length > 0);
        return loans;
    }

    private ResponseEntity<Loan[]> consumeZonkyRestApi(String rating, int page) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Size", String.valueOf(maxLoansPage));
        headers.set("X-Page", String.valueOf(page));

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

    private void validateResponse(String expectedRating, List<Loan> loans) {
        // do all loans belong to the rating, which was used in the filter ?
        Optional<Loan> invalidLoanOption = loans.stream()
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
