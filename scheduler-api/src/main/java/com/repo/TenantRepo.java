package com.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.entity.Tenant;
import java.util.UUID;

public interface TenantRepo extends JpaRepository<Tenant, UUID> {
    
}
