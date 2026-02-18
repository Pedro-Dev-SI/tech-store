package com.br.userservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateAddressDTO {

    private String street;

    private String number;

    @Length(max = 100)
    private String complement;

    private String neighborhood;

    private String city;

    private String state;

    @Length(min = 9, max = 9)
    private String zipCode;

    private Boolean isDefault;
}
