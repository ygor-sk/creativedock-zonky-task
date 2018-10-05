package sk.ygor.creativedock.zonky;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Before
    public void setUp() throws Exception {
        String detailsString = objectMapper.writeValueAsString(new Loan[]{
                new Loan(1, "Porsche", "AAAAA", new BigDecimal("1234"))
        });
        this.server.expect(requestTo("/loans/marketplace?rating__eq=AAAAA"))
                .andRespond(withSuccess(detailsString, MediaType.APPLICATION_JSON));
    }

    @Test
    public void whenCallingLoanStatistics_thenClientMakesCorrectCall() {
        LoanStatistics loanStatistics = this.restApiController.loanStatistics("AAAAA");
        assertThat(loanStatistics.getCount()).isEqualTo(1);
    }
}