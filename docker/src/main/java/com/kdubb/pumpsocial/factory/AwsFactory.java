package com.kdubb.pumpsocial.factory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.kdubb.pumpsocial.ConfigConstants;

@Service
public class AwsFactory {

    @Value(ConfigConstants.SES_ACCESS_KEY)
    private String sesAccessKey;

    @Value(ConfigConstants.SES_SECRET_KEY)
    private String sesSecretKey;
    
    public AmazonSimpleEmailServiceClient getAmazonSimpleEmailServiceClient() {
    	BasicAWSCredentials credentials = new BasicAWSCredentials(sesAccessKey, sesSecretKey);
        return new AmazonSimpleEmailServiceClient(credentials);
    }
}