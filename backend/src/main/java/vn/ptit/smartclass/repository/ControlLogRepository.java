package vn.ptit.smartclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.ptit.smartclass.entity.ControlLog;

public interface ControlLogRepository
        extends JpaRepository<ControlLog, Long>, JpaSpecificationExecutor<ControlLog> {
}
