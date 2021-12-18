package com.aspicereporting.entity;

import com.aspicereporting.entity.views.View;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonView(View.Simple.class)
@Entity
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;

    public enum ERole {
        ROLE_ADMIN,
        ROLE_USER
    }
}
