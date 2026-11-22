package com.payroute.provider;

import org.springframework.stereotype.Component;

@Component("providerCClient")
public class SimulatedProviderCClient extends BaseSimulatedProviderClient {
    @Override
    public String getProviderCode() {
        return "PROVIDER_C";
    }
}
