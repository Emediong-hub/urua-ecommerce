package com.urua.urua_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {
    private String fullName;
    private String phone;
    private String addressLine;
    private String city;
    private String state;
}