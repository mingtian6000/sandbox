package com.fraud.detection;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.Contact;

@OpenAPIDefinition(
    info = @Info(
        title = "Fraud Detection API",
        version = "1.0.0",
        description = "Real-time fraud scoring pipeline for bank transactions",
        contact = @Contact(name = "Fraud Detection Team")
    )
)
public class FraudDetectionApplication extends Application {
}
