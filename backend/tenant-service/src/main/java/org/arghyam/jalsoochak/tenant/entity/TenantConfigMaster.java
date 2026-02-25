package org.arghyam.jalsoochak.tenant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.arghyam.jalsoochak.tenant.enums.TenantConfigKeyEnum;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_config_master_table", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantConfigMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "uuid", unique = true, nullable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantMaster tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "config_key", columnDefinition = "text")
    private TenantConfigKeyEnum configKey;

    @Column(name = "config_value", columnDefinition = "text")
    private String configValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private TenantAdminUserMaster createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private TenantAdminUserMaster updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private TenantAdminUserMaster deletedBy;
}
