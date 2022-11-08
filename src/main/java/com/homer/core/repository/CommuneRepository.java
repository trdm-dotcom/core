package com.homer.core.repository;

import com.homer.core.model.db.Commune;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommuneRepository extends JpaRepository<Commune, Long> {
    List<Commune> findAllByDistrictId(Long districtCode);
}
