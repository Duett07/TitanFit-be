package com.app.be.service;

import com.app.be.dto.address.AddressRequest;
import com.app.be.dto.address.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    List<AddressResponse> getMyAddresses(UUID userId);

    AddressResponse createAddress(UUID userId, AddressRequest request);

    AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request);

    void deleteAddress(UUID userId, UUID addressId);

    AddressResponse setDefaultAddress(UUID userId, UUID addressId);
}
