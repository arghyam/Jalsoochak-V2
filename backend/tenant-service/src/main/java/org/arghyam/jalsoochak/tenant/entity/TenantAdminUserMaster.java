package org.arghyam.jalsoochak.tenant.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_admin_user_master_table", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantAdminUserMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "uuid", unique = true, nullable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantMaster tenant;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone_number", nullable = false, columnDefinition = "text")
    private String phoneNumber;

    @Column(name = "admin_level", nullable = false)
    private Integer adminLevel;

    @Column(name = "password", nullable = false, columnDefinition = "text")
    private String password;

    @Builder.Default
    @Column(name = "email_verification_status", nullable = false)
    private Boolean emailVerificationStatus = false;

    @Builder.Default
    @Column(name = "phone_verification_status", nullable = false)
    private Boolean phoneVerificationStatus = false;

    @Column(name = "status", nullable = false)
    private Integer status;

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
