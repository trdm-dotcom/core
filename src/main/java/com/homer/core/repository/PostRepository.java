package com.homer.core.repository;

import com.homer.core.model.Category;
import com.homer.core.model.db.Feature;
import com.homer.core.model.db.Post;
import org.apache.commons.collections.CollectionUtils;
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
import java.util.ArrayList;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor {
    List<Post> findByIdIn(List<Long> ids);

    default Page<Post> findAll(List<Long> ids, String userId, String name, Long city, Long commune, Long district, Category category, Double start, Double end, Double size, List<Long> features, Pageable pageable) {
        return this.findAll(new Specification<Post>() {
            @Override
            public Predicate toPredicate(Root<Post> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                if(!CollectionUtils.isEmpty(ids)){
                    predicates.add(criteriaBuilder.and(root.get("id").in(ids)));
                }
                if (userId != null) {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("userId"), userId)));
                }
                if (name != null) {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.like(root.get("name"), "%" + name + "%")));
                }
                if (city != null) {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("city").get("id"), city)));
                }
                if (commune != null) {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("commune").get("id"), commune)));
                }
                if (district != null) {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("district").get("id"), district)));
                }
                if (category != null) {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("category"), category)));
                }
                if (start != null) {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), category)));
                }
                if (end != null) {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.lessThanOrEqualTo(root.get("price"), category)));
                }
                if (size != null) {
                    predicates.add(criteriaBuilder.and(criteriaBuilder.lessThanOrEqualTo(root.get("size"), category)));
                }
                if (!CollectionUtils.isEmpty(features)) {
                    predicates.add(criteriaBuilder.and(root.get("features").get("id").in("features")));
                }
                return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, pageable);
    }
}
