package com.lab.monitoring.repository;

import com.lab.monitoring.entity.MaintenanceParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceParameterRepository extends JpaRepository<MaintenanceParameter, Integer> {

    // Custom query: Untuk mengambil daftar parameter berdasarkan ID alat yang dipilih
    List<MaintenanceParameter> findByInstrumentId(Integer instrumentId);
}
