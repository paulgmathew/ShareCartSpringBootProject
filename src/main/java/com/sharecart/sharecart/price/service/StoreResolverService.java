package com.sharecart.sharecart.price.service;

import com.sharecart.sharecart.price.dto.StoreInfoRequest;
import com.sharecart.sharecart.price.model.Store;

public interface StoreResolverService {

    Store resolve(StoreInfoRequest request);
}