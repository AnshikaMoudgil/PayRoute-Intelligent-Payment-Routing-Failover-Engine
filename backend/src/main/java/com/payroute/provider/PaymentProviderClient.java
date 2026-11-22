package com.payroute.provider;

import com.payroute.entity.PaymentProvider;
import com.payroute.entity.PaymentTransaction;
import com.payroute.entity.Refund;

public interface PaymentProviderClient {
    String getProviderCode();
    ProviderTransactionResult processPayment(PaymentTransaction transaction, PaymentProvider providerConfig);
    ProviderTransactionResult processRefund(Refund refund, PaymentProvider providerConfig);
}
