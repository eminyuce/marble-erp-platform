package com.ozerler.marble.controller.admin;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.GlobalSearchResponse;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class GlobalSearchController extends AbstractController {

    private final GlobalSearchService globalSearchService;

    @GetMapping("/api/search")
    public @ResponseBody BackEndResponse search(@RequestParam(value = "q", defaultValue = "") String query) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Executing global search for query '{}'", query);
            GlobalSearchResponse searchResponse = globalSearchService.search(query);

            HttpHeaders responseHeaders = new HttpHeaders();
            ResponseEntity<GlobalSearchResponse> resp = new ResponseEntity<>(searchResponse, responseHeaders, HttpStatus.OK);

            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("Global search successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred in global search query '{}'", query, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "search", Constants.ERR_FATAL);
        }

        return ber;
    }
}
