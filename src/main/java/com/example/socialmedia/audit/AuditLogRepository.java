package com.example.socialmedia.audit;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Repository for audit logs
 */
@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    /**
     * Find audit logs by user ID
     */
    List<AuditLog> findByUserId(Long userId);

    /**
     * Find audit logs by action
     */
    List<AuditLog> findByAction(String action);

    /**
     * Find audit logs by resource
     */
    List<AuditLog> findByResource(String resource);

    /**
     * Find failed audit logs
     */
    List<AuditLog> findByStatus(String status);

    /**
     * Find audit logs within time range
     */
    @Query("{'timestamp': {'$gte': ?0, '$lte': ?1}}")
    List<AuditLog> findByTimestampBetween(ZonedDateTime start, ZonedDateTime end);

    /**
     * Find recent audit logs by user
     */
    @Query("{'userId': ?0, 'status': 'DENIED'}")
    List<AuditLog> findDeniedAccessByUser(Long userId);
}
