package software.amazon.dynamodb.table;

import software.amazon.awssdk.core.SdkClient;
// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;
// import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
  /*
  TODO: uncomment the following, replacing YourServiceAsyncClient with your service client name
  It is recommended to use static HTTP client so less memory is consumed
  e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ClientBuilder.java#L9

  public static YourServiceAsyncClient getClient() {
    return YourServiceAsyncClient SERVICE_CLIENT = YourServiceAsyncClient.builder()
              .httpClient(LambdaWrapper.HTTP_CLIENT)
              .build();
  }
  */

  // TODO: remove this implementation once you have uncommented the above
  public static SdkClient getClient() {
    return null;
  }
}
