package vn.ptit.smartclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.ptit.smartclass.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
