package vn.ptit.smartclass.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.ptit.smartclass.entity.EnvironmentalMetric;

import java.util.List;
import java.util.Optional;

public interface EnvironmentalMetricRepository
        extends JpaRepository<EnvironmentalMetric, Long>, JpaSpecificationExecutor<EnvironmentalMetric> {

    /** Lay ban ghi moi nhat cua mot loai cam bien - dung cho /api/sensors/latest */
    Optional<EnvironmentalMetric> findFirstBySensor_CategoryOrderByTimestampDesc(String category);

    /** Lay N ban ghi gan nhat cua mot loai cam bien - dung ve bieu do Dashboard */
    List<EnvironmentalMetric> findBySensor_CategoryOrderByTimestampDesc(String category, Pageable pageable);
}
