package com.payroute.provider;

import com.payroute.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PaymentProviderRegistry {

    private final Map<String, PaymentProviderClient> clientMap = new ConcurrentHashMap<>();

    public PaymentProviderRegistry(List<PaymentProviderClient> clients) {
        for (PaymentProviderClient client : clients) {
            clientMap.put(client.getProviderCode().toUpperCase(), client);
        }
    }

    public PaymentProviderClient getClient(String providerCode) {
        PaymentProviderClient client = clientMap.get(providerCode.toUpperCase());
        if (client == null) {
            throw new ResourceNotFoundException("No payment client implementation found for provider: " + providerCode);
        }
        return client;
    }

    public Optional<PaymentProviderClient> findClient(String providerCode) {
        return Optional.ofNullable(clientMap.get(providerCode.toUpperCase()));
    }
}
