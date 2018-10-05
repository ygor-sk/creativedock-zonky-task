package sk.ygor.creativedock.zonky;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest(RestApiController.class)
public class RestApiControllerTest {


    @Autowired
    private RestApiController restApiController;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSuccess_AAAAA() {
        setupServerMock("AAAAA", new Loan[]{
                createLoan(1, "AAAAA", 4.5),
                createLoan(2, "AAAAA", 5.5),
                createLoan(3, "AAAAA", 9.0),
                createLoan(4, "AAAAA", 6.0),
        });

        LoanStatistics loanStatistics = this.restApiController.loanStatistics("AAAAA");
        assertThat(loanStatistics).isNotNull();
        assertThat(loanStatistics.getCount()).isEqualTo(4);
        assertThat(loanStatistics.getAverage()).isEqualTo(new BigDecimal("6.25"));
        assertThat(loanStatistics.getRating()).isEqualTo("AAAAA");
    }

    @Test
    public void testSuccess_single_loan() {
        setupServerMock("single_loan", new Loan[]{
                createLoan(1, "single_loan", 42.7),
        });

        LoanStatistics loanStatistics = this.restApiController.loanStatistics("single_loan");
        assertThat(loanStatistics).isNotNull();
        assertThat(loanStatistics.getCount()).isEqualTo(1);
        assertThat(loanStatistics.getAverage()).isEqualTo(new BigDecimal("42.7"));
        assertThat(loanStatistics.getRating()).isEqualTo("single_loan");
    }

    @Test
    public void testSuccess_no_loans() {
        setupServerMock("no_loans", new Loan[]{});

        LoanStatistics loanStatistics = this.restApiController.loanStatistics("no_loans");
        assertThat(loanStatistics).isNotNull();
        assertThat(loanStatistics.getCount()).isEqualTo(0);
        assertThat(loanStatistics.getAverage()).isNull();
        assertThat(loanStatistics.getRating()).isEqualTo("no_loans");
    }

    @Test
    public void testException_missing_X_Total() {
        setupServerMock("missing_X_Total", null, new Loan[]{
                createLoan(1, "missing_X_Total", 10.0),
                createLoan(2, "missing_X_Total", 20.0),
        });

        assertThatExceptionOfType(RestApiControllerException.class)
                .isThrownBy(() -> this.restApiController.loanStatistics("missing_X_Total"))
                .withMessage("Zonky API did not provide X-Total header. Unable to confirm, that all loans were retrieved.")
                .withNoCause();
    }

    @Test
    public void testException_wrong_X_Total() {
        setupServerMock("wrong_X_Total", 3, new Loan[]{
                createLoan(1, "wrong_X_Total", 10.0),
                createLoan(2, "wrong_X_Total", 20.0),
        });

        assertThatExceptionOfType(RestApiControllerException.class)
                .isThrownBy(() -> this.restApiController.loanStatistics("wrong_X_Total"))
                .withMessage("Zonky API returned indicated total of 3 loans. However, 2 loans were retrieved.")
                .withNoCause();
    }

    @Test
    public void testException_invalid_rating() {
        setupServerMock("invalid_rating", new Loan[]{
                createLoan(1, "invalid_rating", 10.0),
                createLoan(2, "AAA", 20.0),
        });

        assertThatExceptionOfType(RestApiControllerException.class)
                .isThrownBy(() -> this.restApiController.loanStatistics("invalid_rating"))
                .withMessage("Zonky API returned loan with invalid rating. Loan ID: 2. Expected rating: invalid_rating. Actual rating: AAA")
                .withNoCause();
    }

    private void setupServerMock(String rating, Loan[] loans) {
        setupServerMock(rating, loans.length, loans);
    }

    private void setupServerMock(String rating, Integer xTotal, Loan[] loans) {
        String detailsString = formatToJson(loans);
        this.server.expect(requestTo("/loans/marketplace?rating__eq=" + rating))
                .andRespond(withSuccess(detailsString, MediaType.APPLICATION_JSON).headers(createXTotalHeader(xTotal)));
    }

    private String formatToJson(Loan... loans) {
        try {
            return objectMapper.writeValueAsString(loans);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpHeaders createXTotalHeader(Integer xTotal) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (xTotal != null) {
            httpHeaders.set("X-Total", String.valueOf(xTotal));
        }
        return httpHeaders;
    }

    private Loan createLoan(int id, String rating, Double amount) {
        return new Loan(id, "Loan" + id, rating, BigDecimal.valueOf(amount));
    }
}