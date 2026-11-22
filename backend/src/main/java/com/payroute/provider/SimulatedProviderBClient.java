package com.payroute.provider;

import org.springframework.stereotype.Component;

@Component("providerBClient")
public class SimulatedProviderBClient extends BaseSimulatedProviderClient {
    @Override
    public String getProviderCode() {
        return "PROVIDER_B";
    }
}
