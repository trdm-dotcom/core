package com.homer.core.repository;

import com.homer.core.model.db.Booking;
import com.homer.core.model.db.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor {
    List<Booking> findByPost(Post post);

    List<Booking> findByPostIdAndFromTimeBetween(Long id, LocalDateTime from, LocalDateTime to);

    default Page<Booking> findAll(Boolean side, String userId, LocalDateTime fromTime, LocalDateTime toTime, Pageable pageable) {
        return this.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                if (side) {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.lessThanOrEqualTo(root.get("userId"), userId)));
                } else {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.lessThanOrEqualTo(root.get("userIdSideB"), userId)));
                }
                predicates.add(criteriaBuilder.and(criteriaBuilder.greaterThanOrEqualTo(root.get("toTime"), fromTime)));
                predicates.add(criteriaBuilder.and(criteriaBuilder.lessThanOrEqualTo(root.get("fromTime"), toTime)));
                predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("active"), true)));
                return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, pageable);
    }

    Optional<Booking> findByUserIdAndPostIdAndActiveAndToTimeLessThan(String userId, Long postId, Boolean isActive, LocalDateTime now);
}
