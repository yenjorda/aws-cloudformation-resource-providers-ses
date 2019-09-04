package com.aws.ses.configurationset;

import com.amazonaws.cloudformation.exceptions.ResourceAlreadyExistsException;
import com.amazonaws.cloudformation.exceptions.ResourceNotFoundException;
import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.cloudformation.resource.IdentifierUtils;
import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.ConfigurationSet;
import software.amazon.awssdk.services.ses.model.ConfigurationSetAlreadyExistsException;
import software.amazon.awssdk.services.ses.model.ConfigurationSetDoesNotExistException;
import software.amazon.awssdk.services.ses.model.CreateConfigurationSetRequest;

import static com.aws.ses.configurationset.ResourceModelExtensions.getPrimaryIdentifier;

public class CreateHandler extends BaseHandler<CallbackContext> {

    public static final int MAX_LENGTH_CONFIGURATION_SET_NAME = 64;

    private AmazonWebServicesClientProxy proxy;
    private SesClient client;
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        this.proxy = proxy;
        this.client = ClientBuilder.getClient();
        this.logger = logger;

        if (callbackContext != null && callbackContext.getIsStabilization()) {
            return stabilizeConfigurationSet(proxy, callbackContext, request);
        } else {
            return createConfigurationSet(proxy, request);
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> createConfigurationSet(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request) {

        ResourceModel model = request.getDesiredResourceState();

        // resource can auto-generate a name if not supplied by caller
        // this logic should move up into the CloudFormation engine, but
        // currently exists here for backwards-compatibility with existing models
        if (StringUtils.isNullOrEmpty(model.getName())) {
            model.setName(
                IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(),
                    MAX_LENGTH_CONFIGURATION_SET_NAME
                )
            );
        }

        // pre-creation read to ensure no existing resource exists
        try {
            new ReadHandler().handleRequest(proxy, request, null, this.logger);
            throw new ResourceAlreadyExistsException(ResourceModel.TYPE_NAME, model.getName());
        } catch (final ResourceNotFoundException e) {
            // no existing resource, creation can proceed
        }

        try {
            final CreateConfigurationSetRequest createConfigurationSetRequest =
                CreateConfigurationSetRequest.builder()
                    .configurationSet(ConfigurationSet.builder()
                        .name(model.getName())
                        .build())
                    .build();
            proxy.injectCredentialsAndInvokeV2(createConfigurationSetRequest, this.client::createConfigurationSet);
            logger.log(String.format("%s [%s] created successfully",
                ResourceModel.TYPE_NAME, getPrimaryIdentifier(model).toString()));
        } catch (final ConfigurationSetAlreadyExistsException e) {
            // failing here would suggest a conflicting operation was performed out of band
            throw new ResourceAlreadyExistsException(ResourceModel.TYPE_NAME, model.getName());
        }

        CallbackContext stabilizationContext = CallbackContext.builder()
            .isStabilization(true)
            .build();
        return ProgressEvent.defaultInProgressHandler(
            stabilizationContext,
            5,
            model);
    }

    private ProgressEvent<ResourceModel, CallbackContext> stabilizeConfigurationSet(
        final AmazonWebServicesClientProxy proxy,
        final CallbackContext callbackContext,
        final ResourceHandlerRequest<ResourceModel> request) {
        ResourceModel model = request.getDesiredResourceState();

        // read to ensure resource exists
        try {
            final ProgressEvent<ResourceModel, CallbackContext> readResult =
                new ReadHandler().handleRequest(proxy, request, null, this.logger);
            return ProgressEvent.defaultSuccessHandler(readResult.getResourceModel());
        } catch (final ConfigurationSetDoesNotExistException e) {
            // resource not yet found, re-invoke
        }

        return ProgressEvent.defaultInProgressHandler(
            callbackContext,
            5,
            model);
    }
}