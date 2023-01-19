package com.homer.core.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.homer.core.common.redis.RedisDao;
import com.homer.core.model.db.City;
import com.homer.core.model.db.Commune;
import com.homer.core.model.db.District;
import com.homer.core.model.dto.CityDTO;
import com.homer.core.model.dto.CommuneDTO;
import com.homer.core.model.dto.DistrictDTO;
import com.homer.core.repository.CityRepository;
import com.homer.core.repository.CommuneRepository;
import com.homer.core.repository.DistrictRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class AddressServiceTest {
    @Autowired
    RedisDao redisDao;
    @Autowired
    CityRepository cityRepository;
    @Autowired
    DistrictRepository districtRepository;
    @Autowired
    CommuneRepository communeRepository;

    @Test
    public void syncMySqlRedis() {
        List<CityDTO> cityDTOs = redisDao.get("cache_city", new TypeReference<List<CityDTO>>() {
        });
        cityDTOs.forEach(cityDTO -> {
            City city = new City();
            city.setId(cityDTO.getValue());
            city.setName(cityDTO.getLabel());
            cityRepository.save(city);
        });

        List<List<DistrictDTO>> districtDTOS = redisDao.hGetAll("cache_district", new TypeReference<List<DistrictDTO>>() {});
        districtDTOS.forEach(i -> {
            i.forEach(districtDTO -> {
                District district = new District();
                district.setId(districtDTO.getValue());
                district.setName(districtDTO.getLabel());
                district.setCityId(districtDTO.getCityCode());
                districtRepository.save(district);
            });
        });

        List<List<CommuneDTO>> communeDTOS = redisDao.hGetAll("cache_commune", new TypeReference<List<CommuneDTO>>() {});
        communeDTOS.forEach(i -> {
            i.forEach(communeDTO -> {
                Commune commune = new Commune();
                commune.setId(communeDTO.getValue());
                commune.setName(communeDTO.getLabel());
                commune.setDistrictId(communeDTO.getDistrictCode());
                communeRepository.save(commune);
            });
        });
    }
}
