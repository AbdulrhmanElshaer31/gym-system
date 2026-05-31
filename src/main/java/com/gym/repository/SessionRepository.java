package com.gym.repository;

import com.gym.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByActiveTrue();

    List<Session> findByActiveTrueOrderByPriceAsc();

    List<Session> findAllByOrderByCreatedAtDesc();

    @Query("SELECT s FROM Session s ORDER BY s.active DESC, s.price ASC")
    List<Session> findAllOrderByActiveAndPrice();

    boolean existsByName(String name);
}