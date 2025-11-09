package com.agendamento.smart.model.clinic;

import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.util.CodeGeneratorListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "CLINIC")
@EntityListeners(CodeGeneratorListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private Long code;

    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "clinic")
    @JsonIgnore
    private List<User> users;

    // -------------------------
    // GERAÇÃO AUTOMÁTICA DO CODE
    // -------------------------
    @PrePersist
    public void generateCode() {
        if (this.code == null) {
            this.code = System.currentTimeMillis(); // número único crescente aleatório
        }
    }
}
