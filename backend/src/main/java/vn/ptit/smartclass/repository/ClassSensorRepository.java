package vn.ptit.smartclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.smartclass.entity.ClassSensor;

import java.util.Optional;

public interface ClassSensorRepository extends JpaRepository<ClassSensor, Long> {

    Optional<ClassSensor> findByCategory(String category);
}
