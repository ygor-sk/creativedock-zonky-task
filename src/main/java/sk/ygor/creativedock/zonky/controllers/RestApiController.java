package sk.ygor.creativedock.zonky.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sk.ygor.creativedock.zonky.services.ZonkyRestApiConsumerService;

@RestController
public class RestApiController {

    private final ZonkyRestApiConsumerService zonkyRestApiConsumerService;

    @Autowired
    public RestApiController(ZonkyRestApiConsumerService zonkyRestApiConsumerService) {
        this.zonkyRestApiConsumerService = zonkyRestApiConsumerService;
    }

    @RequestMapping("/loanStatistics")
    public ResponseEntity<?> loanStatistics(@RequestParam(value = "rating", defaultValue = "AAAAA") String rating) {
        return zonkyRestApiConsumerService.loanStatistics(rating)
                .right().map(loanStatistics -> new ResponseEntity<Object>(loanStatistics, HttpStatus.OK) )
                .getOrElseGet(errorMessage -> new ResponseEntity<>(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR));
    }

}
