package com.ayd.pubsub;

import com.ayd.dto.DocumentStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
@Slf4j
public class NotificationHub {

    private final Sinks.Many<DocumentStatusEvent> localSink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publish(DocumentStatusEvent event) {
        log.info("Publishing to sink {}", event);
        localSink.tryEmitNext(event);
    }
    public Flux<DocumentStatusEvent> stream(Long documentId) {
        return localSink.asFlux()
                .filter(e -> e.getDocumentId().equals(documentId));
    }
}
