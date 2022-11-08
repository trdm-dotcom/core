package com.homer.core.repository;

import com.homer.core.model.db.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findAllByCityId(Long CityCode);
}
