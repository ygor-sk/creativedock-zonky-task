package sk.ygor.creativedock.zonky;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Stream;

@RestController
public class RestApiController {

    private final RestTemplate restTemplate;

    public RestApiController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @RequestMapping("/loanStatistics")
    public LoanStatistics greeting(@RequestParam(value = "rating", defaultValue = "AAAAA") String rating) {
        ResponseEntity<Loan[]> response = restTemplate.getForEntity(
                "https://api.zonky.cz/loans/marketplace?rating__eq={rating}",
                Loan[].class,
                Collections.singletonMap("rating", rating)
        );
        Loan[] loans = response.getBody();
        if (loans == null) {
            throw new RuntimeException("TODO");
        } else if (loans.length == 0) {
            return new LoanStatistics(rating, 0, null, LocalDateTime.now());
        } else {
            BigDecimal totalAmount = Stream.of(loans).map(Loan::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = totalAmount.divide(new BigDecimal(loans.length), MathContext.UNLIMITED);
            return new LoanStatistics(rating, loans.length, average, LocalDateTime.now());
        }
    }

}
