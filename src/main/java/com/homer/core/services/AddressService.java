package com.homer.core.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.homer.core.common.redis.RedisDao;
import com.homer.core.model.dto.CityDTO;
import com.homer.core.model.dto.CommuneDTO;
import com.homer.core.model.dto.DistrictDTO;
import com.homer.core.model.request.AddressRequest;
import com.homer.core.model.response.CityResponse;
import com.homer.core.model.response.CommuneResponse;
import com.homer.core.model.response.DistrictResponse;
import com.homer.core.repository.CityRepository;
import com.homer.core.repository.CommuneRepository;
import com.homer.core.repository.DistrictRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AddressService {
    private final String REDIS_KEY_CITY = "cache_city";

    private final String REDIS_KEY_DISTRICT = "cache_district";

    private final String REDIS_KEY_COMMUNE = "cache_commune";

    private final RedisDao redisDao;

    private final CityRepository cityRepository;

    private final DistrictRepository districtRepository;

    private final CommuneRepository communeRepository;

    @Autowired
    public AddressService(
            RedisDao redisDao,
            CityRepository cityRepository,
            DistrictRepository districtRepository,
            CommuneRepository communeRepository
    ){
        this.redisDao = redisDao;
        this.cityRepository = cityRepository;
        this.districtRepository = districtRepository;
        this.communeRepository = communeRepository;
    }

    public CityResponse getCities(String transactionId){
        log.info("{} getAllCity", transactionId);
        List<CityDTO> cityDTOs = redisDao.get(this.REDIS_KEY_CITY, new TypeReference<List<CityDTO>>() {});
        CityResponse response = new CityResponse();
        response.addAll(cityDTOs);
        return response;
    }

    public DistrictResponse getDistrictsByCity(AddressRequest request, String transactionId){
        log.info("{} getDistrictsByCity", transactionId);
        List<DistrictDTO> districtDTOs = redisDao.hGet(this.REDIS_KEY_DISTRICT, request.getId().toString(), new TypeReference<List<DistrictDTO>>() {});
        DistrictResponse response = new DistrictResponse();
        response.addAll(districtDTOs);
        return response;
    }

    public CommuneResponse getCommuneByDistrict(AddressRequest request, String transactionId){
        log.info("{} getCommuneByDistrict", transactionId);
        List<CommuneDTO> communeDTOs = redisDao.hGet(this.REDIS_KEY_COMMUNE, request.getId().toString(), new TypeReference<List<CommuneDTO>>() {});
        CommuneResponse response = new CommuneResponse();
        response.addAll(communeDTOs);
        return response;
    }

    public void initCache() {
        List<CityDTO> cityDTOs = this.cityRepository.findAll().stream().map(c -> {
            CityDTO dto = new CityDTO();
            dto.setLabel(c.getName());
            dto.setValue(c.getId());
            return dto;
        }).collect(Collectors.toList());
        redisDao.set("cache_city", cityDTOs);
        Map<Long, List<DistrictDTO>> mapDistrict = this.districtRepository.findAll().stream().map(d -> {
            DistrictDTO dto = new DistrictDTO();
            dto.setLabel(d.getName());
            dto.setValue(d.getId());
            dto.setCityCode(d.getCityId());
            return dto;
        }).collect(Collectors.groupingBy(DistrictDTO::getCityCode));
        mapDistrict.forEach((key, value) -> {
            System.out.println(key);
            System.out.println(value);
            redisDao.saveToMap("cache_district", key.toString(), value);
        });
        Map<Long, List<CommuneDTO>> mapCommune = this.communeRepository.findAll().stream().map(c -> {
            CommuneDTO dto = new CommuneDTO();
            dto.setLabel(c.getName());
            dto.setValue(c.getId());
            dto.setDistrictCode(c.getDistrictId());
            return dto;
        }).collect(Collectors.groupingBy(CommuneDTO::getDistrictCode));
        mapCommune.forEach((key, value) -> {
            redisDao.saveToMap("cache_commune", key.toString(), value);
        });
    }
}
