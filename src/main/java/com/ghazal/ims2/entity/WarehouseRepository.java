package com.ghazal.ims2.entity;

import io.jmix.core.repository.JmixDataRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JmixDataRepository<Warehouse, Long> {
}
