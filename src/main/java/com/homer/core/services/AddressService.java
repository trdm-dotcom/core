package com.homer.core.services;

import com.homer.core.model.request.AddressRequest;
import com.homer.core.model.response.CityResponse;
import com.homer.core.model.response.CommuneResponse;
import com.homer.core.model.response.DistrictResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AddressService {

    public CityResponse getCities(String transactionId){
        log.info("{} getAllCity", transactionId);
        CityResponse response = new CityResponse();
        return response;
    }

    public DistrictResponse getDistrictsByCity(AddressRequest request, String transactionId){
        log.info("{} getDistrictsByCity", transactionId);
        DistrictResponse response = new DistrictResponse();
        return response;
    }

    public CommuneResponse getCommuneByDistrict(AddressRequest request, String transactionId){
        log.info("{} getCommuneByDistrict", transactionId);
        CommuneResponse response = new CommuneResponse();
        return response;
    }
}
