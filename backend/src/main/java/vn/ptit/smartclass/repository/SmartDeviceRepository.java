package vn.ptit.smartclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.smartclass.entity.SmartDevice;

import java.util.List;
import java.util.Optional;

public interface SmartDeviceRepository extends JpaRepository<SmartDevice, Long> {

    Optional<SmartDevice> findByDeviceCode(String deviceCode);

    List<SmartDevice> findAllByOrderByIdAsc();
}
