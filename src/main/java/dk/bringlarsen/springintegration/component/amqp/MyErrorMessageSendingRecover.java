package dk.bringlarsen.springintegration.component.amqp;

import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

public class MyErrorMessageSendingRecover implements RecoveryCallback<Object> {

    private final MessagingTemplate messagingTemplate = new MessagingTemplate();

    public MyErrorMessageSendingRecover(MessageChannel channel) {
        messagingTemplate.setDefaultChannel(channel);
    }

    @Override
    public Object recover(RetryContext context) throws Exception {
        Throwable lastThrowable = context.getLastThrowable();
        Throwable cause = lastThrowable.getCause();

        // TODO: Sanity checks..

        if(cause instanceof HttpClientErrorException) {
            throw (HttpClientErrorException) cause;

        } else if(cause instanceof HttpServerErrorException){
            Message<String> failedMessage = (Message<String>) context.getAttribute("message");
            messagingTemplate.send(MessageBuilder.fromMessage(failedMessage)
                    .setHeader("cause", lastThrowable.getMessage())
                    .build());
        }
        // TODO: handle other cases..
        return null;
    }
}
