package sk.ygor.creativedock.zonky.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sk.ygor.creativedock.zonky.dto.LoanStatistics;
import sk.ygor.creativedock.zonky.services.ZonkyRestApiConsumerService;

@RestController
public class RestApiController {

    private final ZonkyRestApiConsumerService zonkyRestApiConsumerService;

    @Autowired
    public RestApiController(ZonkyRestApiConsumerService zonkyRestApiConsumerService) {
        this.zonkyRestApiConsumerService = zonkyRestApiConsumerService;
    }

    @RequestMapping("/loanStatistics")
    public LoanStatistics loanStatistics(@RequestParam(value = "rating", defaultValue = "AAAAA") String rating) {
        return zonkyRestApiConsumerService.loanStatistics(rating);
    }

}
