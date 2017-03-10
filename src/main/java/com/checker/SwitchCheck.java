package com.checker;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

import java.io.*;
import java.net.URL;

public class SwitchCheck implements RequestStreamHandler {

    public void handleRequest(InputStream in, OutputStream output, Context context) throws IOException {
        try {
            URL url = new URL(System.getenv("URL"));
            String[] indexChecks = System.getenv("CHECKS").split(",");
            String snsArn = System.getenv("SNS_ARN");
            StringBuilder out = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            for (String line; (line = reader.readLine()) != null; ) {
                out.append(line);
            }

            for(String check : indexChecks) {
                if(check.charAt(0) == '!') {
                     if(out.indexOf(check.substring(1)) == 0) {
                        alert(snsArn, "Missing key" + check + ".  URL: " + System.getenv("URL"));
                        output.write("Alerting...".getBytes());
                        return;
                    }
                } else {
                    if (out.indexOf(check) > -1) {
                        alert(snsArn, "Found key " + check + ".  URL: " + System.getenv("URL"));
                        output.write("Alerting...".getBytes());
                        return;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(e);
            output.write(e.getMessage().getBytes());
            System.exit(2);
        }
        output.write("Not alerting".getBytes());
    }

    private void alert(String arn, String message) {
        AmazonSNSClient snsClient = new AmazonSNSClient();
        PublishResult result = snsClient.publish(new PublishRequest(arn,
                "Switch Appears to be available." + message).withSubject("Switch Alert!!!!"));
    }
}
