package org.arghyam.jalsoochak.tenant.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_master_table", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "uuid", unique = true, nullable = false)
    private String uuid;

    @Column(name = "state_code", unique = true, nullable = false)
    private String stateCode;

    @Column(name = "lgd_code", unique = true, nullable = false)
    private Integer lgdCode;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private TenantAdminUserMaster createdBy;

    @Column(name = "onboarded_at")
    private LocalDateTime onboardedAt;

    /**
     * @see org.arghyam.jalsoochak.tenant.enums.TenantStatusEnum
     */
    @Column(name = "status", nullable = false)
    private Integer status;

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
