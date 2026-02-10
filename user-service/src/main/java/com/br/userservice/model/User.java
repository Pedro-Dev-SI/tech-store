package com.br.userservice.model;

import com.br.userservice.enums.RoleEnum;
import com.br.userservice.enums.StatusEnum;
import com.br.userservice.model.vo.Cpf;
import com.br.userservice.model.vo.Email;
import com.br.userservice.model.vo.Phone;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, unique = true, length = 254))
    private Email email;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "cpf", nullable = false, unique = true, length = 11))
    private Cpf cpf;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "phone", nullable = false, length = 11))
    private Phone phone;

    @Column(nullable = false)
    @Size(min = 2, max = 100)
    private String name;

    @Column(nullable = false)
    @Size(min = 8, max = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RoleEnum role = RoleEnum.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusEnum status = StatusEnum.ACTIVE;

    @PrePersist
    private void prePersist() {
        if (this.role == null) this.role = RoleEnum.USER;
        if (this.status == null) this.status = StatusEnum.ACTIVE;
    }

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Size(max = 5)
    private List<Address> addressList;

}
