package com.payroute.provider;

import org.springframework.stereotype.Component;

@Component("providerAClient")
public class SimulatedProviderAClient extends BaseSimulatedProviderClient {
    @Override
    public String getProviderCode() {
        return "PROVIDER_A";
    }
}
