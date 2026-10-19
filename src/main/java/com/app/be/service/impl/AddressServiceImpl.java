package com.app.be.service.impl;

import com.app.be.dto.address.AddressRequest;
import com.app.be.dto.address.AddressResponse;
import com.app.be.entity.Address;
import com.app.be.entity.User;
import com.app.be.exception.ResourceNotFoundException;
import com.app.be.repository.AddressRepository;
import com.app.be.repository.UserRepository;
import com.app.be.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(UUID userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(AddressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse createAddress(UUID userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        boolean shouldBeDefault = Boolean.TRUE.equals(request.getIsDefault());
        if (addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).isEmpty()) {
            shouldBeDefault = true;
        } else if (shouldBeDefault) {
            unsetDefaultAddress(userId);
        }

        Address address = Address.builder()
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .province(request.getProvince().trim())
                .district(request.getDistrict().trim())
                .ward(request.getWard() != null ? request.getWard().trim() : null)
                .detailedAddress(request.getDetailedAddress().trim())
                .isDefault(shouldBeDefault)
                .label(request.getLabel() != null ? request.getLabel().trim() : "Nhà Riêng")
                .build();

        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại hoặc không thuộc về bạn"));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            unsetDefaultAddress(userId);
            address.setIsDefault(true);
        }

        address.setRecipientName(request.getRecipientName().trim());
        address.setPhoneNumber(request.getPhoneNumber().trim());
        address.setProvince(request.getProvince().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setWard(request.getWard() != null ? request.getWard().trim() : null);
        address.setDetailedAddress(request.getDetailedAddress().trim());
        if (request.getLabel() != null) {
            address.setLabel(request.getLabel().trim());
        }

        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại hoặc không thuộc về bạn"));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setIsDefault(true);
                addressRepository.save(remaining.get(0));
            }
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tồn tại hoặc không thuộc về bạn"));

        unsetDefaultAddress(userId);
        address.setIsDefault(true);
        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    private void unsetDefaultAddress(UUID userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(addr -> {
            addr.setIsDefault(false);
            addressRepository.save(addr);
        });
    }
}
